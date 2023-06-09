package kr.ilf.routineweather.model.dust

data class DustItem(
    val pm10Value: String,
    val pm10Grade1h: String,
    val pm10Flag: String,
    val pm25Value: String,
    val pm25Grade1h: String,
    val pm25Flag: String,
    val stationName: String,
    val stationCode: String,
    val dataTime: String

)
