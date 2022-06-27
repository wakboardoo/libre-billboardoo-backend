package be.zvz.billboardoo.plugins

import be.zvz.billboardoo.utils.JacksonUtils
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

object Serialization {
    fun Application.configureSerialization() {
        install(ContentNegotiation) {
            jackson() {
                registerKotlinModule()
                registerModule(JacksonUtils.blackbirdModule)
            }
        }
    }
}
