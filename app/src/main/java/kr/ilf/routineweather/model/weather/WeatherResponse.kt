package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class WeatherResponse(
    val response: Response
) : Serializable
