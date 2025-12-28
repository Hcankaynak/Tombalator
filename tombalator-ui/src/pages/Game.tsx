import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Chat from '../components/Chat'
import UsersList from '../components/UsersList'
import CardSelection from '../components/CardSelection'
import type { TombalaCard } from '../components/CardSelection'
import PlayerCard from '../components/PlayerCard'
import { generateCards } from '../utils/cardGenerator'
import { useWebSocket } from '../hooks/useWebSocket'
import './Game.css'

const NICKNAME_STORAGE_KEY = 'tombalator_nickname'

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
  const [gameStarted, setGameStarted] = useState(false)
  const [currentUserId, setCurrentUserId] = useState('')
  const [users, setUsers] = useState<User[]>([])
  const [messages, setMessages] = useState<Message[]>([])

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
      // Save nickname to localStorage
      localStorage.setItem(NICKNAME_STORAGE_KEY, nickname.trim())
      
      // Generate user ID
      const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      setCurrentUserId(userId)
      
      // Add current user to users list
      const newUser: User = {
        id: userId,
        username: nickname.trim(),
      }
      setUsers([newUser])
      setIsJoined(true)
      
      // Generate 3 cards for selection
      const cards = generateCards(3)
      setAvailableCards(cards)
      setShowCardSelection(true)
    }
  }

  // WebSocket connection - only connect after joining
  const { isConnected, sendMessage } = useWebSocket({
    gameId: gameId || '',
    userId: currentUserId,
    username: nickname,
    onMessage: (message) => {
      switch (message.type) {
        case 'players_update':
          // Update entire players list
          if (message.players && Array.isArray(message.players)) {
            setUsers(
              message.players.map((p: any) => ({
                id: p.userId,
                username: p.username,
              }))
            )
          }
          break
        case 'chat':
          // Add chat message (received from server, broadcasted to all)
          if (message.username && message.message) {
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
          // Handle number drawn (for future game logic)
          console.log('Number drawn:', message.number)
          break
        case 'error':
          console.error('WebSocket error:', message.message)
          break
      }
    },
    onOpen: () => {
      console.log('WebSocket connected to game:', gameId)
    },
    onClose: () => {
      console.log('WebSocket disconnected')
    },
    onError: (error) => {
      console.error('WebSocket error:', error)
    },
  })

  const handleCardSelect = (card: TombalaCard) => {
    setSelectedCard(card)
    setShowCardSelection(false)
  }

  const handleChangeCard = () => {
    // Generate new cards for selection
    const cards = generateCards(3)
    setAvailableCards(cards)
    setShowCardSelection(true)
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
              onChange={(e) => setNickname(e.target.value)}
              placeholder="Your nickname"
              className="nickname-input"
              required
              autoFocus
            />
            <button type="submit" className="nickname-button">
              Join Game
            </button>
          </form>
        </div>
      </div>
    )
  }

  return (
    <div className="game">
      {showCardSelection && (
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
                  {!gameStarted && (
                    <button
                      onClick={handleChangeCard}
                      className="change-card-button"
                    >
                      Change Card
                    </button>
                  )}
                </div>
                <PlayerCard card={selectedCard} />
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

