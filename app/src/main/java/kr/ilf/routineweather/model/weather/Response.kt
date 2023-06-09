package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class Response(
    val header: ResponseHeader,
    val body: ResponseBody?
) :Serializable
