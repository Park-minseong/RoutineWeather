package kr.ilf.routineweather.model.geocoding

import java.io.Serializable

data class Reverse(
    val status: Status,
    val results: ArrayList<Result>
) : Serializable {

    data class Status(
        val code: Int,
        val name: String,
        val message: String
    )

    data class Result(
        val name: String,
        val region: Region
    ) : Serializable {

        data class Region(
            val area1: Area,
            val area2: Area,
            val area3: Area,
            val area4: Area
        ) : Serializable {

            data class Area(val name: String) : Serializable
        }
    }
}

