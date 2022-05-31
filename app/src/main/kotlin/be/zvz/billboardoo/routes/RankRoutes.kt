package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.schedule.RankScheduler
import be.zvz.billboardoo.utils.JacksonUtils
import guru.zoroark.ratelimit.rateLimited
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Duration

fun Route.rankRouting() {
    route("/rank") {
        patch("/refresh") {
            if (call.request.header("Authorization") == Config.settings.secretKey) {
                RankScheduler.apply()
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(
                        JacksonUtils.mapper.createObjectNode().apply {
                            put("result", true)
                            put("message", "Refreshed ranks")
                        }
                    ),
                    ContentType.Application.Json
                )
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        rateLimited(
            limit = 30,
            timeBeforeReset = Duration.ofMinutes(1)
        ) {
            get("/hourly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.hourlyRank), ContentType.Application.Json)
            }
            get("/24hours") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.twentyFourHoursRank), ContentType.Application.Json)
            }
            get("/daily") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.dailyRank), ContentType.Application.Json)
            }
            get("/weekly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.weeklyRank), ContentType.Application.Json)
            }
            get("/monthly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.monthlyRank), ContentType.Application.Json)
            }
            get("/yearly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.yearlyRank), ContentType.Application.Json)
            }
            get("/all-time") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.allTimeRank), ContentType.Application.Json)
            }
            get("/new") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.newRank), ContentType.Application.Json)
            }
        }
    }
}
