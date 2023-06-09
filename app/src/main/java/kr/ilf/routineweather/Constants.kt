package kr.ilf.routineweather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val OPENWEATHER_API_KEY = BuildConfig.OPENWEATHER_API_KEY
    const val OPENAPI_API_KEY = BuildConfig.OPENAPI_API_KEY
    const val BASE_URL = "http://apis.data.go.kr/"
    const val METRIC_UNIT = "metric"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA_ULTRA_FCST = "weather_response_data_ultra_fcst"
    const val WEATHER_RESPONSE_DATA_ULTRA_NCST = "weather_response_data_ultra_ncst"
    const val WEATHER_RESPONSE_DATA_VILAGE_FCST = "weather_response_data_vilage_fcst"
    const val WEATHER_RESPONSE_DATA_DUST = "weather_response_data_dust"

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false

            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }

        } else {
            val networkInfo = connectivityManager.activeNetworkInfo

            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

    val weatherDescOpenApi: Map<String, String> = mapOf(
        "10" to "맑음",
        "11" to "맑고 비",
        "12" to "맑고 비/눈",
        "13" to "맑고 눈",
        "14" to "맑고 소나기",
        "15" to "맑고 빗방울 떨어짐",
        "16" to "맑고 빗방울과 눈날림",
        "17" to "맑고 눈날림",
        "30" to "구름많음",
        "31" to "구름많고 비",
        "32" to "구름많고 비/눈",
        "33" to "구름많고 눈",
        "34" to "구름많고 소나기",
        "35" to "구름많고 빗방울 떨어짐",
        "36" to "구름많고 빗방울과 눈날림",
        "37" to "구름많고 눈날림",
        "40" to "흐림",
        "41" to "흐리고 비",
        "42" to "흐리고 비/눈",
        "43" to "흐리고 눈",
        "44" to "흐리고 소나기",
        "45" to "흐리고 빗방울 떨어짐",
        "46" to "흐리고 빗방울과 눈날림",
        "47" to "흐리고 눈날림"
    )

    val weatherDescKo: Map<Int, String> = mapOf(
        201 to "가벼운 비를 동반한 천둥구름",
        200 to "비를 동반한 천둥구름",
        202 to "폭우를 동반한 천둥구름",
        210 to "약한 천둥구름",
        211 to "천둥구름",
        212 to "강한 천둥구름",
        221 to "불규칙적 천둥구름",
        230 to "약한 연무를 동반한 천둥구름",
        231 to "연무를 동반한 천둥구름",
        232 to "강한 안개비를 동반한 천둥구름",
        300 to "가벼운 안개비",
        301 to "안개비",
        302 to "강한 안개비",
        310 to "가벼운 적은비",
        311 to "적은비",
        312 to "강한 적은비",
        313 to "소나기와 안개비",
        314 to "강한 소나기와 안개비",
        321 to "소나기",
        500 to "악한 비",
        501 to "중간 비",
        502 to "강한 비",
        503 to "매우 강한 비",
        504 to "극심한 비",
        511 to "우박",
        520 to "약한 소나기 비",
        521 to "소나기 비",
        522 to "강한 소나기 비",
        531 to "불규칙적 소나기 비",
        600 to "가벼운 눈",
        601 to "눈",
        602 to "강한 눈",
        611 to "진눈깨비",
        612 to "소나기 진눈깨비",
        615 to "약한 비와 눈",
        616 to "비와 눈",
        620 to "약한 소나기 눈",
        621 to "소나기 눈",
        622 to "강한 소나기 눈",
        701 to "박무",
        711 to "연기",
        721 to "연무",
        731 to "모래 먼지",
        741 to "안개",
        751 to "모래",
        761 to "먼지",
        762 to "화산재",
        771 to "돌풍",
        781 to "토네이도",
        800 to "구름 한 점 없는 맑은 하늘",
        801 to "약간의 구름이 낀 하늘",
        802 to "드문드문 구름이 낀 하늘",
        803 to "구름이 거의 없는 하늘",
        804 to "구름으로 뒤덮인 흐린 하늘",
        900 to "토네이도",
        901 to "태풍",
        902 to "허리케인",
        903 to "한랭",
        904 to "고온",
        905 to "바람부는 날씨",
        906 to "우박",
        951 to "바람이 거의 없는 날씨",
        952 to "약한 바람",
        953 to "부드러운 바람",
        954 to "중간 세기 바람",
        955 to "신선한 바람",
        956 to "센 바람",
        957 to "돌풍에 가까운 센 바람",
        958 to "돌풍",
        959 to "심각한 돌풍",
        960 to "폭풍",
        961 to "강한 폭풍",
        962 to "허리케인"
    )
}