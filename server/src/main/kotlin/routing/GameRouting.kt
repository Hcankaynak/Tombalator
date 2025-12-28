package com.tombalator.routing

import com.tombalator.config.Config
import com.tombalator.game.GameManager
import com.tombalator.models.CreateGameResponse
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
    }
}

