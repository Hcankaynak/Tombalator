package com.tombalator.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap

data class ConnectionInfo(
    val session: DefaultWebSocketSession,
    val gameId: String,
    val userId: String,
    val username: String
)

object WebSocketManager {
    // Map of gameId -> Set of connections
    private val gameConnections = ConcurrentHashMap<String, MutableSet<ConnectionInfo>>()
    
    // Map of session -> ConnectionInfo for quick lookup
    private val sessionToConnection = ConcurrentHashMap<DefaultWebSocketSession, ConnectionInfo>()
    
    fun addConnection(gameId: String, userId: String, username: String, session: DefaultWebSocketSession) {
        val connection = ConnectionInfo(session, gameId, userId, username)
        
        gameConnections.getOrPut(gameId) { mutableSetOf() }.add(connection)
        sessionToConnection[session] = connection
    }
    
    fun removeConnection(session: DefaultWebSocketSession) {
        val connection = sessionToConnection.remove(session) ?: return
        
        gameConnections[connection.gameId]?.remove(connection)
        
        // Clean up empty game sets
        if (gameConnections[connection.gameId]?.isEmpty() == true) {
            gameConnections.remove(connection.gameId)
        }
    }
    
    fun getConnectionsForGame(gameId: String): Set<ConnectionInfo> {
        return gameConnections[gameId]?.toSet() ?: emptySet()
    }
    
    fun getAllConnections(): Set<ConnectionInfo> {
        return sessionToConnection.values.toSet()
    }
    
    suspend fun broadcastToGame(gameId: String, message: String) {
        val connections = getConnectionsForGame(gameId)
        connections.forEach { connection ->
            try {
                connection.session.send(Frame.Text(message))
            } catch (e: Exception) {
                // Connection might be closed, remove it
                removeConnection(connection.session)
            }
        }
    }
    
    suspend fun sendToUser(session: DefaultWebSocketSession, message: String) {
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            removeConnection(session)
        }
    }
}

