package be.zvz.billboardoo.plugins

import be.zvz.billboardoo.routes.RankRoutes.rankRouting
import be.zvz.billboardoo.routes.SongRoutes.songRouting
import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.routing.*

object Routing {
    fun Application.configureRouting() {
        install(ForwardedHeaders)
        install(XForwardedHeaders)

        routing {
            rankRouting()
            songRouting()
        }
    }
}
