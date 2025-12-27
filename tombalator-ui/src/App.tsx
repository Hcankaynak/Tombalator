import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from './contexts/ThemeContext'
import { AdminProvider } from './contexts/AdminContext'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home from './pages/Home'
import Lobby from './pages/Lobby'
import Presenter from './pages/Presenter'
import './App.css'

function App() {
  return (
    <ThemeProvider>
      <AdminProvider>
        <Router>
          <div className="app">
            <Navbar />
            <main className="main-content">
              <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/lobby/:lobbyId" element={<Lobby />} />
                <Route path="/presenter" element={<Presenter />} />
              </Routes>
            </main>
            <Footer />
          </div>
        </Router>
      </AdminProvider>
    </ThemeProvider>
  )
}

export default App
