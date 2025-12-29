import { useState } from 'react'
import './CardSelection.css'

export interface TombalaCard {
  id: string
  rows: (number | null)[][]
  theme?: string
}

interface CardSelectionProps {
  cards: TombalaCard[]
  onSelectCard: (card: TombalaCard) => void
  onRefresh?: () => void
  isRefreshing?: boolean
}

function CardSelection({ cards, onSelectCard, onRefresh, isRefreshing = false }: CardSelectionProps) {
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null)

  const handleSelect = (card: TombalaCard) => {
    setSelectedCardId(card.id)
  }

  const handleConfirm = () => {
    const selectedCard = cards.find((card) => card.id === selectedCardId)
    if (selectedCard) {
      onSelectCard(selectedCard)
    }
  }

  const handleRefresh = () => {
    setSelectedCardId(null) // Clear selection when refreshing
    onRefresh?.()
  }

  return (
    <div className="card-selection-overlay">
      <div className="card-selection-modal">
        <div className="card-selection-header">
          <div>
            <h2 className="card-selection-title">Select Your Card</h2>
            <p className="card-selection-subtitle">Choose one card to play with</p>
          </div>
          {onRefresh && (
            <button
              className="refresh-cards-button"
              onClick={handleRefresh}
              disabled={isRefreshing}
              title="Refresh cards"
            >
              {isRefreshing ? '⟳' : '↻'}
            </button>
          )}
        </div>
        <div className="cards-grid">
          {cards.map((card) => (
            <div
              key={card.id}
              className={`card-option ${
                selectedCardId === card.id ? 'selected' : ''
              }`}
              onClick={() => handleSelect(card)}
            >
              <div className={`card-preview ${card.theme || 'theme-red'}`}>
                {card.rows.map((row, rowIndex) => (
                  <div key={rowIndex} className="card-row">
                    {row.map((cell, cellIndex) => (
                      <div
                        key={cellIndex}
                        className={`card-cell ${cell ? 'has-number' : 'empty'}`}
                      >
                        {cell || <span className="empty-placeholder">—</span>}
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
        <button
          className="confirm-card-button"
          onClick={handleConfirm}
          disabled={!selectedCardId}
        >
          Confirm Selection
        </button>
      </div>
    </div>
  )
}

export default CardSelection

