import { useState, useEffect } from 'react'
import './Presenter.css'

// Use environment variable or fallback to localhost for development
// In production (Docker), empty string uses nginx proxy
const API_BASE_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:3000' : '')

function Presenter() {
  const [gameId, setGameId] = useState('')
  const [hasGame, setHasGame] = useState(false)
  const [drawnNumbers, setDrawnNumbers] = useState<number[]>([])
  const [currentNumber, setCurrentNumber] = useState<number | null>(null)
  const [isSelecting, setIsSelecting] = useState(false)
  const [availableNumbers, setAvailableNumbers] = useState<number[]>(
    Array.from({ length: 90 }, (_, i) => i + 1)
  )

  useEffect(() => {
    // Check if game ID is stored
    const savedGameId = localStorage.getItem('presenter_game_id')
    if (savedGameId) {
      setGameId(savedGameId)
      setHasGame(true)
    }
  }, [])

  const selectRandomNumber = async () => {
    if (availableNumbers.length === 0 || isSelecting || !gameId) return

    setIsSelecting(true)
    
    // Mystery animation - shuffle through numbers
    const animationDuration = 2000 // 2 seconds
    const shuffleInterval = 50 // Change number every 50ms

    const shuffleIntervalId = setInterval(() => {
      const randomIndex = Math.floor(Math.random() * availableNumbers.length)
      setCurrentNumber(availableNumbers[randomIndex])
    }, shuffleInterval)

    try {
      const savedApiKey = localStorage.getItem('admin_api_key')
      if (!savedApiKey) {
        alert('Admin API key not found. Please authenticate first.')
        clearInterval(shuffleIntervalId)
        setIsSelecting(false)
        setCurrentNumber(null)
        return
      }

      // Call backend to draw number
      const response = await fetch(`${API_BASE_URL}/api/game/${gameId}/draw-number`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': savedApiKey,
        },
      })

      const data = await response.json()

      // Wait for animation to complete
      setTimeout(() => {
        clearInterval(shuffleIntervalId)

        if (data.success && data.number !== null) {
          // Update state with backend response
          setCurrentNumber(data.number)
          setDrawnNumbers(data.drawnNumbers || [])
          setAvailableNumbers(Array.from({ length: 90 }, (_, i) => i + 1).filter(n => !data.drawnNumbers.includes(n)))
          console.log('Number drawn:', data.number)
        } else {
          // Error or all numbers drawn
          alert(data.message || 'Failed to draw number')
          setCurrentNumber(null)
        }
        setIsSelecting(false)
      }, animationDuration)
    } catch (error) {
      clearInterval(shuffleIntervalId)
      console.error('Error drawing number:', error)
      alert('Failed to draw number. Please try again.')
      setCurrentNumber(null)
      setIsSelecting(false)
    }
  }

  const handleGameIdSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (gameId.trim()) {
      setHasGame(true)
      localStorage.setItem('presenter_game_id', gameId.trim())
      // TODO: Connect to game via WebSocket/API
      console.log('Joined game:', gameId.trim())
    }
  }

  const handleCreateGame = async () => {
    try {
      const savedApiKey = localStorage.getItem('admin_api_key')
      if (!savedApiKey) {
        alert('Admin API key not found. Please authenticate first.')
        return
      }

      const response = await fetch(`${API_BASE_URL}/api/game/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': savedApiKey,
        },
      })

      const data = await response.json()

      if (data.success && data.gameId) {
        setGameId(data.gameId)
        setHasGame(true)
        localStorage.setItem('presenter_game_id', data.gameId)
        console.log('Created game:', data.gameId)
      } else {
        alert(`Failed to create game: ${data.message || 'Unknown error'}`)
        console.error('Error creating game:', data)
      }
    } catch (error) {
      console.error('Error creating game:', error)
      alert('Failed to create game. Please try again.')
    }
  }

  const resetGame = async () => {
    if (!window.confirm('Are you sure you want to reset the game? This will clear all drawn numbers.')) {
      return
    }

    try {
      const savedApiKey = localStorage.getItem('admin_api_key')
      if (!savedApiKey) {
        alert('Admin API key not found. Please authenticate first.')
        return
      }

      // TODO: Add reset endpoint to backend
      // For now, just reset local state
      // When backend endpoint is added, call it here:
      // const response = await fetch(`${API_BASE_URL}/api/game/${gameId}/reset`, {
      //   method: 'POST',
      //   headers: {
      //     'Content-Type': 'application/json',
      //     'X-API-Key': savedApiKey,
      //   },
      // })

      setDrawnNumbers([])
      setCurrentNumber(null)
      setAvailableNumbers(Array.from({ length: 90 }, (_, i) => i + 1))
    } catch (error) {
      console.error('Error resetting game:', error)
      alert('Failed to reset game. Please try again.')
    }
  }

  if (!hasGame) {
    return (
      <div className="presenter">
        <div className="presenter-game-setup">
          <h2>Select or Create Game</h2>
          <div className="game-setup-content">
            <div className="join-game-section">
              <h3>Join Existing Game</h3>
              <p>Enter a game ID to join an existing game</p>
              <form onSubmit={handleGameIdSubmit} className="game-id-form">
                <input
                  type="text"
                  value={gameId}
                  onChange={(e) => setGameId(e.target.value)}
                  placeholder="Enter Game ID"
                  className="game-id-input"
                  required
                />
                <button type="submit" className="game-id-button">
                  Join Game
                </button>
              </form>
            </div>
            <div className="divider">
              <span>OR</span>
            </div>
            <div className="create-game-section">
              <h3>Create New Game</h3>
              <p>Create a new game to start</p>
              <button onClick={handleCreateGame} className="create-game-button">
                Create Game
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="presenter">
      <div className="presenter-container">
        <div className="presenter-header">
          <div>
            <h2>Presenter Control Panel</h2>
            <p className="game-info">Game: {gameId}</p>
          </div>
          <div className="presenter-header-actions">
            <button onClick={() => { setHasGame(false); localStorage.removeItem('presenter_game_id'); }} className="change-game-button">
              Change Game
            </button>
            <button onClick={resetGame} className="reset-button">
              Reset Game
            </button>
          </div>
        </div>

        <div className="presenter-main">
          <div className="number-display-section">
            <div className={`current-number-display ${isSelecting ? 'selecting' : ''}`}>
              {currentNumber ? (
                <div className="number-content">
                  <span className="number-value">{currentNumber}</span>
                </div>
              ) : (
                <div className="number-placeholder">
                  <span>Ready to draw</span>
                </div>
              )}
            </div>
            <button
              onClick={selectRandomNumber}
              disabled={isSelecting || availableNumbers.length === 0}
              className="draw-button"
            >
              {isSelecting ? 'Selecting...' : 'Draw Next Number'}
            </button>
          </div>

          <div className="drawn-numbers-section">
            <h3>Drawn Numbers ({drawnNumbers.length})</h3>
            <div className="drawn-numbers-grid">
              {drawnNumbers.map((num, index) => (
                <div
                  key={index}
                  className={`drawn-number ${num === currentNumber ? 'latest' : ''}`}
                >
                  {num}
                </div>
              ))}
            </div>
          </div>

          <div className="stats-section">
            <div className="stat-card">
              <div className="stat-label">Remaining</div>
              <div className="stat-value">{availableNumbers.length}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Drawn</div>
              <div className="stat-value">{drawnNumbers.length}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total</div>
              <div className="stat-value">90</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Presenter

