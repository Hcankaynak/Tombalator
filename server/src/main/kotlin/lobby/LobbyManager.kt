package com.tombalator.lobby

import java.util.concurrent.ConcurrentHashMap

data class Lobby(
    val lobbyId: String,
    val createdAt: Long = System.currentTimeMillis()
)

object LobbyManager {
    private val lobbies = ConcurrentHashMap<String, Lobby>()
    
    /**
     * Generates a unique 4-digit lobby ID
     * Returns null if all 4-digit numbers (1000-9999) are taken (very unlikely)
     */
    private fun generateUniqueLobbyId(): String? {
        var attempts = 0
        val maxAttempts = 100 // Prevent infinite loop
        
        while (attempts < maxAttempts) {
            // Generate random 4-digit number (1000-9999)
            val lobbyId = (1000..9999).random().toString()
            
            if (!lobbies.containsKey(lobbyId)) {
                return lobbyId
            }
            attempts++
        }
        
        return null // All IDs taken (extremely unlikely)
    }
    
    /**
     * Creates a new lobby with a generated 4-digit ID
     * Returns the lobby ID if created successfully, null if all IDs are taken
     */
    fun createLobby(): String? {
        val lobbyId = generateUniqueLobbyId() ?: return null
        lobbies[lobbyId] = Lobby(lobbyId = lobbyId)
        return lobbyId
    }
    
    /**
     * Creates a new lobby with the given ID
     * Returns true if created successfully, false if lobby already exists
     */
    fun createLobby(lobbyId: String): Boolean {
        return if (lobbies.containsKey(lobbyId)) {
            false // Lobby already exists
        } else {
            lobbies[lobbyId] = Lobby(lobbyId = lobbyId)
            true
        }
    }
    
    /**
     * Checks if a lobby exists
     */
    fun lobbyExists(lobbyId: String): Boolean {
        return lobbies.containsKey(lobbyId)
    }
    
    /**
     * Gets a lobby by ID
     */
    fun getLobby(lobbyId: String): Lobby? {
        return lobbies[lobbyId]
    }
    
    /**
     * Removes a lobby (when empty or admin action)
     */
    fun removeLobby(lobbyId: String): Boolean {
        return lobbies.remove(lobbyId) != null
    }
    
    /**
     * Gets all lobbies
     */
    fun getAllLobbies(): List<Lobby> {
        return lobbies.values.toList()
    }
}

