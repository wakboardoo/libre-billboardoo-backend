package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.dto.SummaryRankResponse
import be.zvz.billboardoo.schedule.RankScheduler
import be.zvz.billboardoo.utils.JacksonUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object RankRoutes {
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
            get("/hourly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.hourly), ContentType.Application.Json)
            }
            get("/24hours") {
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(Rank.twentyFourHours),
                    ContentType.Application.Json
                )
            }
            get("/daily") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.daily), ContentType.Application.Json)
            }
            get("/weekly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.weekly), ContentType.Application.Json)
            }
            get("/monthly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.monthly), ContentType.Application.Json)
            }
            get("/yearly") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.yearly), ContentType.Application.Json)
            }
            get("/all-time") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.allTime), ContentType.Application.Json)
            }
            get("/new") {
                call.respondText(JacksonUtils.mapper.writeValueAsString(Rank.new), ContentType.Application.Json)
            }
            get("/summary") {
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(
                        SummaryRankResponse(
                            Rank.hourly.ranking.take(10),
                            Rank.twentyFourHours.ranking.take(10),
                            Rank.daily.ranking.take(10),
                            Rank.weekly.ranking.take(10),
                            Rank.monthly.ranking.take(10),
                            Rank.yearly.ranking.take(10),
                            Rank.allTime.ranking.take(10),
                            Rank.new.ranking.take(10)
                        )
                    ),
                    ContentType.Application.Json
                )
            }
        }
    }
}
