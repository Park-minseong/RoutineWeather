package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class UltraSrtNcst(
    val baseTime: String,
    val baseDate: String,
    val pty: String,
    val t1h: String,
    val rn1: String,
    val reh: String,
): Serializable
