package kr.ilf.routineweather.model

import java.io.Serializable

data class ResponseHeader(
    val resultCode: String,
    val resultMsg: String
): Serializable
