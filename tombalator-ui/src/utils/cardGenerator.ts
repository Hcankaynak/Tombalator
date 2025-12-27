import type { TombalaCard } from '../components/CardSelection'

// Card color themes
const CARD_THEMES = [
  'theme-red',
  'theme-blue',
  'theme-green',
  'theme-purple',
  'theme-orange',
  'theme-teal',
  'theme-pink',
  'theme-yellow',
]

// Generate a random number in a specific range
function getRandomInRange(min: number, max: number, exclude: Set<number>): number {
  const available = Array.from({ length: max - min + 1 }, (_, i) => min + i).filter(
    (n) => !exclude.has(n)
  )
  return available[Math.floor(Math.random() * available.length)]
}

// Generate a single Tombala card
export function generateTombalaCard(): TombalaCard {
  const cardId = `card_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  const rows: (number | null)[][] = []
  const usedNumbers = new Set<number>()
  const theme = CARD_THEMES[Math.floor(Math.random() * CARD_THEMES.length)]

  // Column ranges for Tombala (1-9, 10-19, 20-29, ..., 80-90)
  const columnRanges = [
    { min: 1, max: 9 },
    { min: 10, max: 19 },
    { min: 20, max: 29 },
    { min: 30, max: 39 },
    { min: 40, max: 49 },
    { min: 50, max: 59 },
    { min: 60, max: 69 },
    { min: 70, max: 79 },
    { min: 80, max: 90 },
  ]

  // Generate 3 rows
  for (let rowIndex = 0; rowIndex < 3; rowIndex++) {
    const row: (number | null)[] = []
    const rowUsedNumbers = new Set<number>()

    // Each row has 5 numbers and 4 empty cells
    const numberPositions = [0, 1, 2, 3, 4, 5, 6, 7, 8]
      .sort(() => Math.random() - 0.5)
      .slice(0, 5)
      .sort((a, b) => a - b)

    for (let colIndex = 0; colIndex < 9; colIndex++) {
      if (numberPositions.includes(colIndex)) {
        // Place a number from the column's range
        const range = columnRanges[colIndex]
        const number = getRandomInRange(range.min, range.max, rowUsedNumbers)
        row.push(number)
        rowUsedNumbers.add(number)
        usedNumbers.add(number)
      } else {
        // Empty cell
        row.push(null)
      }
    }

    rows.push(row)
  }

  return {
    id: cardId,
    rows,
    theme,
  }
}

// Generate multiple cards
export function generateCards(count: number): TombalaCard[] {
  return Array.from({ length: count }, () => generateTombalaCard())
}

