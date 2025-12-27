package com.tombalator.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap

data class ConnectionInfo(
    val session: DefaultWebSocketSession,
    val lobbyId: String,
    val userId: String,
    val username: String
)

object WebSocketManager {
    // Map of lobbyId -> Set of connections
    private val lobbyConnections = ConcurrentHashMap<String, MutableSet<ConnectionInfo>>()
    
    // Map of session -> ConnectionInfo for quick lookup
    private val sessionToConnection = ConcurrentHashMap<DefaultWebSocketSession, ConnectionInfo>()
    
    fun addConnection(lobbyId: String, userId: String, username: String, session: DefaultWebSocketSession) {
        val connection = ConnectionInfo(session, lobbyId, userId, username)
        
        lobbyConnections.getOrPut(lobbyId) { mutableSetOf() }.add(connection)
        sessionToConnection[session] = connection
    }
    
    fun removeConnection(session: DefaultWebSocketSession) {
        val connection = sessionToConnection.remove(session) ?: return
        
        lobbyConnections[connection.lobbyId]?.remove(connection)
        
        // Clean up empty lobby sets
        if (lobbyConnections[connection.lobbyId]?.isEmpty() == true) {
            lobbyConnections.remove(connection.lobbyId)
        }
    }
    
    fun getConnectionsForLobby(lobbyId: String): Set<ConnectionInfo> {
        return lobbyConnections[lobbyId]?.toSet() ?: emptySet()
    }
    
    fun getAllConnections(): Set<ConnectionInfo> {
        return sessionToConnection.values.toSet()
    }
    
    suspend fun broadcastToLobby(lobbyId: String, message: String) {
        val connections = getConnectionsForLobby(lobbyId)
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

