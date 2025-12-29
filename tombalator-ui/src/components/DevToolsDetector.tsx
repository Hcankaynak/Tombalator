import { useState, useEffect } from 'react'
import './DevToolsDetector.css'

function DevToolsDetector() {
  const [showModal, setShowModal] = useState(false)

  useEffect(() => {
    let devToolsOpen = false
    let checkInterval: ReturnType<typeof setInterval> | null = null

    // Method 1: Check window dimensions (DevTools changes window size)
    const checkDevTools = () => {
      // Check if DevTools is open by comparing outer and inner dimensions
      const widthDiff = window.outerWidth - window.innerWidth
      const heightDiff = window.outerHeight - window.innerHeight
      
      // Threshold: if difference is significant, DevTools is likely open
      const threshold = 160
      const isOpen = widthDiff > threshold || heightDiff > threshold
      
      if (isOpen && !devToolsOpen) {
        devToolsOpen = true
        setShowModal(true)
      }
    }

    // Method 2: Detect DevTools using console detection
    const element = new Image()
    let startTime = Date.now()
    
    Object.defineProperty(element, 'id', {
      get: function() {
        // Only trigger if DevTools is actually inspecting
        if (Date.now() - startTime > 100) {
          if (!devToolsOpen) {
            devToolsOpen = true
            setShowModal(true)
          }
        }
      }
    })

    // Method 3: Detect keyboard shortcuts
    const handleKeyDown = (e: KeyboardEvent) => {
      // F12
      if (e.key === 'F12') {
        e.preventDefault()
        setTimeout(() => {
          if (!devToolsOpen) {
            devToolsOpen = true
            setShowModal(true)
          }
        }, 100)
      }
      // Cmd+Option+I (Mac) or Ctrl+Shift+I (Windows/Linux)
      if ((e.metaKey || e.ctrlKey) && e.shiftKey && (e.key === 'I' || e.key === 'i')) {
        setTimeout(() => {
          if (!devToolsOpen) {
            devToolsOpen = true
            setShowModal(true)
          }
        }, 100)
      }
      // Cmd+Option+J (Mac) or Ctrl+Shift+J (Windows/Linux) - Console
      if ((e.metaKey || e.ctrlKey) && e.shiftKey && (e.key === 'J' || e.key === 'j')) {
        setTimeout(() => {
          if (!devToolsOpen) {
            devToolsOpen = true
            setShowModal(true)
          }
        }, 100)
      }
    }

    // Check periodically
    checkInterval = setInterval(() => {
      checkDevTools()
      // Trigger the getter to detect DevTools inspection
      startTime = Date.now()
      console.log("Oyununu oyna buralara karışma!" + startTime)
    }, 500)

    window.addEventListener('keydown', handleKeyDown)

    // Cleanup
    return () => {
      if (checkInterval) {
        clearInterval(checkInterval)
      }
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [])

  if (!showModal) {
    return null
  }

  return (
    <div className="devtools-detector-overlay">
      <div className="devtools-detector-modal">
        <div className="devtools-detector-content">
          <div className="devtools-detector-emoji">👀</div>
          <h2 className="devtools-detector-title">Ne işler peşindesin düzgünce oynunu oyna</h2>
          <p className="devtools-detector-subtitle">(What are you up to? Play the game properly.)</p>
          <button
            className="devtools-detector-button"
            onClick={() => setShowModal(false)}
          >
            Tamam, tamam! (Okay, okay!)
          </button>
        </div>
      </div>
    </div>
  )
}

export default DevToolsDetector

