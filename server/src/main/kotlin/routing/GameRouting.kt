package com.tombalator.routing

import com.tombalator.config.Config
import com.tombalator.game.GameManager
import com.tombalator.models.CreateGameResponse
import com.tombalator.websocket.WebSocketCodec
import com.tombalator.websocket.WebSocketHandler
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.tombalator.routing.GameRouting")

fun Application.configureGameRouting() {
    routing {
        route("/api/game") {
            post("/create") {
                logger.info("POST /api/game/create - Game creation requested")
                
                // Check admin authentication
                val apiKey = call.request.header("X-API-Key") 
                    ?: call.request.queryParameters["apiKey"]
                
                if (apiKey == null || apiKey != Config.ADMIN_API_KEY) {
                    logger.warn("POST /api/game/create - UNAUTHORIZED: Invalid or missing API key")
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        CreateGameResponse(
                            success = false,
                            gameId = null,
                            message = "Unauthorized. Admin API key required."
                        )
                    )
                    return@post
                }
                
                // Generate and create game with random 4-digit ID
                val gameId = GameManager.createGame()
                
                if (gameId != null) {
                    logger.info("POST /api/game/create - SUCCESS: Game created with ID: $gameId")
                    call.respond(
                        HttpStatusCode.Created,
                        CreateGameResponse(
                            success = true,
                            gameId = gameId,
                            message = "Game created successfully."
                        )
                    )
                } else {
                    logger.error("POST /api/game/create - FAILED: Unable to generate unique game ID")
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        CreateGameResponse(
                            success = false,
                            gameId = null,
                            message = "Unable to generate unique game ID. All IDs may be in use."
                        )
                    )
                }
            }
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

