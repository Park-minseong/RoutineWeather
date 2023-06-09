package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class ResponseItem(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val fcstDate: String?,
    val fcstTime: String?,
    val fcstValue: String?,
    val obsrValue: String?,
    val nx: Int,
    val ny: Int
): Serializable
