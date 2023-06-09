package kr.ilf.routineweather.model.dust

import kr.ilf.routineweather.model.weather.ResponseHeader
import java.io.Serializable

data class Response<T>(
    val body: ResponseBody<T>?
) :Serializable
