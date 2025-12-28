package com.tombalator

import com.tombalator.config.Config
import com.tombalator.routing.configureGameRouting
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
            call.respond(
                TestResponse(
                    received = request.text,
                    message = "Text received successfully"
                )
            )
        }
    }
}
