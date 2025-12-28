import { useEffect, useRef, useState, useCallback } from 'react'

// Use environment variable or fallback to localhost for development
// In production (Docker), auto-detect from window.location for nginx proxy
const WS_BASE_URL = import.meta.env.VITE_WS_URL || 
  (import.meta.env.DEV 
    ? 'ws://localhost:3000' 
    : (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host)

interface WebSocketMessage {
  type: string
  [key: string]: any
}

interface UseWebSocketOptions {
  lobbyId: string
  userId: string
  username: string
  onMessage?: (message: WebSocketMessage) => void
  onError?: (error: Event) => void
  onOpen?: () => void
  onClose?: () => void
}

export function useWebSocket({
  lobbyId,
  userId,
  username,
  onMessage,
  onError,
  onOpen,
  onClose,
}: UseWebSocketOptions) {
  const [isConnected, setIsConnected] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return
    }

    try {
      const ws = new WebSocket(`${WS_BASE_URL}/ws/lobby/${lobbyId}`)
      wsRef.current = ws

      ws.onopen = () => {
        setIsConnected(true)
        // Send join message
        ws.send(
          JSON.stringify({
            type: 'join_lobby',
            lobbyId,
            userId,
            username,
          })
        )
        onOpen?.()
      }

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data) as WebSocketMessage
          onMessage?.(message)
        } catch (error) {
          console.error('Error parsing WebSocket message:', error)
        }
      }

      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        onError?.(error)
      }

      ws.onclose = () => {
        setIsConnected(false)
        onClose?.()
        
        // Attempt to reconnect after 3 seconds
        reconnectTimeoutRef.current = setTimeout(() => {
          if (lobbyId && userId && username) {
            connect()
          }
        }, 3000)
      }
    } catch (error) {
      console.error('Error creating WebSocket:', error)
    }
  }, [lobbyId, userId, username, onMessage, onError, onOpen, onClose])

  const sendMessage = useCallback((message: WebSocketMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket is not connected')
    }
  }, [])

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
    }
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
    setIsConnected(false)
  }, [])

  useEffect(() => {
    if (lobbyId && userId && username) {
      connect()
    }

    return () => {
      disconnect()
    }
  }, [lobbyId, userId, username])

  return {
    isConnected,
    sendMessage,
    disconnect,
  }
}

