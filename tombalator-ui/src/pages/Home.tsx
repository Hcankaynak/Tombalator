import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './Home.css'

// Use environment variable or fallback to localhost for development
// In production (Docker), empty string uses nginx proxy
const API_BASE_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:3000' : '')

interface GameExistsResponse {
  exists: boolean
  gameId: string
}

function Home() {
  const [gameId, setGameId] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isChecking, setIsChecking] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!gameId.trim()) {
      return
    }

    const trimmedGameId = gameId.trim()
    setErrorMessage(null)
    setIsChecking(true)

    try {
      // Check if game exists
      const response = await fetch(`${API_BASE_URL}/api/game/${trimmedGameId}`)
      
      if (!response.ok) {
        throw new Error('Failed to check game existence')
      }

      const data: GameExistsResponse = await response.json()

      if (data.exists) {
        // Game exists, redirect to game page
        navigate(`/game/${trimmedGameId}`)
      } else {
        // Game doesn't exist
        setErrorMessage(`Game with ID "${trimmedGameId}" does not exist. Please check the game ID and try again.`)
      }
    } catch (error) {
      console.error('Error checking game existence:', error)
      setErrorMessage('Failed to check game. Please try again.')
    } finally {
      setIsChecking(false)
    }
  }

  return (
    <div className="home">
      <div className="home-container">
        <h2 className="home-title">Join a Game</h2>
        <form onSubmit={handleSubmit} className="game-form">
          <input
            type="text"
            value={gameId}
            onChange={(e) => {
              setGameId(e.target.value)
              setErrorMessage(null) // Clear error when user types
            }}
            placeholder="Enter Game ID"
            className="game-input"
            required
            disabled={isChecking}
          />
          {errorMessage && (
            <div className="error-message">
              {errorMessage}
            </div>
          )}
          <button 
            type="submit" 
            className="game-button"
            disabled={isChecking}
          >
            {isChecking ? 'Checking...' : 'Join'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default Home

