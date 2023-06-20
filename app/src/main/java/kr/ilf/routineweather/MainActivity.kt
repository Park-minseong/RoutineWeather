package kr.ilf.routineweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kr.hyosang.coordinate.CoordPoint
import kr.hyosang.coordinate.TransCoord
import kr.ilf.routineweather.databinding.ActivityMainBinding
import kr.ilf.routineweather.model.dust.DustItem
import kr.ilf.routineweather.model.dust.DustResponse
import kr.ilf.routineweather.model.dust.StationItem
import kr.ilf.routineweather.model.geocoding.Reverse
import kr.ilf.routineweather.model.weather.MidLandItem
import kr.ilf.routineweather.model.weather.MidTa
import kr.ilf.routineweather.model.weather.MidTaItem
import kr.ilf.routineweather.model.weather.SrtItem
import kr.ilf.routineweather.model.weather.UltraSrtFcst
import kr.ilf.routineweather.model.weather.UltraSrtNcst
import kr.ilf.routineweather.model.weather.VilageFcst
import kr.ilf.routineweather.model.weather.WeatherResponse
import kr.ilf.routineweather.network.WeatherService
import kr.ilf.routineweather.utils.TransLocalPoint
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PATH_GET_ULTRA_SRT_NCST = "getUltraSrtNcst"
        private const val PATH_GET_ULTRA_SRT_FCST = "getUltraSrtFcst"
        private const val PATH_GET_VILAGE_FCST = "getVilageFcst"
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var weatherService: WeatherService
    private var isCompletedCallUltraSrtNcst = false

    private var isCompletedCallUltraSrtFcst = false
    private var isCompletedCallVilageFcst = false
    private var isCompletedCallMidTa = false
    private var isCompletedCallMidLand = false
    private var updatedSrtUI = false
    private var updatedDustUI = false
    private var updatedMidUI = false
    private var isFirst = true
    private var ultraSrtFcstBaseTime = "0030"
    private var vilageFcstBaseTime = "0000"
    private var currentDate = "000000"
    private var mAddress = ""

    private var binding: ActivityMainBinding? = null

    private var ultraSrtNcstBaseTime = "0000"

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding?.refresh?.setOnRefreshListener {
            showCustomProgressDialog()
            checkPermissionsAndRequestData()
            binding?.refresh?.isRefreshing = false
        }

        binding?.rvSrtFcst?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                binding?.refresh?.isEnabled = newState == RecyclerView.SCROLL_STATE_IDLE
            }
        })

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        isCompletedCallUltraSrtNcst = true
        isCompletedCallUltraSrtFcst = true
        isCompletedCallVilageFcst = true
        isCompletedCallMidTa = true
        isCompletedCallMidLand = true

        setupSrtUI()
        setupMidUI()
        setupDustUI()

        isFirst = false

        val updatedDateTime = mSharedPreferences.getString(Constants.WEATHER_REQUEST_DATETIME, null)
        val updatedAddress = mSharedPreferences.getString(Constants.WEATHER_REQUEST_ADDRESS, null)

        if (updatedDateTime.isNullOrEmpty() || updatedAddress.isNullOrEmpty()) {
            checkPermissionsAndRequestData()

            return
        }

        val updatedDatetimeInt =
            LocalDateTime.parse(updatedDateTime, DateTimeFormatter.ofPattern("yy.MM.dd HH:mm"))
                .format(DateTimeFormatter.ofPattern("yyMMddHH")).toInt()

        val currentDatetimeInt =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHH")).toInt()

        if (currentDatetimeInt - updatedDatetimeInt >= 2) {
            checkPermissionsAndRequestData()

            return
        }

        binding?.tvUpdatedTime?.text = "최근 갱신: $updatedDateTime"
        binding?.tvUpdatedAddress?.text = updatedAddress
    }

    private fun checkPermissionsAndRequestData() {
        if (!isLocationEnabled()) {
            Toast.makeText(
                this@MainActivity,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

            startActivity(intent)
        } else {
            Dexter.withContext(this@MainActivity).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        showCustomProgressDialog()

                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity,
                            "You have denied location permission. Please enable them as it is mandatory for the app to work.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?, token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                checkPermissionsAndRequestData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        showCustomProgressDialog()

        val mLocationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0).setMaxUpdates(1).build()

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.getMainLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation!!

            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude

            Log.d("좌표: ", "$latitude,$longitude")

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            // HTTP요청 Logging용 클라이언트 선언
            val interceptor = HttpLoggingInterceptor()

            if (BuildConfig.DEBUG) {
                interceptor.level = HttpLoggingInterceptor.Level.BODY
            } else {
                interceptor.level = HttpLoggingInterceptor.Level.NONE
            }

            val okHttpClient =
                OkHttpClient().newBuilder().addNetworkInterceptor(interceptor).build()
            // HTTP요청 Logging용 클라이언트 선언 끝

            // 기상청 격자 좌표로 변환
            val transLocalPoint = TransLocalPoint()
            val gridGps =
                transLocalPoint.convertGRID_GPS(TransLocalPoint.TO_GRID, latitude, longitude)
            val nx = gridGps.x.toInt()
            val ny = gridGps.y.toInt()
            // 기상청 격자 좌표로 변환 끝

            // tm 좌표로 변환mAddress
            val pt = CoordPoint(longitude, latitude)
            val transCoord =
                TransCoord.getTransCoord(pt, TransCoord.COORD_TYPE_WGS84, TransCoord.COORD_TYPE_TM)

            val tmX = transCoord.x
            val tmY = transCoord.y
            // tm 좌표로 변환 끝

            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.OPENAPI_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build()

            weatherService = retrofit.create(WeatherService::class.java)

            ultraSrtNcstBaseTime = getBaseTime("ultraSrtNcst")
            ultraSrtFcstBaseTime = getBaseTime("ultraSrtFcst")
            vilageFcstBaseTime = getBaseTime("vilageFcst")

            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            if (vilageFcstBaseTime == "2400")
                currentDate =
                    LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            // 초단기 실황 Call
            val ultraSrtNcstCall: Call<WeatherResponse<SrtItem>> = weatherService.getOpenApiWeather(
                PATH_GET_ULTRA_SRT_NCST,
                Constants.OPENAPI_API_KEY,
                1,
                10,
                "JSON",
                currentDate,
                ultraSrtNcstBaseTime,
                nx,
                ny
            )

            // 초단기 예보 Call
            val ultraSrtFcstCall: Call<WeatherResponse<SrtItem>> = weatherService.getOpenApiWeather(
                PATH_GET_ULTRA_SRT_FCST,
                Constants.OPENAPI_API_KEY,
                1,
                60,
                "JSON",
                currentDate,
                ultraSrtFcstBaseTime,
                nx,
                ny
            )

            // 단기 예보 Call
            val vilageFcstCall: Call<WeatherResponse<SrtItem>> = weatherService.getOpenApiWeather(
                PATH_GET_VILAGE_FCST,
                Constants.OPENAPI_API_KEY,
                1,
                1000,
                "JSON",
                currentDate,
                vilageFcstBaseTime,
                nx,
                ny
            )

            // 근접 측정소 Call
            val nearStationCall: Call<DustResponse<StationItem>> =
                weatherService.getNearbyMsrstnList(Constants.OPENAPI_API_KEY, "json", tmX, tmY)

            isCompletedCallUltraSrtNcst = false
            isCompletedCallUltraSrtFcst = false
            isCompletedCallVilageFcst = false

            enqueueUltraSrtNcstCall(ultraSrtNcstCall)
            enqueueUltraSrtFcstCall(ultraSrtFcstCall)
            enqueueVilageNcstCall(vilageFcstCall)
            enqueueNearStationCall(nearStationCall)

            val naverRetrofit = Retrofit.Builder()
                .baseUrl(Constants.NAVER_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()

            val geocodingService = naverRetrofit.create(WeatherService::class.java)

            val reverseGeocodingCall: Call<Reverse> =
                geocodingService.reverseGeocoding("$longitude,$latitude")

            enqueueReverseGeocodingCall(reverseGeocodingCall)
        }
    }

    private fun getMidTaItemCall(
        adminArea: String?, locality: String?
    ): Call<WeatherResponse<MidTaItem>> {
        var midTaRefId: String? = null

        Constants.midTaRefIds.forEach { (key, value) ->
            if (adminArea!!.contains(key)) {
                midTaRefId = value
            }
        }

        if (midTaRefId == null) Constants.midTaRefIds.forEach { (key, value) ->
            if (locality!!.contains(key)) {
                midTaRefId = value
            }
        }

        val tmFc = getRequestDateTime()

        // 중기 기온조회 Call
        return weatherService.getMidTa(regId = midTaRefId!!, tmFc = tmFc)
    }

    private fun getRequestDateTime(): String {
        return when (LocalDateTime.now().hour) {
            in 0..5 -> LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "1800"

            in 18..24 -> currentDate + "1800"
            else -> currentDate + "0600"
        }
    }

    private fun getMidLandItemCall(
        adminArea: String?, locality: String?
    ): Call<WeatherResponse<MidLandItem>> {
        var midLandRefId: String? = null

        if (adminArea!!.contains("강원")) {
            run {
                Constants.yeongDong.forEach {
                    if (locality!!.contains(it)) {
                        midLandRefId = Constants.midLandRefIds["강원도영동"]

                        return@run
                    } else {
                        midLandRefId = Constants.midLandRefIds["강원도영서"]
                    }
                }
            }
        } else {

            Constants.midLandRefIds.forEach { (key, value) ->
                if (adminArea.contains(key)) {
                    midLandRefId = value
                }
            }

            if (midLandRefId == null) Constants.midLandRefIds.forEach { (key, value) ->
                if (locality!!.contains(key)) {
                    midLandRefId = value
                }
            }
        }

        val tmFc = getRequestDateTime()

        // 중기 육상예보조회 Call
        return weatherService.getMidLandFcst(regId = midLandRefId!!, tmFc = tmFc)
    }

    private fun enqueueReverseGeocodingCall(reverseGeocodingCall: Call<Reverse>) {
        var requestCount = 0

        reverseGeocodingCall.enqueue(object : Callback<Reverse> {
            override fun onResponse(call: Call<Reverse>, response: Response<Reverse>) {
                if (response.isSuccessful) {
                    val region = response.body()?.results?.get(0)?.region

                    mAddress =
                        "${region?.area1?.name} ${region?.area2?.name ?: ""} ${region?.area3?.name ?: ""} ${region?.area4?.name ?: ""}".replace(
                            "  ",
                            " "
                        )

                    val adminArea = region?.area1?.name
                    val locality = region?.area2?.name

                    val midTaItemCall: Call<WeatherResponse<MidTaItem>> =
                        getMidTaItemCall(adminArea, locality)

                    val midLandItemCall: Call<WeatherResponse<MidLandItem>> =
                        getMidLandItemCall(adminArea, locality)

                    enqueueMidTaCall(midTaItemCall)
                    enqueueMidLandCall(midLandItemCall)
                }
            }

            override fun onFailure(call: Call<Reverse>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedMidUI = true

                    hideProgressDialog()
                    showRequestFailedDialog("주소정보 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "reverseGeocodingCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    reverseGeocodingCall.cancel()
                    reverseGeocodingCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun enqueueMidTaCall(midTaItemCall: Call<WeatherResponse<MidTaItem>>) {
        var requestCount = 0

        midTaItemCall.enqueue((object : Callback<WeatherResponse<MidTaItem>> {
            override fun onResponse(
                call: Call<WeatherResponse<MidTaItem>>,
                response: Response<WeatherResponse<MidTaItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items?.item!![0]

                    val taItemJsonString = Gson().toJson(responseData)

                    mSharedPreferences.edit()
                        .putString(Constants.WEATHER_RESPONSE_DATA_MID_TA, taItemJsonString).apply()

                    isCompletedCallMidTa = true

                    setupMidUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse<MidTaItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedMidUI = true

                    hideProgressDialog()
                    showRequestFailedDialog("중기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e("midTaCall Request Errorrrrr count $requestCount.", t.message.toString())
                    midTaItemCall.cancel()
                    midTaItemCall.clone().enqueue(this)
                }
            }
        }))
    }

    private fun enqueueMidLandCall(midLandItemCall: Call<WeatherResponse<MidLandItem>>) {
        var requestCount = 0

        midLandItemCall.enqueue((object : Callback<WeatherResponse<MidLandItem>> {
            override fun onResponse(
                call: Call<WeatherResponse<MidLandItem>>,
                response: Response<WeatherResponse<MidLandItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items?.item!![0]

                    val landItemJsonString = Gson().toJson(responseData)

                    mSharedPreferences.edit()
                        .putString(Constants.WEATHER_RESPONSE_DATA_MID_LAND, landItemJsonString)
                        .apply()

                    isCompletedCallMidLand = true

                    setupMidUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse<MidLandItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedMidUI = true

                    hideProgressDialog()
                    showRequestFailedDialog("중기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "midLandItemCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    midLandItemCall.cancel()
                    midLandItemCall.clone().enqueue(this)
                }
            }
        }))
    }

    private fun enqueueNearStationCall(nearStationCall: Call<DustResponse<StationItem>>) {
        var requestCount = 0

        nearStationCall.enqueue(object : Callback<DustResponse<StationItem>> {
            override fun onResponse(
                call: Call<DustResponse<StationItem>>, response: Response<DustResponse<StationItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items!!

                    val dustDataCall = weatherService.getMsrstnAcctoRltmMesureDnsty(
                        Constants.OPENAPI_API_KEY, "json", 1, 24, responseData[0].stationName
                    )

                    enqueueDustData(dustDataCall)
                }
            }

            override fun onFailure(call: Call<DustResponse<StationItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedDustUI = true

                    hideProgressDialog()
                    showRequestFailedDialog("중기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {

                    Log.e(
                        "nearStationCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    nearStationCall.cancel()
                    nearStationCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun enqueueDustData(dustDataCall: Call<DustResponse<DustItem>>) {
        var requestCount = 0

        dustDataCall.enqueue(object : Callback<DustResponse<DustItem>> {
            override fun onResponse(
                call: Call<DustResponse<DustItem>>, response: Response<DustResponse<DustItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items!!

                    val dustItemsJsonString = Gson().toJson(responseData)

                    val editor = mSharedPreferences.edit()
                    editor.putString(
                        Constants.WEATHER_RESPONSE_DATA_DUST, dustItemsJsonString
                    )
                    editor.apply()

                    setupDustUI()
                }
            }

            override fun onFailure(call: Call<DustResponse<DustItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedDustUI = true

                    hideProgressDialog()

                    showRequestFailedDialog("미세먼지데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "dustDataCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    dustDataCall.cancel()
                    dustDataCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun enqueueVilageNcstCall(vilageFcstCall: Call<WeatherResponse<SrtItem>>) {
        var requestCount = 0

        vilageFcstCall.enqueue(object : Callback<WeatherResponse<SrtItem>> {
            override fun onResponse(
                call: Call<WeatherResponse<SrtItem>>, response: Response<WeatherResponse<SrtItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items?.item
                    val VilageFcsts = LinkedHashMap<String, HashMap<String, String>>()

                    responseData?.forEach {
                        val key = it.fcstDate + it.fcstTime

                        if (!VilageFcsts.containsKey(key)) {
                            VilageFcsts[key] = HashMap()
                            VilageFcsts[key]?.put("fcstDate", it.fcstDate!!)
                            VilageFcsts[key]?.put("fcstTime", it.fcstTime!!)
                        }

                        VilageFcsts[key]?.put(it.category, it.fcstValue!!)
                    }

                    val vilageFcstJSonString = Gson().toJson(VilageFcsts)

                    val editor = mSharedPreferences.edit()
                    editor.putString(
                        Constants.WEATHER_RESPONSE_DATA_VILAGE_FCST, vilageFcstJSonString
                    )
                    editor.apply()

                    isCompletedCallVilageFcst = true

                    setupSrtUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse<SrtItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedSrtUI = true

                    hideProgressDialog()

                    showRequestFailedDialog("단기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "vilageFcstCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    vilageFcstCall.cancel()
                    vilageFcstCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun enqueueUltraSrtFcstCall(ultraSrtFcstCall: Call<WeatherResponse<SrtItem>>) {
        var requestCount = 0

        ultraSrtFcstCall.enqueue(object : Callback<WeatherResponse<SrtItem>> {
            override fun onResponse(
                call: Call<WeatherResponse<SrtItem>>, response: Response<WeatherResponse<SrtItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items?.item
                    val ultraSrtFcst = LinkedHashMap<String, HashMap<String, String>>()

                    responseData?.forEach {
                        if (!ultraSrtFcst.containsKey(it.fcstTime)) {
                            ultraSrtFcst[it.fcstTime!!] = HashMap()
                            ultraSrtFcst[it.fcstTime]?.put("fcstDate", it.fcstDate!!)
                            ultraSrtFcst[it.fcstTime]?.put("fcstTime", it.fcstTime)
                        }

                        ultraSrtFcst[it.fcstTime]?.put(it.category, it.fcstValue!!)
                    }

                    val ultraSrtFcstJSonString = Gson().toJson(ultraSrtFcst)

                    val editor = mSharedPreferences.edit()
                    editor.putString(
                        Constants.WEATHER_RESPONSE_DATA_ULTRA_FCST, ultraSrtFcstJSonString
                    )
                    editor.apply()

                    isCompletedCallUltraSrtFcst = true

                    setupSrtUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse<SrtItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedSrtUI = true

                    hideProgressDialog()

                    showRequestFailedDialog("단기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "ultraSrtFcstCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    ultraSrtFcstCall.cancel()
                    ultraSrtFcstCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun enqueueUltraSrtNcstCall(ultraSrtNcstCall: Call<WeatherResponse<SrtItem>>) {
        var requestCount = 0

        ultraSrtNcstCall.enqueue(object : Callback<WeatherResponse<SrtItem>> {
            override fun onResponse(
                call: Call<WeatherResponse<SrtItem>>, response: Response<WeatherResponse<SrtItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData: WeatherResponse<SrtItem>? = response.body()

                    val baseTime = responseData?.response?.body?.items?.item?.get(0)?.baseTime
                    val baseDate = responseData?.response?.body?.items?.item?.get(0)?.baseDate
                    var pty: String? = null
                    var t1h: String? = null
                    var rn1: String? = null
                    var reh: String? = null

                    responseData?.response?.body?.items?.item?.forEach {
                        when (it.category) {
                            "PTY" -> {
                                pty = it.obsrValue!!
                            }

                            "T1H" -> {
                                t1h = it.obsrValue!!
                            }

                            "RN1" -> {
                                rn1 = it.obsrValue!!
                            }

                            "REH" -> {
                                reh = it.obsrValue!!
                            }
                        }
                    }

                    val ultraSrtNcst =
                        UltraSrtNcst(baseTime!!, baseDate!!, pty!!, t1h!!, rn1!!, reh!!)

                    val ultraSrtNcstJSonString = Gson().toJson(ultraSrtNcst)

                    val editor = mSharedPreferences.edit()
                    editor.putString(
                        Constants.WEATHER_RESPONSE_DATA_ULTRA_NCST, ultraSrtNcstJSonString
                    )
                    editor.apply()

                    isCompletedCallUltraSrtNcst = true

                    setupSrtUI()

                } else {
                    val rc = response.code()

                    when (rc) {
                        400 -> Log.e("Error 400.", "Bad BadConnection")

                        404 -> Log.e("Error 404.", "Not Found")

                        else -> Log.e("Error.", "Generic Error")

                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse<SrtItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    updatedSrtUI = true

                    hideProgressDialog()

                    showRequestFailedDialog("단기예보데이터 갱신 실패: ${t.message.toString()} 재시도하시겠습니까?")
                } else {
                    Log.e(
                        "ultraSrtNcstCall Request Errorrrrr count $requestCount.",
                        t.message.toString()
                    )
                    ultraSrtNcstCall.cancel()
                    ultraSrtNcstCall.clone().enqueue(this)
                }
            }
        })
    }

    private fun showRequestFailedDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("갱신 실패")
            .setMessage(message)
            .setPositiveButton(
                "재시도"
            ) { _, _ ->
                checkPermissionsAndRequestData()
            }.setNegativeButton("아니요") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showCustomProgressDialog() {
        if (mProgressDialog == null) mProgressDialog = Dialog(this)

        if (!mProgressDialog!!.isShowing) {
            mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog!!.setCancelable(false)

            mProgressDialog!!.show()
        }
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null && updatedSrtUI && updatedDustUI && updatedMidUI) {
            mProgressDialog!!.dismiss()

            updatedSrtUI = false
            updatedDustUI = false
            updatedMidUI = false
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setupSrtUI() {

        if (isCompletedCallUltraSrtNcst && isCompletedCallUltraSrtFcst && isCompletedCallVilageFcst) {
            val ultraSrtNcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_NCST, "")
            val ultraSrtFcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_FCST, "")
            val vilageFcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_VILAGE_FCST, "")

            if (!ultraSrtNcstJsonString.isNullOrEmpty() && !ultraSrtFcstJsonString.isNullOrEmpty() && !vilageFcstJsonString.isNullOrEmpty()) {
                isCompletedCallUltraSrtNcst = false
                isCompletedCallUltraSrtFcst = false
                isCompletedCallVilageFcst = false

                val ultraSrtNcst = getUltraSrtNcstFromSP()
                val ultraSrtFcsts = setViewAndGetUltraSrtFcstsFromSP(ultraSrtNcst)
                val vilageFcsts = getVilageFcstsFromSP()

                val timeWeatherList = createTimeWeatherList(vilageFcsts, ultraSrtFcsts)

                binding?.rvSrtFcst?.adapter = TimeWeatherAdapter(this, timeWeatherList)

                binding?.tvTemp?.text = ultraSrtNcst.t1h + " °C"
                binding?.tvHumidity?.text = ultraSrtNcst.reh + " %"
                binding?.tvPrecipitation?.text = ultraSrtNcst.rn1 + "mm"

                updatedSrtUI = true

                saveUpdateInfo()

                hideProgressDialog()
            }
        }
    }

    private fun setupDustUI() {
        val dustDataJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_DUST, "")

        if (!dustDataJsonString.isNullOrEmpty()) {
            val dustItems =
                Gson().fromJson(dustDataJsonString, ArrayList<Map<String, String>>()::class.java)

            dustItems.filter { it["pm10Flag"] == null && it["pm25Flag"] == null }
                .maxBy { it["dataTime"]!! }
                .also {
                    binding?.tvPm10?.text = it["pm10Value"]
                    binding?.tvPm25?.text = it["pm25Value"]
                    binding?.tvStation?.text = it["stationName"]
                    binding?.tvDatetime?.text = it["dataTime"]!!.substring(5).replace("-", ".")

                    binding?.tvPm10?.setTextColor(getDustColor(it["pm10Grade1h"]!!))
                    binding?.tvPm25?.setTextColor(getDustColor(it["pm25Grade1h"]!!))

                    updatedDustUI = true

                    saveUpdateInfo()

                    hideProgressDialog()
                }
        }
    }


    private fun setupMidUI() {
        val midTaItemJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_MID_TA, "")

        val midLandItemJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_MID_LAND, "")

        if (!midTaItemJsonString.isNullOrEmpty() && !midLandItemJsonString.isNullOrEmpty() && isCompletedCallMidTa && isCompletedCallMidLand) {
            val midTaItem =
                Gson().fromJson(midTaItemJsonString, LinkedHashMap<String, Int>()::class.java)

            val midLandItem =
                Gson().fromJson(midLandItemJsonString, LinkedHashMap<String, Any>()::class.java)

            val midTas = ArrayList<MidTa>()

            for (afterDate in 3..10) {
                val taMin = midTaItem["taMin${afterDate}"]!!
                val taMinLow = midTaItem["taMin${afterDate}Low"]!!
                val taMinHigh = midTaItem["taMin${afterDate}High"]!!
                val taMax = midTaItem["taMax$afterDate"]!!
                val taMaxLow = midTaItem["taMax${afterDate}Low"]!!
                val taMaxHigh = midTaItem["taMax${afterDate}High"]!!

                val rnStAm = midLandItem["rnSt${afterDate}Am"]!! as Double
                val rnStPm = midLandItem["rnSt${afterDate}Pm"]!! as Double

                val wfAm = midLandItem["wf${afterDate}Am"]?.toString()
                    ?: midLandItem["wf${afterDate}"]!!.toString()
                val wfPm = midLandItem["wf${afterDate}Pm"]?.toString()
                    ?: midLandItem["wf${afterDate}"]!!.toString()

                val midTa = MidTa(
                    afterDate,
                    taMin,
                    taMinLow,
                    taMinHigh,
                    taMax,
                    taMaxLow,
                    taMaxHigh,
                    rnStAm.toInt(),
                    rnStPm.toInt(),
                    wfAm,
                    wfPm
                )

                midTas.add(midTa)
            }

            binding?.rvMid?.adapter = MidWeatherAdapter(this, midTas)
            binding?.rvMid?.layoutManager = LinearLayoutManager(this)

            updatedMidUI = true

            saveUpdateInfo()
            hideProgressDialog()
        }
    }

    private fun saveUpdateInfo() {
        if (updatedSrtUI && updatedDustUI && updatedMidUI && !isFirst) {
            val currentDatetime =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm"))

            mSharedPreferences.edit()
                .putString(Constants.WEATHER_REQUEST_DATETIME, currentDatetime)
                .putString(Constants.WEATHER_REQUEST_ADDRESS, mAddress)
                .apply()

            binding?.tvUpdatedTime?.text = "갱신: $currentDatetime  자료: 기상청"
            binding?.tvUpdatedAddress?.text = "위치: $mAddress"
        }
    }

    private fun createTimeWeatherList(
        vilageFcsts: ArrayList<VilageFcst>, ultraSrtFcsts: ArrayList<UltraSrtFcst>
    ): ArrayList<VilageFcst> {
        val timeWeatherList = ArrayList<VilageFcst>()

        ultraSrtFcsts.forEachIndexed { index, it ->
            val isLast = index == ultraSrtFcsts.size - 1
            var dataAdded = false

            val currentHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"))

            if (it.fcstTime.substring(0, 2) != currentHour) run {
                vilageFcsts.forEach { vilageFcst ->
                    if (it.fcstDate == vilageFcst.fcstDate && it.fcstTime == vilageFcst.fcstTime) {
                        timeWeatherList.add(
                            VilageFcst(
                                it.baseTime,
                                it.baseDate,
                                it.fcstTime,
                                it.fcstDate,
                                it.pty,
                                it.t1h,
                                it.sky,
                                vilageFcst.pop
                            )
                        )

                        dataAdded = true

                        if (isLast) return@forEach
                        return@run
                    }

                    if (isLast && dataAdded) {
                        timeWeatherList.add(vilageFcst)
                    }
                }
            }
        }

        return timeWeatherList
    }

    private fun getUltraSrtNcstFromSP(): UltraSrtNcst {
        val ultraSrtNcstJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_NCST, "")

        return Gson().fromJson(ultraSrtNcstJsonString, UltraSrtNcst::class.java)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setViewAndGetUltraSrtFcstsFromSP(ultraSrtNcst: UltraSrtNcst): ArrayList<UltraSrtFcst> {
        val ultraSrtFcstJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_FCST, "")

        val ultraSrtFcstsMap = Gson().fromJson(
            ultraSrtFcstJsonString, LinkedHashMap<String, Map<String, String>>()::class.java
        )

        val ultraSrtFcsts = ArrayList<UltraSrtFcst>()

        var count = 0

        ultraSrtFcstsMap.forEach { (key, ultraSrtFcstMap) ->
            val weatherCode = ultraSrtFcstMap["SKY"] + ultraSrtNcst.pty

            if (count == 0) {
                binding?.ivMain?.setImageDrawable(
                    getDrawable(
                        Constants.getDrawableIdWeather(
                            weatherCode
                        )
                    )
                )

                binding?.tvMain?.text =
                    Constants.weatherDescOpenApi[ultraSrtFcstMap["SKY"] + ultraSrtNcst.pty]
                ++count
            }

            val ultraSrtFcst = UltraSrtFcst(
                null,
                null,
                ultraSrtFcstMap["fcstTime"]!!,
                ultraSrtFcstMap["fcstDate"]!!,
                ultraSrtFcstMap["PTY"]!!,
                ultraSrtFcstMap["T1H"]!!,
                ultraSrtFcstMap["SKY"]!!
            )

            ultraSrtFcsts.add(ultraSrtFcst)
        }

        return ultraSrtFcsts
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getVilageFcstsFromSP(): ArrayList<VilageFcst> {
        val vilageFcstJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_VILAGE_FCST, "")

        val vilageFcstsMap = Gson().fromJson(
            vilageFcstJsonString, LinkedHashMap<String, Map<String, String>>()::class.java
        )


        val vilageFcsts = ArrayList<VilageFcst>()

        vilageFcstsMap.forEach { (key, vilageFcstMap) ->
            val vilageFcst = VilageFcst(
                null,
                null,
                vilageFcstMap["fcstTime"]!!,
                vilageFcstMap["fcstDate"]!!,
                vilageFcstMap["PTY"]!!,
                vilageFcstMap["TMP"]!!,
                vilageFcstMap["SKY"]!!,
                vilageFcstMap["POP"]!!
            )

            vilageFcsts.add(vilageFcst)
        }

        return vilageFcsts
    }

    private fun getUnit(): String {
        val value: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) application.resources.configuration.locales.get(
                0
            ).toString()
            else application.resources.configuration.locale.toString()

        var unit = " °C"
        if ("US" == value || "LR" == value || "MM" == value) {
            unit = " °F"
        }
        return unit
    }

    private fun unixTime(timex: Long): String {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }

    private fun getDustColor(grade: String): Int {
        return getColor(
            when (grade) {
                "1" -> R.color.dust_good
                "2" -> R.color.dust_normal
                "3" -> R.color.dust_bad
                "4" -> R.color.dust_very_bad
                else -> R.color.primary_text_color
            }
        )
    }

    private fun getBaseTime(apiType: String): String {

        var currentHour = LocalDateTime.now().hour
        val currentMinute = LocalDateTime.now().minute

        return when (apiType) {
            "ultraSrtNcst" -> {
                if (currentMinute <= 40) {
                    currentHour--
                    if (currentHour == -1) {
                        return "2300"
                    } else if (currentHour < 10) {
                        return "0${currentHour}00"
                    } else {
                        return "${currentHour}00"
                    }
                } else {
                    if (currentHour < 10) {
                        return "0${currentHour}00"
                    } else {
                        return "${currentHour}00"
                    }
                }
            }

            "ultraSrtFcst" -> {
                if (currentMinute <= 45) {
                    currentHour--
                    if (currentHour == -1) {
                        return "2330"
                    } else if (currentHour < 10) {
                        return "0${currentHour}30"
                    } else {
                        return "${currentHour}30"
                    }
                } else {
                    if (currentHour < 10) {
                        return "0${currentHour}30"
                    } else {
                        return "${currentHour}30"
                    }
                }
            }

            "vilageFcst" -> {
                if (currentMinute <= 10) {
                    currentHour--
                }

                when (currentHour) {
                    in 2..4 -> "0200"
                    in 5..7 -> "0500"
                    in 8..10 -> "0800"
                    in 11..13 -> "1100"
                    in 14..16 -> "1400"
                    in 17..19 -> "1700"
                    in 20..22 -> "2000"
                    in 23..24 -> "2300"
                    else -> "2400"
                }
            }

            else -> "0200"

        }
    }
}
