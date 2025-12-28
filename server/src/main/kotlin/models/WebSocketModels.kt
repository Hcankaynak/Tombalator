package com.tombalator.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class WebSocketMessage

@Serializable
@SerialName("join_game")
data class JoinGameMessage(
    val gameId: String,
    val userId: String,
    val username: String
) : WebSocketMessage()

@Serializable
@SerialName("leave_game")
data class LeaveGameMessage(
    val dummy: String = ""
) : WebSocketMessage()

@Serializable
@SerialName("chat")
data class ChatMessage(
    val userId: String,
    val username: String,
    val message: String,
    val timestamp: Long
) : WebSocketMessage()

@Serializable
@SerialName("number_drawn")
data class NumberDrawnMessage(
    val number: Int,
    val drawnNumbers: List<Int>
) : WebSocketMessage()

@Serializable
@SerialName("player_joined")
data class PlayerJoinedMessage(
    val userId: String,
    val username: String,
    val playerCount: Int
) : WebSocketMessage()

@Serializable
@SerialName("player_left")
data class PlayerLeftMessage(
    val userId: String,
    val username: String,
    val playerCount: Int
) : WebSocketMessage()

@Serializable
@SerialName("players_update")
data class PlayersUpdateMessage(
    val players: List<PlayerInfo>
) : WebSocketMessage()

@Serializable
data class PlayerInfo(
    val userId: String,
    val username: String
)

@Serializable
@SerialName("error")
data class ErrorMessage(
    val message: String
) : WebSocketMessage()

@Serializable
data class CreateGameResponse(
    val success: Boolean,
    val gameId: String?,
    val message: String
)

