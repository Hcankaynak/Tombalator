package com.tombalator.game

import java.util.concurrent.ConcurrentHashMap

data class Game(
    val gameId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val drawnNumbers: MutableSet<Int> = mutableSetOf()
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
     * Gets all drawn numbers for a game
     */
    fun getDrawnNumbers(gameId: String): List<Int> {
        return games[gameId]?.drawnNumbers?.toList()?.sorted() ?: emptyList()
    }
    
    /**
     * Resets drawn numbers for a game
     */
    fun resetDrawnNumbers(gameId: String): Boolean {
        val game = games[gameId] ?: return false
        game.drawnNumbers.clear()
        return true
    }
}
