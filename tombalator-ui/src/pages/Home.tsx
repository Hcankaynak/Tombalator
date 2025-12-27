import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './Home.css'

function Home() {
  const [lobbyId, setLobbyId] = useState('')
  const navigate = useNavigate()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (lobbyId.trim()) {
      // Redirect to lobby page with the lobby ID
      navigate(`/lobby/${lobbyId.trim()}`)
    }
  }

  return (
    <div className="home">
      <div className="home-container">
        <h2 className="home-title">Join a Lobby</h2>
        <form onSubmit={handleSubmit} className="lobby-form">
          <input
            type="text"
            value={lobbyId}
            onChange={(e) => setLobbyId(e.target.value)}
            placeholder="Enter Lobby ID"
            className="lobby-input"
            required
          />
          <button type="submit" className="lobby-button">
            Join
          </button>
        </form>
      </div>
    </div>
  )
}

export default Home

