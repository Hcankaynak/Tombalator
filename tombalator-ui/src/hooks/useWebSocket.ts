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
  gameId: string
  userId: string
  username: string
  enabled?: boolean
  onMessage?: (message: WebSocketMessage) => void
  onError?: (error: Event) => void
  onOpen?: () => void
  onClose?: () => void
}

export function useWebSocket({
  gameId,
  userId,
  username,
  enabled = true,
  onMessage,
  onError,
  onOpen,
  onClose,
}: UseWebSocketOptions) {
  const [isConnected, setIsConnected] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const wasConnectedRef = useRef(false)

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return
    }

    try {
      const ws = new WebSocket(`${WS_BASE_URL}/ws/game/${gameId}`)
      wsRef.current = ws

      ws.onopen = () => {
        setIsConnected(true)
        wasConnectedRef.current = true
        // Send join message
        ws.send(
          JSON.stringify({
            type: 'join_game',
            gameId,
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
        
        // Only attempt to reconnect if we were previously successfully connected
        // This prevents reconnection when join is rejected (e.g., duplicate nickname)
        if (wasConnectedRef.current && enabled) {
          reconnectTimeoutRef.current = setTimeout(() => {
            if (gameId && userId && username && enabled) {
              connect()
            }
          }, 3000)
        } else {
          wasConnectedRef.current = false
        }
      }
    } catch (error) {
      console.error('Error creating WebSocket:', error)
    }
  }, [gameId, userId, username, enabled, onMessage, onError, onOpen, onClose])

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
      reconnectTimeoutRef.current = null
    }
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
    setIsConnected(false)
    wasConnectedRef.current = false
  }, [])

  useEffect(() => {
    if (enabled && gameId && userId && username) {
      connect()
    } else {
      disconnect()
    }

    return () => {
      disconnect()
    }
  }, [enabled, gameId, userId, username])

  return {
    isConnected,
    sendMessage,
    disconnect,
  }
}

