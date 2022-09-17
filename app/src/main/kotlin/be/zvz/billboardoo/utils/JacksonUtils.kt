package be.zvz.billboardoo.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder

object JacksonUtils {
    val blackbirdModule = BlackbirdModule()
    val mapper: ObjectMapper = jacksonMapperBuilder().addModule(blackbirdModule).build()
}
