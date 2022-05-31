package be.zvz.billboardoo

import be.zvz.billboardoo.plugins.configureRouting
import be.zvz.billboardoo.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureRouting()
    configureSerialization()
}
