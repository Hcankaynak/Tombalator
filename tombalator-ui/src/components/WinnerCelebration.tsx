import { useEffect, useRef } from 'react'
import './WinnerCelebration.css'

interface WinnerCelebrationProps {
  winnerName: string
  onClose: () => void
}

function WinnerCelebration({ winnerName, onClose }: WinnerCelebrationProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set canvas size
    const setCanvasSize = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    }
    setCanvasSize()

    // Confetti particles
    const particles: Array<{
      x: number
      y: number
      vx: number
      vy: number
      color: string
      size: number
      rotation: number
      rotationSpeed: number
    }> = []

    const colors = ['#FFD700', '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F', '#BB8FCE']

    // Create confetti particles
    const createParticles = () => {
      particles.length = 0 // Clear existing particles
      for (let i = 0; i < 150; i++) {
        particles.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height - canvas.height, // Start above screen
          vx: (Math.random() - 0.5) * 2,
          vy: Math.random() * 3 + 2,
          color: colors[Math.floor(Math.random() * colors.length)],
          size: Math.random() * 8 + 4,
          rotation: Math.random() * Math.PI * 2,
          rotationSpeed: (Math.random() - 0.5) * 0.2,
        })
      }
    }
    createParticles()

    let animationId: number | null = null
    let isActive = true

    const animate = () => {
      if (!isActive || !canvas) return

      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)

      particles.forEach((particle) => {
        // Update position
        particle.x += particle.vx
        particle.y += particle.vy
        particle.rotation += particle.rotationSpeed

        // Add gravity
        particle.vy += 0.1

        // Draw particle
        ctx.save()
        ctx.translate(particle.x, particle.y)
        ctx.rotate(particle.rotation)
        ctx.fillStyle = particle.color
        ctx.fillRect(-particle.size / 2, -particle.size / 2, particle.size, particle.size)
        ctx.restore()

        // Reset if off screen (below or to the sides)
        if (particle.y > canvas.height + 50 || 
            particle.x < -50 || 
            particle.x > canvas.width + 50) {
          // Reset particle to top of screen
          particle.y = -10 - Math.random() * 100
          particle.x = Math.random() * canvas.width
          particle.vy = Math.random() * 3 + 2
          particle.vx = (Math.random() - 0.5) * 2
        }
      })

      if (isActive) {
        animationId = requestAnimationFrame(animate)
      }
    }

    animate()

    // Handle window resize
    const handleResize = () => {
      setCanvasSize()
      // Recreate particles with new canvas size
      createParticles()
    }
    window.addEventListener('resize', handleResize)

    return () => {
      isActive = false
      if (animationId !== null) {
        cancelAnimationFrame(animationId)
      }
      window.removeEventListener('resize', handleResize)
    }
  }, [onClose])

  return (
    <div className="winner-celebration-overlay">
      <canvas ref={canvasRef} className="winner-celebration-canvas" />
      <div className="winner-celebration-content">
        <button
          className="winner-celebration-close"
          onClick={onClose}
          aria-label="Close celebration"
        >
          ×
        </button>
        <div className="winner-celebration-title">🎉 Winner! 🎉</div>
        <div className="winner-celebration-name">{winnerName}</div>
        <div className="winner-celebration-subtitle">wins the game!</div>
      </div>
    </div>
  )
}

export default WinnerCelebration

