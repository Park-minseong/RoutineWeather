package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class MidTa(
    val date: Int,
    val taMin: Int,
    val taMinLow: Int,
    val taMinHigh: Int,
    val taMax: Int,
    val taMaxLow: Int,
    val taMaxHigh: Int,

    // 강수 확률
    val rnStAm: Int,
    val rnStPm: Int,

    // 날씨
    val wfAm: String,
    val wfPm: String
) : Serializable
