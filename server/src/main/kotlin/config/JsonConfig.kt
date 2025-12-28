package com.tombalator.config

import com.tombalator.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object JsonConfig {
    val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(WebSocketMessage::class) {
                subclass(JoinGameMessage::class)
                subclass(LeaveGameMessage::class)
                subclass(ChatMessage::class)
                subclass(NumberDrawnMessage::class)
                subclass(PlayerJoinedMessage::class)
                subclass(PlayerLeftMessage::class)
                subclass(PlayersUpdateMessage::class)
                subclass(ErrorMessage::class)
            }
        }
    }
}

