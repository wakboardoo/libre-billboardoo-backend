package be.zvz.billbodoo

import be.zvz.billbodoo.plugins.configureRouting
import be.zvz.billbodoo.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureRouting()
    configureSerialization()
}
