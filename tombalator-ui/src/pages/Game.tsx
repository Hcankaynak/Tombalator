import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Chat from '../components/Chat'
import UsersList from '../components/UsersList'
import CardSelection from '../components/CardSelection'
import type { TombalaCard } from '../components/CardSelection'
import PlayerCard from '../components/PlayerCard'
import WinnerCelebration from '../components/WinnerCelebration'
import { useWebSocket } from '../hooks/useWebSocket'
import './Game.css'

const NICKNAME_STORAGE_KEY = 'tombalator_nickname'

// Use environment variable or fallback to localhost for development
// In production (Docker), empty string uses nginx proxy
const API_BASE_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:3000' : '')

interface GameExistsResponse {
  exists: boolean
  gameId: string
}

interface CardOptionsResponse {
  success: boolean
  cards: TombalaCard[]
  message: string
}

interface Message {
  id: string
  username: string
  text: string
  timestamp: Date
}

interface User {
  id: string
  username: string
}

function Game() {
  const { gameId } = useParams<{ gameId: string }>()
  const navigate = useNavigate()
  const [nickname, setNickname] = useState('')
  const [isJoined, setIsJoined] = useState(false)
  const [showCardSelection, setShowCardSelection] = useState(false)
  const [availableCards, setAvailableCards] = useState<TombalaCard[]>([])
  const [selectedCard, setSelectedCard] = useState<TombalaCard | null>(null)
  const [currentUserId, setCurrentUserId] = useState('')
  const [users, setUsers] = useState<User[]>([])
  const [messages, setMessages] = useState<Message[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isConnecting, setIsConnecting] = useState(false)
  const [isCheckingGame, setIsCheckingGame] = useState(true)
  const [gameExists, setGameExists] = useState(false)
  const [drawnNumbers, setDrawnNumbers] = useState<number[]>([])
  const [closedNumbers, setClosedNumbers] = useState<Set<number>>(new Set())
  const [isLoadingCards, setIsLoadingCards] = useState(false)
  const [winnerName, setWinnerName] = useState<string | null>(null)

  // Check if game exists when component mounts or gameId changes
  useEffect(() => {
    if (!gameId) {
      setIsCheckingGame(false)
      setGameExists(false)
      setErrorMessage('Invalid game ID')
      return
    }

    const checkGameExists = async () => {
      setIsCheckingGame(true)
      setErrorMessage(null)

      try {
        const response = await fetch(`${API_BASE_URL}/api/game/${gameId}`)
        
        if (!response.ok) {
          throw new Error('Failed to check game existence')
        }

        const data: GameExistsResponse = await response.json()

        if (data.exists) {
          setGameExists(true)
        } else {
          setGameExists(false)
          setErrorMessage(`Game with ID "${gameId}" does not exist. Please check the game ID and try again.`)
        }
      } catch (error) {
        console.error('Error checking game existence:', error)
        setGameExists(false)
        setErrorMessage('Failed to check game. Please try again.')
      } finally {
        setIsCheckingGame(false)
      }
    }

    checkGameExists()
  }, [gameId])

  useEffect(() => {
    // Load nickname from localStorage if it exists
    const savedNickname = localStorage.getItem(NICKNAME_STORAGE_KEY)
    if (savedNickname) {
      setNickname(savedNickname)
    }
  }, [])

  const handleJoin = (e: React.FormEvent) => {
    e.preventDefault()
    if (nickname.trim()) {
      setErrorMessage(null)
      setIsConnecting(true)
      
      // Save nickname to localStorage
      localStorage.setItem(NICKNAME_STORAGE_KEY, nickname.trim())
      
      // Generate user ID
      const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      setCurrentUserId(userId)
      
      // Note: isJoined will be set to true only after successful WebSocket join
      // (when we receive players_update message)
    }
  }

  // WebSocket connection - only connect after user submits nickname
  // Keep enabled true after joining to maintain connection
  const { isConnected, sendMessage } = useWebSocket({
    gameId: gameId || '',
    userId: currentUserId,
    username: nickname,
    enabled: (isConnecting || isJoined) && !!currentUserId && !!nickname.trim(),
    onMessage: (message) => {
      switch (message.type) {
        case 'players_update':
          // Update entire players list - this confirms successful join
          if (message.players && Array.isArray(message.players)) {
            setUsers(
              message.players.map((p: any) => ({
                id: p.userId,
                username: p.username,
              }))
            )
            // Only set isJoined after receiving players list (successful join)
            if (!isJoined) {
              setIsJoined(true)
              setIsConnecting(false)
              // Fetch card options from backend
              fetchCardOptions()
            }
          }
          break
        case 'chat':
          // Add chat message (received from server, broadcasted to all)
          if (message.username && message.message) {
            // Check if this is a win message
            if (message.username === 'SYSTEM' && message.message.includes('wins the game')) {
              // Extract winner name from message (format: "username wins the game")
              const winnerMatch = message.message.match(/^(.+?)\s+wins the game$/)
              if (winnerMatch && winnerMatch[1]) {
                setWinnerName(winnerMatch[1])
              }
            }
            
            // Check if message already exists to prevent duplicates
            setMessages((prev) => {
              const messageId = `msg_${message.userId}_${message.timestamp}`
              // Check if message already exists
              if (prev.find((m) => m.id === messageId)) {
                return prev
              }
              return [
                ...prev,
                {
                  id: messageId,
                  username: message.username,
                  text: message.message,
                  timestamp: new Date(message.timestamp || Date.now()),
                },
              ]
            })
          }
          break
        case 'number_drawn':
          // Handle number drawn - update drawn numbers list
          if (message.number && message.drawnNumbers) {
            console.log('Number drawn:', message.number)
            setDrawnNumbers(message.drawnNumbers || [])
          }
          break
        case 'error':
          // Handle error messages (e.g., duplicate nickname)
          console.error('WebSocket error:', message.message)
          setErrorMessage(message.message || 'An error occurred')
          setIsConnecting(false)
          // Disconnect WebSocket and reset state to allow retry
          setCurrentUserId('')
          break
      }
    },
    onOpen: () => {
      console.log('WebSocket connected to game:', gameId)
    },
    onClose: () => {
      console.log('WebSocket disconnected')
      if (isConnecting && !isJoined) {
        // If we were connecting but got disconnected before joining, reset
        setIsConnecting(false)
        setCurrentUserId('')
      }
    },
    onError: (error) => {
      console.error('WebSocket error:', error)
      setErrorMessage('Connection error. Please try again.')
      setIsConnecting(false)
      setCurrentUserId('')
    },
  })

  const handleCardSelect = (card: TombalaCard) => {
    setSelectedCard(card)
    setShowCardSelection(false)
    // Reset closed numbers when selecting a new card
    setClosedNumbers(new Set())
    
    // Send card selection to backend via WebSocket
    if (currentUserId && isConnected && sendMessage) {
      sendMessage({
        type: 'select_card',
        userId: currentUserId,
        card: {
          id: card.id,
          rows: card.rows,
          theme: card.theme || null,
        },
      })
    }
  }

  const fetchCardOptions = async () => {
    if (!gameId) {
      return
    }

    setIsLoadingCards(true)
    setErrorMessage(null)

    try {
      const response = await fetch(`${API_BASE_URL}/api/game/${gameId}/card-options`)
      
      if (!response.ok) {
        throw new Error('Failed to fetch card options')
      }

      const data: CardOptionsResponse = await response.json()

      if (data.success && data.cards) {
        setAvailableCards(data.cards)
        setShowCardSelection(true)
      } else {
        setErrorMessage(data.message || 'Failed to load card options')
      }
    } catch (error) {
      console.error('Error fetching card options:', error)
      setErrorMessage('Failed to load card options. Please try again.')
    } finally {
      setIsLoadingCards(false)
    }
  }

  const handleChangeCard = () => {
    // Fetch new cards from backend
    fetchCardOptions()
    // Reset closed numbers when changing card
    setClosedNumbers(new Set())
    // Note: When user selects a new card, handleCardSelect will send it to backend
  }

  const handleNumberClick = async (number: number) => {
    // If number is already closed, don't allow unmarking (one-way action)
    if (closedNumbers.has(number)) {
      return
    }

    if (!gameId || !currentUserId) {
      return
    }

    try {
      // Check with backend if number can be closed
      const response = await fetch(`${API_BASE_URL}/api/game/${gameId}/close-number`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ number, userId: currentUserId }),
      })

      if (!response.ok) {
        throw new Error('Failed to check if number can be closed')
      }

      const data = await response.json()

      if (data.success && data.canClose) {
        // Number can be closed - update local state
        setClosedNumbers((prev) => {
          const newSet = new Set(prev)
          newSet.add(number)
          return newSet
        })
        console.log(`Number ${number} closed successfully`)
      } else {
        // Number cannot be closed (not drawn yet)
        console.log(`Number ${number} cannot be closed: ${data.message || 'Not drawn yet'}`)
        // Optionally show a message to the user
        // setErrorMessage(data.message || 'Number cannot be closed yet')
      }
    } catch (error) {
      console.error('Error closing number:', error)
      // Optionally show error message to user
      // setErrorMessage('Failed to close number. Please try again.')
    }
  }

  const handleSendMessage = (text: string) => {
    if (!currentUserId || !nickname || !isConnected) return

    // Send message via WebSocket
    sendMessage({
      type: 'chat',
      userId: currentUserId,
      username: nickname,
      message: text,
      timestamp: Date.now(),
    })
  }

  // Show loading state while checking if game exists
  if (isCheckingGame) {
    return (
      <div className="game">
        <div className="game-container">
          <h2 className="game-title">Game: {gameId}</h2>
          <div className="game-checking">
            <p>Checking if game exists...</p>
          </div>
        </div>
      </div>
    )
  }

  // Show error if game doesn't exist
  if (!gameExists) {
    return (
      <div className="game">
        <div className="game-container">
          <h2 className="game-title">Game: {gameId}</h2>
          {errorMessage && (
            <div className="error-message">
              {errorMessage}
            </div>
          )}
          <button 
            onClick={() => navigate('/')}
            className="game-button"
            style={{ marginTop: '1rem', width: '100%' }}
          >
            Go Back to Home
          </button>
        </div>
      </div>
    )
  }

  if (!isJoined) {
    return (
      <div className="game">
        <div className="game-container">
          <h2 className="game-title">Game: {gameId}</h2>
          <form onSubmit={handleJoin} className="nickname-form">
            <label htmlFor="nickname" className="nickname-label">
              Enter your nickname
            </label>
            <input
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => {
                setNickname(e.target.value)
                setErrorMessage(null) // Clear error when user types
              }}
              placeholder="Your nickname"
              className="nickname-input"
              required
              autoFocus
              disabled={isConnecting}
            />
            {errorMessage && (
              <div className="error-message">
                {errorMessage}
              </div>
            )}
            <button 
              type="submit" 
              className="nickname-button"
              disabled={isConnecting}
            >
              {isConnecting ? 'Connecting...' : 'Join Game'}
            </button>
          </form>
        </div>
      </div>
    )
  }

  return (
    <div className="game">
      {winnerName && (
        <WinnerCelebration
          winnerName={winnerName}
          onClose={() => setWinnerName(null)}
        />
      )}
      {isLoadingCards && (
        <div className="game-loading-overlay">
          <div className="game-loading-message">
            <p>Loading card options...</p>
          </div>
        </div>
      )}
      {showCardSelection && !isLoadingCards && (
        <CardSelection
          cards={availableCards}
          onSelectCard={handleCardSelect}
        />
      )}
      <div className="game-container-joined">
        <div className="game-header">
          <h2 className="game-title-joined">Game: {gameId}</h2>
          <button
            onClick={() => navigate('/')}
            className="leave-button"
          >
            Leave Game
          </button>
        </div>
        <div className="game-content">
          <div className="game-sidebar-left">
            <UsersList users={users} currentUsername={nickname} />
          </div>
          <div className="game-center">
            {selectedCard ? (
              <div className="game-card-container">
                <div className="game-card-header-section">
                  <h3 className="game-card-title">Your Card</h3>
                  <button
                    onClick={handleChangeCard}
                    className="change-card-button"
                  >
                    Change Card
                  </button>
                </div>
                <PlayerCard 
                  card={selectedCard} 
                  markedNumbers={closedNumbers}
                  drawnNumbers={drawnNumbers}
                  onNumberClick={handleNumberClick}
                />
              </div>
            ) : (
              <div className="game-card-placeholder">
                <p>Select a card to start</p>
              </div>
            )}
          </div>
          <div className="game-sidebar-right">
            <Chat
              currentUsername={nickname}
              messages={messages}
              onSendMessage={handleSendMessage}
            />
          </div>
        </div>
      </div>
    </div>
  )
}

export default Game

