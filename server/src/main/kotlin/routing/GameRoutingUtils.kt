package com.tombalator.routing

import com.tombalator.config.Config
import com.tombalator.game.GameManager
import com.tombalator.models.CloseNumberResponse
import com.tombalator.models.CreateGameResponse
import com.tombalator.models.DrawNumberResponse
import com.tombalator.models.GameExistsResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*

object GameRoutingUtils {
    /**
     * Validates and extracts game ID from route parameters
     * Returns the game ID or null if missing
     */
    fun getGameId(call: ApplicationCall): String? {
        return call.parameters["gameId"]
    }

    /**
     * Validates game ID exists, responds with error if missing
     * Returns true if valid, false if error was sent
     */
    suspend fun validateGameId(call: ApplicationCall, gameId: String?): Boolean {
        if (gameId == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                GameExistsResponse(exists = false, gameId = "")
            )
            return false
        }
        return true
    }

    /**
     * Validates admin API key from request
     * Returns the API key if valid, null if invalid/missing
     */
    fun getAdminApiKey(call: ApplicationCall): String? {
        return call.request.headers["X-API-Key"] 
            ?: call.request.queryParameters["apiKey"]
    }

    /**
     * Validates admin authentication
     * Responds with error if unauthorized, returns true if authorized
     */
    suspend fun validateAdminAuth(
        call: ApplicationCall,
        gameId: String? = null
    ): Boolean {
        val apiKey = getAdminApiKey(call)
        
        if (apiKey == null || apiKey != Config.ADMIN_API_KEY) {
            val errorMessage = if (gameId != null) {
                "Unauthorized. Admin API key required."
            } else {
                "Unauthorized. Admin API key required."
            }
            
            // Determine which response type to use based on context
            // For now, we'll use a generic approach - caller should specify response type
            return false
        }
        
        return true
    }

    /**
     * Validates admin authentication and responds with CreateGameResponse on failure
     */
    suspend fun validateAdminAuthForCreate(call: ApplicationCall): Boolean {
        if (!validateAdminAuth(call)) {
            call.respond(
                HttpStatusCode.Unauthorized,
                CreateGameResponse(
                    success = false,
                    gameId = null,
                    message = "Unauthorized. Admin API key required."
                )
            )
            return false
        }
        return true
    }

    /**
     * Validates admin authentication and responds with DrawNumberResponse on failure
     */
    suspend fun validateAdminAuthForDraw(call: ApplicationCall, gameId: String): Boolean {
        if (!validateAdminAuth(call, gameId)) {
            call.respond(
                HttpStatusCode.Unauthorized,
                DrawNumberResponse(
                    success = false,
                    number = null,
                    drawnNumbers = emptyList(),
                    message = "Unauthorized. Admin API key required."
                )
            )
            return false
        }
        return true
    }

    /**
     * Validates game exists
     * Responds with error if game doesn't exist, returns true if exists
     */
    suspend fun validateGameExists(
        call: ApplicationCall,
        gameId: String,
        responseType: ResponseType = ResponseType.GENERIC
    ): Boolean {
        if (!GameManager.gameExists(gameId)) {
            when (responseType) {
                ResponseType.DRAW_NUMBER -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        DrawNumberResponse(
                            success = false,
                            number = null,
                            drawnNumbers = emptyList(),
                            message = "Game with ID '$gameId' does not exist."
                        )
                    )
                }
                ResponseType.CLOSE_NUMBER -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        CloseNumberResponse(
                            success = false,
                            canClose = false,
                            message = "Game with ID '$gameId' does not exist."
                        )
                    )
                }
                ResponseType.GENERIC -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        GameExistsResponse(exists = false, gameId = gameId)
                    )
                }
            }
            return false
        }
        return true
    }

    /**
     * Validates number is in valid range (1-90)
     * Returns true if valid, false if invalid
     */
    fun validateNumberRange(number: Int): Boolean {
        return number in 1..90
    }

    /**
     * Validates number range and responds with error if invalid
     */
    suspend fun validateNumberRangeWithResponse(
        call: ApplicationCall,
        gameId: String,
        number: Int
    ): Boolean {
        if (!validateNumberRange(number)) {
            call.respond(
                HttpStatusCode.BadRequest,
                CloseNumberResponse(
                    success = false,
                    canClose = false,
                    message = "Number must be between 1 and 90."
                )
            )
            return false
        }
        return true
    }

    enum class ResponseType {
        DRAW_NUMBER,
        CLOSE_NUMBER,
        GENERIC
    }
}

