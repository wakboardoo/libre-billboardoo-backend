package be.zvz.billboardoo.plugins

import be.zvz.billboardoo.routes.rankRouting
import be.zvz.billboardoo.routes.songRouting
import guru.zoroark.ratelimit.RateLimit
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(RateLimit)

    routing {
        rankRouting()
        songRouting()
    }
}
