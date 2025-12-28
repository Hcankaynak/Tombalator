import { useState, useEffect } from 'react'
import './Presenter.css'

const API_BASE_URL = 'http://localhost:3000'

function Presenter() {
  const [isAdmin, setIsAdmin] = useState(false)
  const [apiKey, setApiKey] = useState('')
  const [lobbyId, setLobbyId] = useState('')
  const [hasLobby, setHasLobby] = useState(false)
  const [drawnNumbers, setDrawnNumbers] = useState<number[]>([])
  const [currentNumber, setCurrentNumber] = useState<number | null>(null)
  const [isSelecting, setIsSelecting] = useState(false)
  const [availableNumbers, setAvailableNumbers] = useState<number[]>(
    Array.from({ length: 90 }, (_, i) => i + 1)
  )

  useEffect(() => {
    // Check if API key is stored
    const savedApiKey = localStorage.getItem('admin_api_key')
    if (savedApiKey) {
      setApiKey(savedApiKey)
      checkAdminStatus(savedApiKey)
    }
    
    // Check if lobby ID is stored
    const savedLobbyId = localStorage.getItem('presenter_lobby_id')
    if (savedLobbyId) {
      setLobbyId(savedLobbyId)
      setHasLobby(true)
    }
  }, [])

  const checkAdminStatus = async (key: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/admin/check`, {
        method: 'GET',
        headers: {
          'X-API-Key': key,
        },
      })
      const data = await response.json()
      if (data.isAdmin) {
        setIsAdmin(true)
        localStorage.setItem('admin_api_key', key)
      } else {
        setIsAdmin(false)
      }
    } catch (error) {
      console.error('Error checking admin status:', error)
      setIsAdmin(false)
    }
  }

  const handleApiKeySubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (apiKey.trim()) {
      checkAdminStatus(apiKey.trim())
    }
  }

  const selectRandomNumber = () => {
    if (availableNumbers.length === 0 || isSelecting) return

    setIsSelecting(true)
    
    // Mystery animation - shuffle through numbers
    const animationDuration = 2000 // 2 seconds
    const shuffleInterval = 50 // Change number every 50ms
    const iterations = animationDuration / shuffleInterval
    let currentIteration = 0

    const shuffleIntervalId = setInterval(() => {
      const randomIndex = Math.floor(Math.random() * availableNumbers.length)
      setCurrentNumber(availableNumbers[randomIndex])
      currentIteration++

      if (currentIteration >= iterations) {
        clearInterval(shuffleIntervalId)
        
        // Select final number
        const finalIndex = Math.floor(Math.random() * availableNumbers.length)
        const selectedNumber = availableNumbers[finalIndex]
        
        setCurrentNumber(selectedNumber)
        setDrawnNumbers((prev) => [...prev, selectedNumber])
        setAvailableNumbers((prev) => prev.filter((n) => n !== selectedNumber))
        setIsSelecting(false)
        
        // TODO: Send to backend/WebSocket
        console.log('Selected number:', selectedNumber)
      }
    }, shuffleInterval)
  }

  const handleLobbyIdSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (lobbyId.trim()) {
      setHasLobby(true)
      localStorage.setItem('presenter_lobby_id', lobbyId.trim())
      // TODO: Connect to lobby via WebSocket/API
      console.log('Joined lobby:', lobbyId.trim())
    }
  }

  const handleCreateLobby = async () => {
    try {
      const savedApiKey = localStorage.getItem('admin_api_key')
      if (!savedApiKey) {
        alert('Admin API key not found. Please authenticate first.')
        return
      }

      const response = await fetch(`${API_BASE_URL}/api/lobby/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': savedApiKey,
        },
      })

      const data = await response.json()

      if (data.success && data.lobbyId) {
        setLobbyId(data.lobbyId)
        setHasLobby(true)
        localStorage.setItem('presenter_lobby_id', data.lobbyId)
        console.log('Created lobby:', data.lobbyId)
      } else {
        alert(`Failed to create lobby: ${data.message || 'Unknown error'}`)
        console.error('Error creating lobby:', data)
      }
    } catch (error) {
      console.error('Error creating lobby:', error)
      alert('Failed to create lobby. Please try again.')
    }
  }

  const resetGame = () => {
    if (window.confirm('Are you sure you want to reset the game? This will clear all drawn numbers.')) {
      setDrawnNumbers([])
      setCurrentNumber(null)
      setAvailableNumbers(Array.from({ length: 90 }, (_, i) => i + 1))
      // TODO: Send reset to backend
    }
  }

  if (!isAdmin) {
    return (
      <div className="presenter">
        <div className="presenter-auth">
          <h2>Presenter Access</h2>
          <p>Enter your admin API key to access the presenter page</p>
          <form onSubmit={handleApiKeySubmit} className="api-key-form">
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Enter API Key"
              className="api-key-input"
              required
            />
            <button type="submit" className="api-key-button">
              Authenticate
            </button>
          </form>
        </div>
      </div>
    )
  }

  if (!hasLobby) {
    return (
      <div className="presenter">
        <div className="presenter-lobby-setup">
          <h2>Select or Create Lobby</h2>
          <div className="lobby-setup-content">
            <div className="join-lobby-section">
              <h3>Join Existing Lobby</h3>
              <p>Enter a lobby ID to join an existing game</p>
              <form onSubmit={handleLobbyIdSubmit} className="lobby-id-form">
                <input
                  type="text"
                  value={lobbyId}
                  onChange={(e) => setLobbyId(e.target.value)}
                  placeholder="Enter Lobby ID"
                  className="lobby-id-input"
                  required
                />
                <button type="submit" className="lobby-id-button">
                  Join Lobby
                </button>
              </form>
            </div>
            <div className="divider">
              <span>OR</span>
            </div>
            <div className="create-lobby-section">
              <h3>Create New Lobby</h3>
              <p>Create a new lobby to start a game</p>
              <button onClick={handleCreateLobby} className="create-lobby-button">
                Create Lobby
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
            <p className="lobby-info">Lobby: {lobbyId}</p>
          </div>
          <div className="presenter-header-actions">
            <button onClick={() => { setHasLobby(false); localStorage.removeItem('presenter_lobby_id'); }} className="change-lobby-button">
              Change Lobby
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

