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

    private var isCompletedCallUltraSrtNcst = true
    private var isCompletedCallUltraSrtFcst = true
    private var isCompletedCallVilageFcst = true

    private var ultraSrtNcstBaseTime = "0000"
    private var ultraSrtFcstBaseTime = "0030"
    private var vilageFcstBaseTime = "0000"
    private var currentDate = "000000"

    private var binding: ActivityMainBinding? = null

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        showCustomProgressDialog()

        setupUI()

        isCompletedCallUltraSrtNcst = false
        isCompletedCallUltraSrtFcst = false
        isCompletedCallVilageFcst = false

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
                requestLocationData()
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

            // tm 좌표로 변환
            val pt = CoordPoint(longitude, latitude)
            val transCoord =
                TransCoord.getTransCoord(pt, TransCoord.COORD_TYPE_WGS84, TransCoord.COORD_TYPE_TM)

            val tmX = transCoord.x
            val tmY = transCoord.y
            // tm 좌표로 변환 끝

            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build()

            weatherService = retrofit.create(WeatherService::class.java)

            ultraSrtNcstBaseTime = getBaseTime("ultraSrtNcst")
            ultraSrtFcstBaseTime = getBaseTime("ultraSrtFcst")
            vilageFcstBaseTime = getBaseTime("vilageFcst")

            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            // 초단기 실황 Call
            val ultraSrtNcstCall: Call<WeatherResponse> = weatherService.getOpenApiWeather(
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
            val ultraSrtFcstCall: Call<WeatherResponse> = weatherService.getOpenApiWeather(
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
            val vilageFcstCall: Call<WeatherResponse> = weatherService.getOpenApiWeather(
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

        }
    }

    private fun enqueueNearStationCall(nearStationCall: Call<DustResponse<StationItem>>) {
        var requestCount = 0

        nearStationCall.enqueue(object : Callback<DustResponse<StationItem>> {
            override fun onResponse(
                call: Call<DustResponse<StationItem>>, response: Response<DustResponse<StationItem>>
            ) {
                if (response.isSuccessful) {
                    val responseData = response.body()?.response?.body?.items!!

                    Log.d("station", responseData.toString())

                    val dustDataCall = weatherService.getMsrstnAcctoRltmMesureDnsty(
                        Constants.OPENAPI_API_KEY, "json", 1, 24, responseData[0].stationName
                    )

                    enqueueDustData(dustDataCall)
                }
            }

            override fun onFailure(call: Call<DustResponse<StationItem>>, t: Throwable) {
                requestCount++


                Log.e("nearStationCall Request Errorrrrr.", t.message.toString())


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

                    Log.d("dustString", dustItemsJsonString)
                    Log.d("dustString", responseData.toString())

                    val editor = mSharedPreferences.edit()
                    editor.putString(
                        Constants.WEATHER_RESPONSE_DATA_DUST, dustItemsJsonString
                    )
                    editor.apply()

                    setupUI()
                }
            }

            override fun onFailure(call: Call<DustResponse<DustItem>>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    hideProgressDialog()
                    Log.e("dustDataCall Request Errorrrrr.", t.message.toString())
                }

                Log.e("dustDataCall Request Errorrrrr count $requestCount.", t.message.toString())
//                dustDataCall.cancel()
//                dustDataCall.enqueue(this)
            }
        })
    }

    private fun enqueueVilageNcstCall(vilageFcstCall: Call<WeatherResponse>) {
        var requestCount = 0

        vilageFcstCall.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>, response: Response<WeatherResponse>
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

                    setupUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    hideProgressDialog()
                    Log.e("vilageFcstCall Request Errorrrrr.", t.message.toString())
                }

                Log.e("vilageFcstCall Request Errorrrrr count $requestCount.", t.message.toString())
//                vilageFcstCall.cancel()
//                vilageFcstCall.enqueue(this)
            }
        })
    }

    private fun enqueueUltraSrtFcstCall(ultraSrtFcstCall: Call<WeatherResponse>) {
        var requestCount = 0

        ultraSrtFcstCall.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>, response: Response<WeatherResponse>
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

                    setupUI()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    hideProgressDialog()
                    Log.e("ultraSrtFcstCall Request Errorrrrr.", t.message.toString())
                }

                Log.e(
                    "ultraSrtFcstCall Request Errorrrrr count $requestCount.",
                    t.message.toString()
                )
//                ultraSrtFcstCall.cancel()
//                ultraSrtFcstCall.enqueue(this)
            }
        })
    }

    private fun enqueueUltraSrtNcstCall(ultraSrtNcstCall: Call<WeatherResponse>) {
        var requestCount = 0

        ultraSrtNcstCall.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>, response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val responseData: WeatherResponse? = response.body()

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

                    setupUI()

                } else {
                    val rc = response.code()

                    when (rc) {
                        400 -> Log.e("Error 400.", "Bad BadConnection")

                        404 -> Log.e("Error 404.", "Not Found")

                        else -> Log.e("Error.", "Generic Error")

                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                requestCount++

                if (requestCount > 2) {
                    hideProgressDialog()
                    Log.e("ultraSrtNcstCall Request Errorrrrr.", t.message.toString())
                }

                Log.e(
                    "ultraSrtNcstCall Request Errorrrrr count $requestCount.",
                    t.message.toString()
                )
//                ultraSrtNcstCall.cancel()
//                ultraSrtNcstCall.enqueue(this)
            }
        })
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

            mProgressDialog!!.show()
        }
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setupUI() {

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

                hideProgressDialog()
                return
            }
        }

        val dustDataJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_DUST, "")

        if (!dustDataJsonString.isNullOrEmpty()) {
            val dustItems =
                Gson().fromJson(dustDataJsonString, ArrayList<Map<String, String>>()::class.java)

            dustItems.forEach {
                if (it["pm10Flag"] == null && it["pm25Flag"] == null) {
                    binding?.tvPm10?.text = it["pm10Value"]
                    binding?.tvPm25?.text = it["pm25Value"]
                    binding?.tvStation?.text = it["stationName"]
                    binding?.tvDatetime?.text = it["dataTime"]!!.substring(5).replace("-", ".")

                    binding?.tvPm10?.setTextColor(getDustColor(it["pm10Grade1h"]!!))
                    binding?.tvPm25?.setTextColor(getDustColor(it["pm25Grade1h"]!!))

                    return
                }
            }
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
            if (count == 0) {
                when (ultraSrtNcst.pty) {
                    "1", "4", "5" -> {
                        binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.rain))
                    }

                    "2", "3", "6", "7" -> {
                        binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.snowflake))
                    }

                    else -> {
                        if (ultraSrtFcstMap["SKY"] == "4") {
                            binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.cloud))
                        } else {
                            binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.sunny))
                        }
                    }
                }

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
                    else -> "2300"
                }
            }

            else -> "0200"

        }
    }
}
