package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class WeatherResponse<T>(
    val response: Response<T>
) : Serializable
