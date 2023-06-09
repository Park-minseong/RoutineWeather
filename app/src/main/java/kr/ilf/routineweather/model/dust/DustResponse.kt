package kr.ilf.routineweather.model.dust

import java.io.Serializable

data class DustResponse<T>(
    val response: Response<T>
) : Serializable
