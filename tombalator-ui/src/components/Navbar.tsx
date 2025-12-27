import { Link } from 'react-router-dom'
import { useTheme } from '../contexts/ThemeContext'
import { useAdmin } from '../contexts/AdminContext'
import './Navbar.css'

function Navbar() {
  const { theme, toggleTheme } = useTheme()
  const { isAdmin } = useAdmin()

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-title-link">
          <h1 className="navbar-title">Tombalator</h1>
        </Link>
        <div className="navbar-right">
          {isAdmin && (
            <Link to="/presenter" className="presenter-link">
              Presenter
            </Link>
          )}
          <button className="theme-toggle" onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
        </div>
      </div>
    </nav>
  )
}

export default Navbar

