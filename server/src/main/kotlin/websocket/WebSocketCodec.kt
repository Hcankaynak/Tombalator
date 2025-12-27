package com.tombalator.websocket

import com.tombalator.config.JsonConfig
import com.tombalator.models.WebSocketMessage
import io.ktor.websocket.*

object WebSocketCodec {
    fun encode(message: WebSocketMessage): String {
        return JsonConfig.json.encodeToString(WebSocketMessage.serializer(), message)
    }
    
    fun decode(text: String): WebSocketMessage? {
        return try {
            JsonConfig.json.decodeFromString<WebSocketMessage>(text)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun sendMessage(session: DefaultWebSocketSession, message: WebSocketMessage) {
        try {
            session.send(Frame.Text(encode(message)))
        } catch (e: Exception) {
            throw e
        }
    }
}

