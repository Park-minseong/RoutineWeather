package kr.ilf.routineweather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val OPENWEATHER_API_KEY = BuildConfig.OPENWEATHER_API_KEY
    const val OPENAPI_API_KEY = BuildConfig.OPENAPI_API_KEY
    const val X_NCP_APIGW_API_KEY_ID = BuildConfig.X_NCP_APIGW_API_KEY_ID
    const val X_NCP_APIGW_API_KEY = BuildConfig.X_NCP_APIGW_API_KEY
    const val NAVER_API_BASE_URL = "https://naveropenapi.apigw.ntruss.com/"
    const val OPENAPI_BASE_URL = "http://apis.data.go.kr/"
    const val METRIC_UNIT = "metric"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA_ULTRA_FCST = "weather_response_data_ultra_fcst"
    const val WEATHER_RESPONSE_DATA_ULTRA_NCST = "weather_response_data_ultra_ncst"
    const val WEATHER_RESPONSE_DATA_VILAGE_FCST = "weather_response_data_vilage_fcst"
    const val WEATHER_RESPONSE_DATA_DUST = "weather_response_data_dust"
    const val WEATHER_RESPONSE_DATA_MID_TA = "weather_response_data_mid_ta"
    const val WEATHER_RESPONSE_DATA_MID_LAND = "weather_response_data_mid_land"
    const val WEATHER_REQUEST_DATETIME = "weather_request_datetime"
    const val WEATHER_REQUEST_ADDRESS = "weather_request_address"

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

    val weatherCodeOpenApi: Map<String, String> = mapOf(
        "맑음" to "10",
        "구름많음" to "30",
        "구름많고 비" to "31",
        "구름많고 비/눈" to "32",
        "구름많고 눈" to "33",
        "구름많고 소나기" to "34",
        "구름많고 빗방울 떨어짐" to "35",
        "구름많고 빗방울과 눈날림" to "36",
        "구름많고 눈날림" to "37",
        "흐림" to "40",
        "흐리고 비" to "41",
        "흐리고 비/눈" to "42",
        "흐리고 눈" to "43",
        "흐리고 소나기" to "44",
        "45" to "흐리고 빗방울 떨어짐",
        "흐리고 빗방울과 눈날림" to "46",
        "흐리고 눈날림" to "47",
    )


    fun getDrawableIdWeather(weatherCode: String): Int {
        return when (weatherCode) {
            "10" -> R.drawable.sun_max_fill
            "30" -> R.drawable.cloud_sun_fill
            "31" -> R.drawable.cloud_sun_rain_fill
            "32" -> R.drawable.cloud_sun_snow_fill
            "33" -> R.drawable.cloud_sun_snow_fill
            "34" -> R.drawable.cloud_sun_rain_fill
            "35" -> R.drawable.cloud_sun_drizzle_fill
            "36" -> R.drawable.cloud_sun_snow_fill
            "37" -> R.drawable.cloud_sun_snow_fill
            "40" -> R.drawable.cloud_fill
            "41" -> R.drawable.cloud_heavyrain_fill
            "42" -> R.drawable.cloud_sleet_fill
            "43" -> R.drawable.cloud_snow_fill
            "44" -> R.drawable.cloud_rain_fill
            "45" -> R.drawable.cloud_drizzle_fill
            "46" -> R.drawable.cloud_sleet_fill
            "47" -> R.drawable.cloud_snow_fill
            else -> R.drawable.sun_max_fill
        }
    }

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

    val yeongDong = listOf(
        "고성",
        "속초",
        "양양",
        "강릉",
        "동해",
        "삼척",
        "태백"
    )

    val midLandRefIds = mapOf<String, String>(
        "서울" to "11B00000",
        "인천" to "11B00000",
        "경기도" to "11B00000",
        "강원도영서" to "11D10000",
        "강원도영동" to "11D20000",
        "대전" to "11C20000",
        "세종" to "11C20000",
        "충청남도" to "11C20000",
        "충청북도" to "11C10000",
        "광주" to "11F20000",
        "전라남도" to "11F20000",
        "전라북도" to "11F10000",
        "대구" to "11H10000",
        "경상북도" to "11H10000",
        "부산" to "11H20000",
        "울산" to "11H20000",
        "경상남도" to "11H20000",
        "제주도" to "11G00000"
    )

    val midTaRefIds = mapOf<String, String>(
        "백령도" to "11A00101",
        "서울" to "11B10101",
        "과천" to "11B10102",
        "광명" to "11B10103",
        "강화" to "11B20101",
        "김포" to "11B20102",
        "인천" to "11B20201",
        "시흥" to "11B20202",
        "안산" to "11B20203",
        "부천" to "11B20204",
        "의정부" to "11B20301",
        "고양" to "11B20302",
        "양주" to "11B20304",
        "파주" to "11B20305",
        "동두천" to "11B20401",
        "연천" to "11B20402",
        "포천" to "11B20403",
        "가평" to "11B20404",
        "구리" to "11B20501",
        "남양주" to "11B20502",
        "양평" to "11B20503",
        "하남" to "11B20504",
        "수원" to "11B20601",
        "안양" to "11B20602",
        "오산" to "11B20603",
        "화성" to "11B20604",
        "성남" to "11B20605",
        "평택" to "11B20606",
        "의왕" to "11B20609",
        "군포" to "11B20610",
        "안성" to "11B20611",
        "용인" to "11B20612",
        "이천" to "11B20701",
        "광주" to "11B20702",
        "여주" to "11B20703",
        "충주" to "11C10101",
        "진천" to "11C10102",
        "음성" to "11C10103",
        "제천" to "11C10201",
        "단양" to "11C10202",
        "청주" to "11C10301",
        "보은" to "11C10302",
        "괴산" to "11C10303",
        "증평" to "11C10304",
        "추풍령" to "11C10401",
        "영동" to "11C10402",
        "옥천" to "11C10403",
        "서산" to "11C20101",
        "태안" to "11C20102",
        "당진" to "11C20103",
        "홍성" to "11C20104",
        "보령" to "11C20201",
        "서천" to "11C20202",
        "천안" to "11C20301",
        "아산" to "11C20302",
        "예산" to "11C20303",
        "대전" to "11C20401",
        "공주" to "11C20402",
        "계룡" to "11C20403",
        "세종" to "11C20404",
        "부여" to "11C20501",
        "청양" to "11C20502",
        "금산" to "11C20601",
        "논산" to "11C20602",
        "철원" to "11D10101",
        "화천" to "11D10102",
        "인제" to "11D10201",
        "양구" to "11D10202",
        "춘천" to "11D10301",
        "홍천" to "11D10302",
        "원주" to "11D10401",
        "횡성" to "11D10402",
        "영월" to "11D10501",
        "정선" to "11D10502",
        "평창" to "11D10503",
        "대관령" to "11D20201",
        "태백" to "11D20301",
        "속초" to "11D20401",
        "고성" to "11D20402",
        "양양" to "11D20403",
        "강릉" to "11D20501",
        "동해" to "11D20601",
        "삼척" to "11D20602",
        "울릉도" to "11E00102",
        "독도" to "11E00103",
        "전주" to "11F10201",
        "익산" to "11F10202",
        "정읍" to "11F10203",
        "완주" to "11F10204",
        "장수" to "11F10301",
        "무주" to "11F10302",
        "진안" to "11F10303",
        "남원" to "11F10401",
        "임실" to "11F10402",
        "순창" to "11F10403",
        "군산" to "21F10501",
        "김제" to "21F10502",
        "고창" to "21F10601",
        "부안" to "21F10602",
        "함평" to "21F20101",
        "영광" to "21F20102",
        "진도" to "21F20201",
        "완도" to "11F20301",
        "해남" to "11F20302",
        "강진" to "11F20303",
        "장흥" to "11F20304",
        "여수" to "11F20401",
        "광양" to "11F20402",
        "고흥" to "11F20403",
        "보성" to "11F20404",
        "순천시" to "11F20405",
        "광주" to "11F20501",
        "장성" to "11F20502",
        "나주" to "11F20503",
        "담양" to "11F20504",
        "화순" to "11F20505",
        "구례" to "11F20601",
        "곡성" to "11F20602",
        "순천" to "11F20603",
        "흑산도" to "11F20701",
        "목포" to "21F20801",
        "영암" to "21F20802",
        "신안" to "21F20803",
        "무안" to "21F20804",
        "성산" to "11G00101",
        "제주" to "11G00201",
        "성판악" to "11G00302",
        "서귀포" to "11G00401",
        "고산" to "11G00501",
        "이어도" to "11G00601",
        "추자도" to "11G00800",
        "울진" to "11H10101",
        "영덕" to "11H10102",
        "포항" to "11H10201",
        "경주" to "11H10202",
        "문경" to "11H10301",
        "상주" to "11H10302",
        "예천" to "11H10303",
        "영주" to "11H10401",
        "봉화" to "11H10402",
        "영양" to "11H10403",
        "안동" to "11H10501",
        "의성" to "11H10502",
        "청송" to "11H10503",
        "김천" to "11H10601",
        "구미" to "11H10602",
        "군위" to "11H10603",
        "고령" to "11H10604",
        "성주" to "11H10605",
        "대구" to "11H10701",
        "영천" to "11H10702",
        "경산" to "11H10703",
        "청도" to "11H10704",
        "칠곡" to "11H10705",
        "울산" to "11H20101",
        "양산" to "11H20102",
        "부산" to "11H20201",
        "창원" to "11H20301",
        "김해" to "11H20304",
        "통영" to "11H20401",
        "사천" to "11H20402",
        "거제" to "11H20403",
        "고성" to "11H20404",
        "남해" to "11H20405",
        "함양" to "11H20501",
        "거창" to "11H20502",
        "합천" to "11H20503",
        "밀양" to "11H20601",
        "의령" to "11H20602",
        "함안" to "11H20603",
        "창녕" to "11H20604",
        "진주" to "11H20701",
        "산청" to "11H20703",
        "하동" to "11H20704",
        "사리원" to "11I10001",
        "신계" to "11I10002",
        "해주" to "11I20001",
        "개성" to "11I20002",
        "장연(용연)" to "11I20003",
        "신의주" to "11J10001",
        "삭주(수풍)" to "11J10002",
        "구성" to "11J10003",
        "자성(중강)" to "11J10004",
        "강계" to "11J10005",
        "희천" to "11J10006",
        "평양" to "11J20001",
        "진남포(남포)" to "11J20002",
        "안주" to "11J20004",
        "양덕" to "11J20005",
        "청진" to "11K10001",
        "웅기(선봉)" to "11K10002",
        "성진(김책)" to "11K10003",
        "무산(삼지연)" to "11K10004",
        "함흥" to "11K20001",
        "장진" to "11K20002",
        "북청(신포)" to "11K20003",
        "혜산" to "11K20004",
        "풍산" to "11K20005",
        "원산" to "11L10001",
        "고성(장전)" to "11L10002",
        "평강" to "11L10003",
    )
}