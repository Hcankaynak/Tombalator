package com.tombalator

import com.tombalator.config.Config
import com.tombalator.lobby.LobbyManager
import com.tombalator.models.*
import com.tombalator.websocket.WebSocketCodec
import com.tombalator.websocket.WebSocketHandler
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.tombalator.Routing")

@Serializable
data class IsAdminResponse(
    val isAdmin: Boolean
)

@Serializable
data class TestRequest(
    val text: String
)

@Serializable
data class TestResponse(
    val received: String,
    val message: String
)

@Serializable
data class CreateLobbyResponse(
    val success: Boolean,
    val lobbyId: String?,
    val message: String
)

fun Application.configureRouting() {
    routing {
        get("/") {
            logger.info("GET / - Root endpoint accessed")
            call.respondText("Hello Worldss!")
        }
        
        get("/api/admin/check") {
            val apiKey = call.request.header("X-API-Key") 
                ?: call.request.queryParameters["apiKey"]
            
            val isAdmin = apiKey != null && apiKey == Config.ADMIN_API_KEY
            logger.info("GET /api/admin/check - Admin check: ${if (isAdmin) "AUTHORIZED" else "UNAUTHORIZED"}")
            
            call.respond(IsAdminResponse(isAdmin))
        }
        
        post("/api/lobby/create") {
            logger.info("POST /api/lobby/create - Lobby creation requested")
            
            // Check admin authentication
            val apiKey = call.request.header("X-API-Key") 
                ?: call.request.queryParameters["apiKey"]
            
            if (apiKey == null || apiKey != Config.ADMIN_API_KEY) {
                logger.warn("POST /api/lobby/create - UNAUTHORIZED: Invalid or missing API key")
                call.respond(
                    HttpStatusCode.Unauthorized,
                    CreateLobbyResponse(
                        success = false,
                        lobbyId = null,
                        message = "Unauthorized. Admin API key required."
                    )
                )
                return@post
            }
            
            // Generate and create lobby with random 4-digit ID
            val lobbyId = LobbyManager.createLobby()
            
            if (lobbyId != null) {
                logger.info("POST /api/lobby/create - SUCCESS: Lobby created with ID: $lobbyId")
                call.respond(
                    HttpStatusCode.Created,
                    CreateLobbyResponse(
                        success = true,
                        lobbyId = lobbyId,
                        message = "Lobby created successfully."
                    )
                )
            } else {
                logger.error("POST /api/lobby/create - FAILED: Unable to generate unique lobby ID")
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    CreateLobbyResponse(
                        success = false,
                        lobbyId = null,
                        message = "Unable to generate unique lobby ID. All IDs may be in use."
                    )
                )
            }
        }
        
        post("/api/test") {
            val request = call.receive<TestRequest>()
            logger.info("POST /api/test - Received text: ${request.text}")
            call.respond(TestResponse(
                received = request.text,
                message = "Text received successfully"
            ))
        }
        
        webSocket("/ws/lobby/{lobbyId}") {
            val lobbyId = call.parameters["lobbyId"] ?: run {
                logger.warn("WebSocket /ws/lobby/{lobbyId} - REJECTED: Missing lobby ID")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing lobby ID"))
                return@webSocket
            }
            
            logger.info("WebSocket /ws/lobby/$lobbyId - Connection established")
            val handler = WebSocketHandler(lobbyId, this)
            
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val message = WebSocketCodec.decode(frame.readText())
                        if (message != null) {
                            val messageType = message::class.simpleName ?: "Unknown"
                            logger.debug("WebSocket /ws/lobby/$lobbyId - Received message type: $messageType")
                            val shouldContinue = handler.handleMessage(message)
                            if (!shouldContinue) {
                                logger.info("WebSocket /ws/lobby/$lobbyId - Connection closing")
                                break
                            }
                        } else {
                            logger.warn("WebSocket /ws/lobby/$lobbyId - Invalid message format")
                            handler.sendError("Invalid message format")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("WebSocket /ws/lobby/$lobbyId - Error: ${e.message}", e)
            } finally {
                logger.info("WebSocket /ws/lobby/$lobbyId - Connection closed")
                handler.cleanup()
            }
        }
    }
}
