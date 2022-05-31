package be.zvz.billboardoo.utils

import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.jsonMapper

object JacksonUtils {
    val blackbirdModule = BlackbirdModule()
    val mapper = jsonMapper().registerModule(blackbirdModule)
}
