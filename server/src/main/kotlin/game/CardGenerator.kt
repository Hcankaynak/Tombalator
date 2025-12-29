package com.tombalator.game

import kotlin.random.Random

object CardGenerator {
    // Card color themes
    private val CARD_THEMES = listOf(
        "theme-red",
        "theme-blue",
        "theme-green",
        "theme-purple",
        "theme-orange",
        "theme-teal",
        "theme-pink",
        "theme-yellow"
    )

    // Column ranges for Tombala (1-9, 10-19, 20-29, ..., 80-90)
    private val COLUMN_RANGES = listOf(
        1 to 9,
        10 to 19,
        20 to 29,
        30 to 39,
        40 to 49,
        50 to 59,
        60 to 69,
        70 to 79,
        80 to 90
    )

    /**
     * Generates a single Tombala card following the rules:
     * - 15 numbers total (5 per row × 3 rows)
     * - Each column (decade) can have max 3 numbers across all rows
     * - Each column must have at least 1 number
     */
    fun generateCard(): TombalaCard {
        val cardId = "card_${System.currentTimeMillis()}_${Random.nextLong(0, Long.MAX_VALUE).toString(36)}"
        val theme = CARD_THEMES.random()
        
        // Initialize 3 rows with 9 null cells each
        val rows = mutableListOf<MutableList<Int?>>()
        for (i in 0 until 3) {
            rows.add(MutableList(9) { null })
        }
        
        // Track how many numbers are used in each column (max 3 per column)
        val columnCounts = IntArray(9) { 0 }
        val allUsedNumbers = mutableSetOf<Int>()
        
        // Step 1: Ensure each column has at least 1 number
        // Assign one number to each column first
        for (colIndex in 0 until 9) {
            val range = COLUMN_RANGES[colIndex]
            val availableNumbers = (range.first..range.second).filter { it !in allUsedNumbers }
            
            if (availableNumbers.isNotEmpty()) {
                // Randomly select a row for this column
                val rowIndex = Random.nextInt(0, 3)
                val number = availableNumbers.random()
                
                rows[rowIndex][colIndex] = number
                allUsedNumbers.add(number)
                columnCounts[colIndex]++
            }
        }
        
        // Step 2: Fill remaining positions to get 5 numbers per row
        // Each row needs 5 numbers total, and we've already placed some
        for (rowIndex in 0 until 3) {
            val currentNumbersInRow = rows[rowIndex].count { it != null }
            val numbersNeeded = 5 - currentNumbersInRow
            
            if (numbersNeeded > 0) {
                // Find empty positions in this row
                val emptyPositions = rows[rowIndex].mapIndexedNotNull { index, value ->
                    if (value == null) index else null
                }.shuffled()
                
                // Try to fill empty positions
                var numbersAdded = 0
                for (colIndex in emptyPositions) {
                    if (numbersAdded >= numbersNeeded) break
                    
                    // Check if we can add a number to this column (max 3 per column)
                    if (columnCounts[colIndex] < 3) {
                        val range = COLUMN_RANGES[colIndex]
                        val availableNumbers = (range.first..range.second)
                            .filter { it !in allUsedNumbers }
                        
                        if (availableNumbers.isNotEmpty()) {
                            val number = availableNumbers.random()
                            rows[rowIndex][colIndex] = number
                            allUsedNumbers.add(number)
                            columnCounts[colIndex]++
                            numbersAdded++
                        }
                    }
                }
            }
        }
        
        // Convert to immutable lists
        val finalRows = rows.map { it.toList() }
        
        return TombalaCard(
            id = cardId,
            rows = finalRows,
            theme = theme
        )
    }

    /**
     * Generates multiple cards
     */
    fun generateCards(count: Int): List<TombalaCard> {
        return (0 until count).map { generateCard() }
    }
}

