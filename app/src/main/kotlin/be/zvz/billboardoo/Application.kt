package be.zvz.billboardoo

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.plugins.Routing.configureRouting
import be.zvz.billboardoo.plugins.Serialization.configureSerialization
import be.zvz.billboardoo.schedule.RankScheduler
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    Config.init()
    RankScheduler.init()
    Rank.init()
    configureRouting()
    configureSerialization()
}
