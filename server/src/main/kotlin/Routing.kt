package com.tombalator

import com.tombalator.config.Config
import com.tombalator.models.*
import com.tombalator.routing.configureGameRouting
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

fun Application.configureRouting() {
    // Configure game-related routes
    configureGameRouting()
    
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
        
        post("/api/test") {
            val request = call.receive<TestRequest>()
            logger.info("POST /api/test - Received text: ${request.text}")
            call.respond(TestResponse(
                received = request.text,
                message = "Text received successfully"
            ))
        }
        
        webSocket("/ws/game/{gameId}") {
            val gameId = call.parameters["gameId"] ?: run {
                logger.warn("WebSocket /ws/game/{gameId} - REJECTED: Missing game ID")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing game ID"))
                return@webSocket
            }
            
            logger.info("WebSocket /ws/game/$gameId - Connection established")
            val handler = WebSocketHandler(gameId, this)
            
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val message = WebSocketCodec.decode(frame.readText())
                        if (message != null) {
                            val messageType = message::class.simpleName ?: "Unknown"
                            logger.debug("WebSocket /ws/game/$gameId - Received message type: $messageType")
                            val shouldContinue = handler.handleMessage(message)
                            if (!shouldContinue) {
                                logger.info("WebSocket /ws/game/$gameId - Connection closing")
                                break
                            }
                        } else {
                            logger.warn("WebSocket /ws/game/$gameId - Invalid message format")
                            handler.sendError("Invalid message format")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("WebSocket /ws/game/$gameId - Error: ${e.message}", e)
            } finally {
                logger.info("WebSocket /ws/game/$gameId - Connection closed")
                handler.cleanup()
            }
        }
    }
}
