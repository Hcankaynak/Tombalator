package com.tombalator.game

import kotlin.random.Random

object CardGenerator {
    // Card configuration constants
    const val ROW_COUNT = 3 // Number of rows in a card
    const val COLUMN_COUNT = 9 // Number of columns in a card
    const val NUMBERS_PER_ROW = 5 // Number of numbers per row (rest are empty cells)
    const val MAX_NUMBERS_PER_COLUMN = 3 // Maximum numbers per column across all rows
    const val MIN_NUMBERS_PER_COLUMN = 1 // Minimum numbers per column (each column must have at least 1)
    
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
     * - Configurable number of rows and columns
     * - Each row has a configurable number of numbers (rest are empty)
     * - Each column (decade) can have max numbers per column across all rows
     * - Each column must have at least 1 number
     */
    fun generateCard(): TombalaCard {
        val cardId = "card_${System.currentTimeMillis()}_${Random.nextLong(0, Long.MAX_VALUE).toString(36)}"
        val theme = CARD_THEMES.random()
        
        // Initialize rows with null cells
        val rows = mutableListOf<MutableList<Int?>>()
        for (i in 0 until ROW_COUNT) {
            rows.add(MutableList(COLUMN_COUNT) { null })
        }
        
        // Track how many numbers are used in each column (max per column)
        val columnCounts = IntArray(COLUMN_COUNT) { 0 }
        val allUsedNumbers = mutableSetOf<Int>()
        
        // Step 1: Ensure each column has at least 1 number
        // Assign one number to each column first
        for (colIndex in 0 until COLUMN_COUNT) {
            val range = COLUMN_RANGES[colIndex]
            val availableNumbers = (range.first..range.second).filter { it !in allUsedNumbers }
            
            if (availableNumbers.isNotEmpty()) {
                // Randomly select a row for this column
                val rowIndex = Random.nextInt(0, ROW_COUNT)
                val number = availableNumbers.random()
                
                rows[rowIndex][colIndex] = number
                allUsedNumbers.add(number)
                columnCounts[colIndex]++
            }
        }
        
        // Step 2: Fill remaining positions to get the required numbers per row
        // Each row needs NUMBERS_PER_ROW numbers total, and we've already placed some
        for (rowIndex in 0 until ROW_COUNT) {
            val currentNumbersInRow = rows[rowIndex].count { it != null }
            val numbersNeeded = NUMBERS_PER_ROW - currentNumbersInRow
            
            if (numbersNeeded > 0) {
                // Find empty positions in this row
                val emptyPositions = rows[rowIndex].mapIndexedNotNull { index, value ->
                    if (value == null) index else null
                }.shuffled()
                
                // Try to fill empty positions
                var numbersAdded = 0
                for (colIndex in emptyPositions) {
                    if (numbersAdded >= numbersNeeded) break
                    
                    // Check if we can add a number to this column (max per column)
                    if (columnCounts[colIndex] < MAX_NUMBERS_PER_COLUMN) {
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
        
        // Step 3: Sort each column so numbers are in ascending order from top to bottom
        // We need to maintain the constraint that each row has exactly NUMBERS_PER_ROW numbers
        for (colIndex in 0 until COLUMN_COUNT) {
            // Collect all numbers in this column with their row indices
            val columnNumbers = mutableListOf<Pair<Int, Int>>() // (number, rowIndex)
            for (rowIndex in 0 until ROW_COUNT) {
                val number = rows[rowIndex][colIndex]
                if (number != null) {
                    columnNumbers.add(Pair(number, rowIndex))
                }
            }
            
            if (columnNumbers.size <= 1) {
                // Only one or zero numbers, no sorting needed
                continue
            }
            
            // Sort numbers by value (ascending)
            val sortedNumbers = columnNumbers.map { it.first }.sorted()
            
            // Get the set of rows that originally had numbers in this column
            val originalRows = columnNumbers.map { it.second }.toSet()
            
            // Clear the column
            for (rowIndex in 0 until ROW_COUNT) {
                rows[rowIndex][colIndex] = null
            }
            
            // Count numbers in each row (excluding this column)
            val rowCounts = IntArray(ROW_COUNT) { rowIndex ->
                rows[rowIndex].mapIndexed { idx, value -> if (idx != colIndex && value != null) 1 else 0 }.sum()
            }
            
            // Place sorted numbers in ascending order, starting from top
            // Try to place in rows that originally had numbers, but ensure ascending order
            val sortedRowList = originalRows.sorted() // Rows sorted from top to bottom
            for ((index, number) in sortedNumbers.withIndex()) {
                var placed = false
                
                // Try to place in the row that corresponds to the sorted position
                // (smallest number in topmost row, etc.)
                if (index < sortedRowList.size) {
                    val targetRow = sortedRowList[index]
                    if (rowCounts[targetRow] < NUMBERS_PER_ROW) {
                        rows[targetRow][colIndex] = number
                        rowCounts[targetRow]++
                        placed = true
                    }
                }
                
                // If not placed, find the first available row from top
                if (!placed) {
                    for (rowIndex in 0 until ROW_COUNT) {
                        if (rowCounts[rowIndex] < NUMBERS_PER_ROW) {
                            rows[rowIndex][colIndex] = number
                            rowCounts[rowIndex]++
                            placed = true
                            break
                        }
                    }
                }
            }
            
            // Final pass: Ensure strict ascending order by swapping if necessary
            // This maintains the NUMBERS_PER_ROW constraint since we're only swapping within the column
            for (rowIndex in 0 until ROW_COUNT - 1) {
                val currentNumber = rows[rowIndex][colIndex]
                val nextNumber = rows[rowIndex + 1][colIndex]
                
                if (currentNumber != null && nextNumber != null && currentNumber > nextNumber) {
                    // Swap to maintain ascending order
                    rows[rowIndex][colIndex] = nextNumber
                    rows[rowIndex + 1][colIndex] = currentNumber
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

