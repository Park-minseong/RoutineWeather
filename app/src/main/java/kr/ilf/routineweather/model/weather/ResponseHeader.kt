package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class ResponseHeader(
    val resultCode: String,
    val resultMsg: String
): Serializable
