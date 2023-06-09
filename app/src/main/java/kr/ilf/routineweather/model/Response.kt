package kr.ilf.routineweather.model

import java.io.Serializable

data class Response(
    val header: ResponseHeader,
    val body: ResponseBody?
) :Serializable
