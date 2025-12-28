import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './Home.css'

function Home() {
  const [gameId, setGameId] = useState('')
  const navigate = useNavigate()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (gameId.trim()) {
      // Redirect to game page with the game ID
      navigate(`/game/${gameId.trim()}`)
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
            onChange={(e) => setGameId(e.target.value)}
            placeholder="Enter Game ID"
            className="game-input"
            required
          />
          <button type="submit" className="game-button">
            Join
          </button>
        </form>
      </div>
    </div>
  )
}

export default Home

