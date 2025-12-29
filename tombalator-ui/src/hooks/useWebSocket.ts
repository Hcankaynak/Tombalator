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
  const prevDepsRef = useRef({ enabled, gameId, userId, username })
  
  // Use refs to store callbacks so they don't cause reconnections
  const onMessageRef = useRef(onMessage)
  const onErrorRef = useRef(onError)
  const onOpenRef = useRef(onOpen)
  const onCloseRef = useRef(onClose)
  
  // Update refs when callbacks change (without causing reconnection)
  useEffect(() => {
    onMessageRef.current = onMessage
    onErrorRef.current = onError
    onOpenRef.current = onOpen
    onCloseRef.current = onClose
  }, [onMessage, onError, onOpen, onClose])

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
        onOpenRef.current?.()
      }

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data) as WebSocketMessage
          onMessageRef.current?.(message)
        } catch (error) {
          console.error('Error parsing WebSocket message:', error)
        }
      }

      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        onErrorRef.current?.(error)
      }

      ws.onclose = () => {
        setIsConnected(false)
        onCloseRef.current?.()
        
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
  }, [gameId, userId, username, enabled])

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
      // Only close if not already closed
      if (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING) {
        wsRef.current.close()
      }
      wsRef.current = null
    }
    setIsConnected(false)
    wasConnectedRef.current = false
  }, [])

  useEffect(() => {
    const prevDeps = prevDepsRef.current
    const depsChanged = 
      prevDeps.enabled !== enabled ||
      prevDeps.gameId !== gameId ||
      prevDeps.userId !== userId ||
      prevDeps.username !== username
    
    // Update previous deps
    prevDepsRef.current = { enabled, gameId, userId, username }
    
    if (enabled && gameId && userId && username) {
      // Only connect if not already connected or if dependencies changed
      if (wsRef.current?.readyState !== WebSocket.OPEN) {
        // If we were connected with different deps, disconnect first
        if (depsChanged && wsRef.current) {
          disconnect()
        }
        connect()
      }
    } else {
      // Only disconnect if actually connected
      if (wsRef.current?.readyState === WebSocket.OPEN || wsRef.current?.readyState === WebSocket.CONNECTING) {
        disconnect()
      }
    }

    return () => {
      // Cleanup: only disconnect if enabled became false or deps changed to invalid values
      const shouldDisconnect = !enabled || !gameId || !userId || !username
      if (shouldDisconnect && (wsRef.current?.readyState === WebSocket.OPEN || wsRef.current?.readyState === WebSocket.CONNECTING)) {
        disconnect()
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, gameId, userId, username])

  return {
    isConnected,
    sendMessage,
    disconnect,
  }
}

