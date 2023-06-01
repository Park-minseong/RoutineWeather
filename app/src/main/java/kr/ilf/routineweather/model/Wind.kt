package kr.ilf.routineweather.model

import java.io.Serializable

data class Wind(
    val speed: Double,
    val deg: Int
) : Serializable