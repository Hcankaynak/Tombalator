import type { TombalaCard } from './CardSelection'
import './PlayerCard.css'

interface PlayerCardProps {
  card: TombalaCard
  markedNumbers?: Set<number>
  drawnNumbers?: number[]
  onNumberClick?: (number: number) => void
}

function PlayerCard({ 
  card, 
  markedNumbers = new Set(),
  drawnNumbers = [],
  onNumberClick
}: PlayerCardProps) {
  const theme = card.theme || 'theme-red'

  const handleCellClick = (number: number | null) => {
    if (number && onNumberClick) {
      onNumberClick(number)
    }
  }

  const isDrawn = (number: number | null): boolean => {
    return number !== null && drawnNumbers.includes(number)
  }

  const isMarked = (number: number | null): boolean => {
    return number !== null && markedNumbers.has(number)
  }

  const isClickable = (number: number | null): boolean => {
    // Only clickable if: has number, is drawn, not already marked, and has click handler
    return number !== null && isDrawn(number) && !isMarked(number) && onNumberClick !== undefined
  }

  return (
    <div className={`player-card ${theme}`}>
      <div className="player-card-header">
        <h3>Your Card</h3>
      </div>
      <div className="player-card-content">
        {card.rows.map((row, rowIndex) => (
          <div key={rowIndex} className="player-card-row">
            {row.map((cell, cellIndex) => (
              <div
                key={cellIndex}
                className={`player-card-cell ${
                  cell ? 'has-number' : 'empty'
                } ${cell && isDrawn(cell) ? 'drawn' : ''} ${
                  cell && isMarked(cell) ? 'marked' : ''
                } ${isClickable(cell) ? 'clickable' : ''}`}
                onClick={() => handleCellClick(cell)}
                title={cell && isDrawn(cell) ? 'Click to mark/unmark' : ''}
              >
                {cell || <span className="empty-placeholder">—</span>}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

export default PlayerCard

