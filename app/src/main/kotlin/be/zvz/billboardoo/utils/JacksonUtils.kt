package be.zvz.billboardoo.utils

import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder

object JacksonUtils {
    val blackbirdModule = BlackbirdModule()
    val mapper = jacksonMapperBuilder().addModule(blackbirdModule).build()
}
