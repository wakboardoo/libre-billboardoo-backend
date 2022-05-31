package be.zvz.billboardoo.utils

import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JacksonUtils {
    val blackbirdModule = BlackbirdModule()
    val mapper = jsonMapper().registerKotlinModule().registerModule(blackbirdModule)
}
