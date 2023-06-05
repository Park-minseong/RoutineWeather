package kr.ilf.routineweather.model

import java.io.Serializable

data class UltraSrtFcst(
    val baseTime: String,
    val baseDate: String,
    val fcstTime: String,
    val fcstDate: String,
    val pty: String,
    val t1h: String,
    val sky: String,
): Serializable