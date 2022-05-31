package be.zvz.billbodoo.plugins

import be.zvz.billbodoo.routes.billbodooRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        billbodooRouting()
    }
}
