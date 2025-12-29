package com.tombalator.game

import java.util.concurrent.ConcurrentHashMap

data class TombalaCard(
    val id: String,
    val rows: List<List<Int?>>, // null represents empty cell
    val theme: String? = null
)

data class Game(
    val gameId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val drawnNumbers: MutableSet<Int> = mutableSetOf(),
    val userCards: MutableMap<String, TombalaCard> = ConcurrentHashMap(), // userId -> card
    val userClosedNumbers: MutableMap<String, MutableSet<Int>> = ConcurrentHashMap(), // userId -> closed numbers
    val userCompletedRows: MutableMap<String, MutableSet<Int>> = ConcurrentHashMap(), // userId -> completed row indices (0-2)
    val gameWinners: MutableSet<String> = ConcurrentHashMap.newKeySet() // userIds who have won the game
)

object GameManager {
    private val games = ConcurrentHashMap<String, Game>()

    /**
     * Generates a unique 4-digit game ID
     * Returns null if all 4-digit numbers (1000-9999) are taken (very unlikely)
     */
    private fun generateUniqueGameId(): String? {
        var attempts = 0
        val maxAttempts = 100 // Prevent infinite loop

        while (attempts < maxAttempts) {
            // Generate random 4-digit number (1000-9999)
            val gameId = (1000..9999).random().toString()

            if (!games.containsKey(gameId)) {
                return gameId
            }
            attempts++
        }

        return null // All IDs taken (extremely unlikely)
    }

    /**
     * Creates a new game with a generated 4-digit ID
     * Returns the game ID if created successfully, null if all IDs are taken
     */
    fun createGame(): String? {
        val gameId = generateUniqueGameId() ?: return null
        games[gameId] = Game(gameId = gameId)
        return gameId
    }

    /**
     * Creates a new game with the given ID
     * Returns true if created successfully, false if game already exists
     */
    fun createGame(gameId: String): Boolean {
        return if (games.containsKey(gameId)) {
            false // Game already exists
        } else {
            games[gameId] = Game(gameId = gameId)
            true
        }
    }

    /**
     * Checks if a game exists
     */
    fun gameExists(gameId: String): Boolean {
        return games.containsKey(gameId)
    }

    /**
     * Gets a game by ID
     */
    fun getGame(gameId: String): Game? {
        return games[gameId]
    }

    /**
     * Removes a game (when empty or admin action)
     */
    fun removeGame(gameId: String): Boolean {
        return games.remove(gameId) != null
    }

    /**
     * Gets all games
     */
    fun getAllGames(): List<Game> {
        return games.values.toList()
    }

    /**
     * Draws a random number for a game (1-90)
     * Returns the drawn number, or null if all numbers have been drawn
     */
    fun drawNumber(gameId: String): Int? {
        val game = games[gameId] ?: return null

        // Generate available numbers (1-90) that haven't been drawn
        val availableNumbers = (1..90).filter { it !in game.drawnNumbers }

        if (availableNumbers.isEmpty()) {
            return null // All numbers have been drawn
        }

        // Select a random number from available numbers
        val drawnNumber = availableNumbers.random()

        // Add to drawn numbers
        game.drawnNumbers.add(drawnNumber)

        return drawnNumber
    }

    /**
     * Gets all drawn numbers for a game (in order they were drawn, not sorted)
     */
    fun getDrawnNumbers(gameId: String): List<Int> {
        return games[gameId]?.drawnNumbers?.toList() ?: emptyList()
    }

    /**
     * Resets drawn numbers for a game
     */
    fun resetDrawnNumbers(gameId: String): Boolean {
        val game = games[gameId] ?: return false
        game.drawnNumbers.clear()
        return true
    }

    /**
     * Stores a user's selected card for a game
     * Returns true if stored successfully, false if game doesn't exist
     */
    fun setUserCard(gameId: String, userId: String, card: TombalaCard): Boolean {
        val game = games[gameId] ?: return false
        game.userCards[userId] = card
        return true
    }

    /**
     * Gets a user's card for a game
     * Returns the card if found, null otherwise
     */
    fun getUserCard(gameId: String, userId: String): TombalaCard? {
        return games[gameId]?.userCards?.get(userId)
    }

    /**
     * Removes a user's card from a game
     * Returns true if removed successfully, false if game or user doesn't exist
     */
    fun removeUserCard(gameId: String, userId: String): Boolean {
        val game = games[gameId] ?: return false
        return game.userCards.remove(userId) != null
    }

    /**
     * Marks a number as closed for a user
     * Returns true if closed successfully, false if game doesn't exist
     */
    fun closeNumberForUser(gameId: String, userId: String, number: Int): Boolean {
        val game = games[gameId] ?: return false
        game.userClosedNumbers.getOrPut(userId) { mutableSetOf() }.add(number)
        return true
    }

    /**
     * Gets all closed numbers for a user
     * Returns the set of closed numbers, or empty set if game/user doesn't exist
     */
    fun getClosedNumbersForUser(gameId: String, userId: String): Set<Int> {
        return games[gameId]?.userClosedNumbers?.get(userId)?.toSet() ?: emptySet()
    }

    /**
     * Clears all closed numbers for a user (e.g., when they change card)
     * Returns true if cleared successfully, false if game doesn't exist
     */
    fun clearClosedNumbersForUser(gameId: String, userId: String): Boolean {
        val game = games[gameId] ?: return false
        game.userClosedNumbers[userId]?.clear()
        return true
    }

    /**
     * Checks if a number is already closed for a user
     * Returns true if closed, false otherwise
     */
    fun isNumberClosedForUser(gameId: String, userId: String, number: Int): Boolean {
        return games[gameId]?.userClosedNumbers?.get(userId)?.contains(number) ?: false
    }

    /**
     * Checks if a user has completed any row (çinko) that hasn't been announced yet
     * Returns the row index (0-2) if a new row is complete, null otherwise
     * A row is complete when all numbers in that row are closed
     */
    fun checkCompletedRow(gameId: String, userId: String): Int? {
        val game = games[gameId] ?: return null
        val userCard = game.userCards[userId] ?: return null
        val closedNumbers = game.userClosedNumbers[userId] ?: return null
        val completedRows = game.userCompletedRows.getOrPut(userId) { mutableSetOf() }

        // Check each row
        for (rowIndex in userCard.rows.indices) {
            // Skip if this row was already announced as completed
            if (completedRows.contains(rowIndex)) {
                continue
            }

            val row = userCard.rows[rowIndex]
            // Get all numbers in this row (excluding nulls)
            val rowNumbers = row.filterNotNull()

            // Check if all numbers in this row are closed
            val allClosed = rowNumbers.isNotEmpty() && rowNumbers.all { number ->
                closedNumbers.contains(number)
            }

            if (allClosed) {
                // Mark this row as completed
                completedRows.add(rowIndex)
                return rowIndex
            }
        }

        return null
    }

    /**
     * Clears completed rows for a user (e.g., when they change card)
     * Returns true if cleared successfully, false if game doesn't exist
     */
    fun clearCompletedRowsForUser(gameId: String, userId: String): Boolean {
        val game = games[gameId] ?: return false
        game.userCompletedRows[userId]?.clear()
        return true
    }

    /**
     * Checks if a user has closed all numbers on their card (won the game)
     * Returns true if the user has won and hasn't been announced yet, false otherwise
     */
    fun checkGameWin(gameId: String, userId: String): Boolean {
        val game = games[gameId] ?: return false
        val userCard = game.userCards[userId] ?: return false
        val closedNumbers = game.userClosedNumbers[userId] ?: return false

        // Check if user has already won
        if (game.gameWinners.contains(userId)) {
            return false // Already announced as winner
        }

        // Get all numbers on the card (excluding nulls)
        val allCardNumbers = userCard.rows.flatMap { row ->
            row.filterNotNull()
        }

        // Check if all numbers on the card are closed
        val allClosed = allCardNumbers.isNotEmpty() && allCardNumbers.all { number ->
            closedNumbers.contains(number)
        }

        if (allClosed) {
            // Mark user as winner
            game.gameWinners.add(userId)
            return true
        }

        return false
    }

    /**
     * Clears win status for a user (e.g., when they change card)
     * Returns true if cleared successfully, false if game doesn't exist
     */
    fun clearWinForUser(gameId: String, userId: String): Boolean {
        val game = games[gameId] ?: return false
        game.gameWinners.remove(userId)
        return true
    }
}
