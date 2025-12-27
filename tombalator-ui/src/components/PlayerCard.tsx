import type { TombalaCard } from './CardSelection'
import './PlayerCard.css'

interface PlayerCardProps {
  card: TombalaCard
  markedNumbers?: Set<number>
}

function PlayerCard({ card, markedNumbers = new Set() }: PlayerCardProps) {
  const theme = card.theme || 'theme-red'

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
                } ${cell && markedNumbers.has(cell) ? 'marked' : ''}`}
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

