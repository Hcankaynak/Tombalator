package com.tombalator.websocket

import com.tombalator.game.GameManager
import com.tombalator.models.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.tombalator.websocket.WebSocketHandler")

class WebSocketHandler(
    private val gameId: String,
    private val session: DefaultWebSocketSession
) {
    private var userId: String? = null
    private var username: String? = null
    
    suspend fun handleJoin(message: JoinGameMessage) {
        // Validate that the message gameId matches the URL gameId
        if (message.gameId != gameId) {
            logger.warn("WebSocket join - Game ID mismatch. Expected: $gameId, got: ${message.gameId}")
            sendError("Game ID mismatch. Expected: $gameId, got: ${message.gameId}")
            return
        }
        
        // Check if game exists
        if (!GameManager.gameExists(gameId)) {
            logger.warn("WebSocket join - Game '$gameId' does not exist. User: ${message.username}")
            sendError("Game with ID '$gameId' does not exist. Please create the game first.")
            return
        }
        
        // Check if username is already taken in this game
        if (WebSocketManager.isUsernameTaken(gameId, message.username)) {
            logger.warn("WebSocket join - Username '${message.username}' is already taken in game '$gameId'")
            sendError("Username '${message.username}' is already taken in this game. Please choose a different nickname.")
            return
        }
        
        userId = message.userId
        username = message.username
        
        logger.info("WebSocket join - User '${message.username}' (${message.userId}) joined game '$gameId'")
        
        // Add connection to manager
        WebSocketManager.addConnection(
            gameId = gameId,
            userId = message.userId,
            username = message.username,
            session = session
        )
        
        // Broadcast updated players list to all in game
        broadcastPlayersUpdate()
    }
    
    private suspend fun broadcastPlayersUpdate() {
        val connections = WebSocketManager.getConnectionsForGame(gameId)
        val players = connections.map { connection ->
            PlayerInfo(
                userId = connection.userId,
                username = connection.username
            )
        }
        
        val updateMessage = PlayersUpdateMessage(players = players)
        WebSocketManager.broadcastToGame(
            gameId,
            WebSocketCodec.encode(updateMessage)
        )
    }
    
    suspend fun handleChat(message: ChatMessage) {
        if (userId == null || username == null) {
            logger.warn("WebSocket chat - Unauthenticated user attempted to send message in game '$gameId'")
            sendError("Not authenticated. Please join game first.")
            return
        }
        
        logger.info("WebSocket chat - User '${username}' sent message in game '$gameId': ${message.message}")
        
        // Create chat message with current user info
        val chatMsg = ChatMessage(
            userId = userId!!,
            username = username!!,
            message = message.message,
            timestamp = System.currentTimeMillis()
        )
        
        // Broadcast to all in game
        WebSocketManager.broadcastToGame(
            gameId,
            WebSocketCodec.encode(chatMsg)
        )
    }
    
    suspend fun handleSelectCard(message: SelectCardMessage) {
        if (userId == null || username == null) {
            logger.warn("WebSocket select_card - Unauthenticated user attempted to select card in game '$gameId'")
            sendError("Not authenticated. Please join game first.")
            return
        }
        
        // Validate that the message userId matches the current user
        if (message.userId != userId) {
            logger.warn("WebSocket select_card - User ID mismatch. Expected: $userId, got: ${message.userId}")
            sendError("User ID mismatch.")
            return
        }
        
        // Convert TombalaCardData to TombalaCard (game model)
        val card = com.tombalator.game.TombalaCard(
            id = message.card.id,
            rows = message.card.rows,
            theme = message.card.theme
        )
        
        // Store the card in GameManager
        val success = GameManager.setUserCard(gameId, userId!!, card)
        
        if (success) {
            logger.info("WebSocket select_card - User '${username}' selected card '${card.id}' in game '$gameId'")
            // Optionally send a confirmation message back
            // For now, we'll just log it
        } else {
            logger.warn("WebSocket select_card - Failed to store card for user '${username}' in game '$gameId'")
            sendError("Failed to store card. Game may not exist.")
        }
    }
    
    suspend fun handleLeave() {
        if (userId == null || username == null) {
            return
        }
        
        logger.info("WebSocket leave - User '$username' left game '$gameId'")
        
        // Remove connection
        WebSocketManager.removeConnection(session)
        
        // Remove user's card when they leave
        if (userId != null) {
            GameManager.removeUserCard(gameId, userId!!)
        }
        
        // Broadcast updated players list to remaining players
        broadcastPlayersUpdate()
    }
    
    suspend fun handleMessage(message: WebSocketMessage): Boolean {
        return when (message) {
            is JoinGameMessage -> {
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
            is SelectCardMessage -> {
                handleSelectCard(message)
                true
            }
            is LeaveGameMessage -> {
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

