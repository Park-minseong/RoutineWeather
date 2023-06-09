package kr.ilf.routineweather.model

import java.io.Serializable

data class VilageFcst(
    val baseTime: String?,
    val baseDate: String?,
    val fcstTime: String,
    val fcstDate: String,
    val pty: String,
    val tmp: String,
    val sky: String,
    val pop: String
): Serializable