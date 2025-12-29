package com.tombalator.routing

import com.tombalator.config.Config
import com.tombalator.game.GameManager
import com.tombalator.game.CardGenerator
import com.tombalator.models.CreateGameResponse
import com.tombalator.models.GameExistsResponse
import com.tombalator.models.DrawNumberResponse
import com.tombalator.models.NumberDrawnMessage
import com.tombalator.models.CloseNumberRequest
import com.tombalator.models.CloseNumberResponse
import com.tombalator.models.CardOptionsResponse
import com.tombalator.models.TombalaCardData
import com.tombalator.models.ChatMessage
import com.tombalator.models.ClosedNumbersResponse
import com.tombalator.routing.GameRoutingUtils.ResponseType
import com.tombalator.websocket.WebSocketCodec
import com.tombalator.websocket.WebSocketHandler
import com.tombalator.websocket.WebSocketManager
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
            get("/{gameId}") {
                val gameId = GameRoutingUtils.getGameId(call) ?: run {
                    logger.warn("GET /api/game/{gameId} - Missing game ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GameExistsResponse(exists = false, gameId = "")
                    )
                    return@get
                }
                
                logger.info("GET /api/game/$gameId - Checking if game exists")
                val exists = GameManager.gameExists(gameId)
                
                if (exists) {
                    logger.info("GET /api/game/$gameId - Game exists")
                } else {
                    logger.info("GET /api/game/$gameId - Game does not exist")
                }
                
                call.respond(GameExistsResponse(exists = exists, gameId = gameId))
            }
            
            get("/{gameId}/card-options") {
                val gameId = GameRoutingUtils.getGameId(call) ?: run {
                    logger.warn("GET /api/game/{gameId}/card-options - Missing game ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        CardOptionsResponse(
                            success = false,
                            cards = emptyList(),
                            message = "Missing game ID"
                        )
                    )
                    return@get
                }
                
                logger.info("GET /api/game/$gameId/card-options - Card options requested")
                
                // Check if game exists
                if (!GameRoutingUtils.validateGameExists(call, gameId, ResponseType.GENERIC)) {
                    logger.warn("GET /api/game/$gameId/card-options - Game does not exist")
                    return@get
                }
                
                // Generate 3 random cards
                val cards = CardGenerator.generateCards(3)
                
                // Convert to TombalaCardData for response
                val cardData = cards.map { card ->
                    TombalaCardData(
                        id = card.id,
                        rows = card.rows,
                        theme = card.theme
                    )
                }
                
                logger.info("GET /api/game/$gameId/card-options - SUCCESS: Generated ${cards.size} cards")
                
                call.respond(
                    HttpStatusCode.OK,
                    CardOptionsResponse(
                        success = true,
                        cards = cardData,
                        message = "Cards generated successfully."
                    )
                )
            }
            
            get("/{gameId}/closed-numbers") {
                val gameId = GameRoutingUtils.getGameId(call) ?: run {
                    logger.warn("GET /api/game/{gameId}/closed-numbers - Missing game ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ClosedNumbersResponse(
                            success = false,
                            closedNumbers = emptyList(),
                            message = "Missing game ID"
                        )
                    )
                    return@get
                }
                
                val userId = call.request.queryParameters["userId"]
                if (userId.isNullOrBlank()) {
                    logger.warn("GET /api/game/$gameId/closed-numbers - Missing userId")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ClosedNumbersResponse(
                            success = false,
                            closedNumbers = emptyList(),
                            message = "User ID is required."
                        )
                    )
                    return@get
                }
                
                logger.info("GET /api/game/$gameId/closed-numbers - Closed numbers requested for user $userId")
                
                // Check if game exists
                if (!GameRoutingUtils.validateGameExists(call, gameId, ResponseType.GENERIC)) {
                    logger.warn("GET /api/game/$gameId/closed-numbers - Game does not exist")
                    return@get
                }
                
                // Get closed numbers for user
                val closedNumbers = GameManager.getClosedNumbersForUser(gameId, userId)
                
                logger.info("GET /api/game/$gameId/closed-numbers - SUCCESS: Found ${closedNumbers.size} closed numbers for user $userId")
                
                call.respond(
                    HttpStatusCode.OK,
                    ClosedNumbersResponse(
                        success = true,
                        closedNumbers = closedNumbers.toList(),
                        message = "Closed numbers retrieved successfully."
                    )
                )
            }
            
            post("/create") {
                logger.info("POST /api/game/create - Game creation requested")
                
                // Check admin authentication
                if (!GameRoutingUtils.validateAdminAuthForCreate(call)) {
                    logger.warn("POST /api/game/create - UNAUTHORIZED: Invalid or missing API key")
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
            
            post("/{gameId}/draw-number") {
                val gameId = GameRoutingUtils.getGameId(call) ?: run {
                    logger.warn("POST /api/game/{gameId}/draw-number - Missing game ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        DrawNumberResponse(
                            success = false,
                            number = null,
                            drawnNumbers = emptyList(),
                            message = "Missing game ID"
                        )
                    )
                    return@post
                }
                
                logger.info("POST /api/game/$gameId/draw-number - Draw number requested")
                
                // Check admin authentication
                if (!GameRoutingUtils.validateAdminAuthForDraw(call, gameId)) {
                    logger.warn("POST /api/game/$gameId/draw-number - UNAUTHORIZED: Invalid or missing API key")
                    return@post
                }
                
                // Check if game exists
                if (!GameRoutingUtils.validateGameExists(call, gameId, ResponseType.DRAW_NUMBER)) {
                    logger.warn("POST /api/game/$gameId/draw-number - Game does not exist")
                    return@post
                }
                
                // Draw a random number (1-90)
                val drawnNumber = GameManager.drawNumber(gameId)
                
                if (drawnNumber != null) {
                    val allDrawnNumbers = GameManager.getDrawnNumbers(gameId)
                    
                    logger.info("POST /api/game/$gameId/draw-number - SUCCESS: Number $drawnNumber drawn. Total drawn: ${allDrawnNumbers.size}")
                    
                    // Broadcast the drawn number to all players in the game via WebSocket
                    val numberDrawnMessage = NumberDrawnMessage(
                        number = drawnNumber,
                        drawnNumbers = allDrawnNumbers
                    )
                    WebSocketManager.broadcastToGame(
                        gameId,
                        WebSocketCodec.encode(numberDrawnMessage)
                    )
                    
                    call.respond(
                        HttpStatusCode.OK,
                        DrawNumberResponse(
                            success = true,
                            number = drawnNumber,
                            drawnNumbers = allDrawnNumbers,
                            message = "Number drawn successfully."
                        )
                    )
                } else {
                    logger.warn("POST /api/game/$gameId/draw-number - FAILED: All numbers (1-90) have been drawn")
                    val allDrawnNumbers = GameManager.getDrawnNumbers(gameId)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        DrawNumberResponse(
                            success = false,
                            number = null,
                            drawnNumbers = allDrawnNumbers,
                            message = "All numbers (1-90) have already been drawn."
                        )
                    )
                }
            }
            
            post("/{gameId}/close-number") {
                val gameId = GameRoutingUtils.getGameId(call) ?: run {
                    logger.warn("POST /api/game/{gameId}/close-number - Missing game ID")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        CloseNumberResponse(
                            success = false,
                            canClose = false,
                            message = "Missing game ID"
                        )
                    )
                    return@post
                }
                
                logger.info("POST /api/game/$gameId/close-number - Close number requested")
                
                // Check if game exists
                if (!GameRoutingUtils.validateGameExists(call, gameId, ResponseType.CLOSE_NUMBER)) {
                    logger.warn("POST /api/game/$gameId/close-number - Game does not exist")
                    return@post
                }
                
                try {
                    val request = call.receive<CloseNumberRequest>()
                    
                    // Validate userId is provided
                    if (request.userId.isBlank()) {
                        logger.warn("POST /api/game/$gameId/close-number - Missing userId")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            CloseNumberResponse(
                                success = false,
                                canClose = false,
                                message = "User ID is required."
                            )
                        )
                        return@post
                    }
                    
                    // Validate number range (1-90)
                    if (!GameRoutingUtils.validateNumberRangeWithResponse(call, gameId, request.number)) {
                        logger.warn("POST /api/game/$gameId/close-number - Invalid number: ${request.number}")
                        return@post
                    }
                    
                    // Get user's card
                    val userCard = GameManager.getUserCard(gameId, request.userId)
                    if (userCard == null) {
                        logger.warn("POST /api/game/$gameId/close-number - User ${request.userId} does not have a card")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            CloseNumberResponse(
                                success = false,
                                canClose = false,
                                message = "You must select a card before closing numbers."
                            )
                        )
                        return@post
                    }
                    
                    // Check if the number exists on the user's card
                    val numberExistsOnCard = userCard.rows.any { row ->
                        row.any { cell -> cell == request.number }
                    }
                    
                    if (!numberExistsOnCard) {
                        logger.info("POST /api/game/$gameId/close-number - FAILED: Number ${request.number} does not exist on user's card")
                        call.respond(
                            HttpStatusCode.OK,
                            CloseNumberResponse(
                                success = true,
                                canClose = false,
                                message = "Number cannot be closed. It does not exist on your card."
                            )
                        )
                        return@post
                    }
                    
                    // Check if the number has been drawn
                    val drawnNumbers = GameManager.getDrawnNumbers(gameId)
                    val canClose = drawnNumbers.contains(request.number)
                    
                    if (canClose) {
                        // Check if number is already closed for this user
                        if (GameManager.isNumberClosedForUser(gameId, request.userId, request.number)) {
                            logger.info("POST /api/game/$gameId/close-number - Number ${request.number} already closed for user ${request.userId}")
                            call.respond(
                                HttpStatusCode.OK,
                                CloseNumberResponse(
                                    success = true,
                                    canClose = false,
                                    message = "Number is already closed."
                                )
                            )
                            return@post
                        }
                        
                        // Mark number as closed for this user
                        GameManager.closeNumberForUser(gameId, request.userId, request.number)
                        
                        logger.info("POST /api/game/$gameId/close-number - SUCCESS: Number ${request.number} closed for user ${request.userId}")
                        
                        // Get username for the player who closed the number
                        val username = WebSocketManager.getUsername(gameId, request.userId) ?: "Unknown"
                        
                        // Broadcast system message about closing the number
                        val closeMessage = ChatMessage(
                            userId = "SYSTEM",
                            username = "SYSTEM",
                            message = "$username closed number ${request.number}",
                            timestamp = System.currentTimeMillis()
                        )
                        WebSocketManager.broadcastToGame(
                            gameId,
                            WebSocketCodec.encode(closeMessage)
                        )
                        
                        // Check if user completed a row (çinko)
                        val completedRowIndex = GameManager.checkCompletedRow(gameId, request.userId)
                        if (completedRowIndex != null) {
                            logger.info("POST /api/game/$gameId/close-number - User $username completed a çinko (row ${completedRowIndex + 1})")
                            
                            // Broadcast çinko message
                            val cinkoMessage = ChatMessage(
                                userId = "SYSTEM",
                                username = "SYSTEM",
                                message = "$username completed a çinko",
                                timestamp = System.currentTimeMillis()
                            )
                            WebSocketManager.broadcastToGame(
                                gameId,
                                WebSocketCodec.encode(cinkoMessage)
                            )
                        }
                        
                        call.respond(
                            HttpStatusCode.OK,
                            CloseNumberResponse(
                                success = true,
                                canClose = true,
                                message = "Number can be closed."
                            )
                        )
                    } else {
                        logger.info("POST /api/game/$gameId/close-number - FAILED: Number ${request.number} cannot be closed (not drawn yet)")
                        call.respond(
                            HttpStatusCode.OK,
                            CloseNumberResponse(
                                success = true,
                                canClose = false,
                                message = "Number cannot be closed. It has not been drawn yet."
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("POST /api/game/$gameId/close-number - Error parsing request: ${e.message}", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        CloseNumberResponse(
                            success = false,
                            canClose = false,
                            message = "Invalid request format."
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

