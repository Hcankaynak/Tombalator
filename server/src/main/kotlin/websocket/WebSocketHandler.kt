package com.tombalator.websocket

import com.tombalator.lobby.LobbyManager
import com.tombalator.models.*
import io.ktor.websocket.*

class WebSocketHandler(
    private val lobbyId: String,
    private val session: DefaultWebSocketSession
) {
    private var userId: String? = null
    private var username: String? = null
    
    suspend fun handleJoin(message: JoinLobbyMessage) {
        // Validate that the message lobbyId matches the URL lobbyId
        if (message.lobbyId != lobbyId) {
            sendError("Lobby ID mismatch. Expected: $lobbyId, got: ${message.lobbyId}")
            return
        }
        
        // Check if lobby exists
        if (!LobbyManager.lobbyExists(lobbyId)) {
            sendError("Lobby with ID '$lobbyId' does not exist. Please create the lobby first.")
            return
        }
        
        userId = message.userId
        username = message.username
        
        // Add connection to manager
        WebSocketManager.addConnection(
            lobbyId = lobbyId,
            userId = message.userId,
            username = message.username,
            session = session
        )
        
        // Broadcast updated players list to all in lobby
        broadcastPlayersUpdate()
    }
    
    private suspend fun broadcastPlayersUpdate() {
        val connections = WebSocketManager.getConnectionsForLobby(lobbyId)
        val players = connections.map { connection ->
            PlayerInfo(
                userId = connection.userId,
                username = connection.username
            )
        }
        
        val updateMessage = PlayersUpdateMessage(players = players)
        WebSocketManager.broadcastToLobby(
            lobbyId,
            WebSocketCodec.encode(updateMessage)
        )
    }
    
    suspend fun handleChat(message: ChatMessage) {
        if (userId == null || username == null) {
            sendError("Not authenticated. Please join lobby first.")
            return
        }
        
        // Create chat message with current user info
        val chatMsg = ChatMessage(
            userId = userId!!,
            username = username!!,
            message = message.message,
            timestamp = System.currentTimeMillis()
        )
        
        // Broadcast to all in lobby
        WebSocketManager.broadcastToLobby(
            lobbyId,
            WebSocketCodec.encode(chatMsg)
        )
    }
    
    suspend fun handleLeave() {
        if (userId == null || username == null) {
            return
        }
        
        // Remove connection
        WebSocketManager.removeConnection(session)
        
        // Broadcast updated players list to remaining players
        broadcastPlayersUpdate()
    }
    
    suspend fun handleMessage(message: WebSocketMessage): Boolean {
        return when (message) {
            is JoinLobbyMessage -> {
                if (userId != null) {
                    sendError("Already joined. Cannot join again.")
                    false
                } else {
                    handleJoin(message)
                    true
                }
            }
            is ChatMessage -> {
                handleChat(message)
                true
            }
            is LeaveLobbyMessage -> {
                handleLeave()
                false // Return false to indicate connection should close
            }
            else -> {
                sendError("Unknown message type: ${message::class.simpleName}")
                true
            }
        }
    }
    
    suspend fun sendError(errorMessage: String) {
        val error = ErrorMessage(message = errorMessage)
        WebSocketCodec.sendMessage(session, error)
    }
    
    fun isAuthenticated(): Boolean {
        return userId != null && username != null
    }
    
    fun cleanup() {
        WebSocketManager.removeConnection(session)
    }
}

