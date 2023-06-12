package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class Response<T>(
    val header: ResponseHeader,
    val body: ResponseBody<T>?
) :Serializable
