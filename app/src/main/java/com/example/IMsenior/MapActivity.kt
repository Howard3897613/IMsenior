package com.example.IMsenior

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val httpClient = OkHttpClient()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var shopMarkers: MutableList<Marker> = mutableListOf()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) {
                enableMyLocationLayer()
                setupMyLocationButtonBehavior()
                moveToMyLocation(showToast = false)
            } else {
                Toast.makeText(this, "需要定位權限才能使用定位/附近搜尋", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as? SupportMapFragment

        if (mapFragment == null) {
            Toast.makeText(this, "地圖載入失敗（map_fragment 不存在）", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        mapFragment.getMapAsync(this)

        // 右下角：App 內搜尋附近店家
        findViewById<FloatingActionButton>(R.id.btn_search_shops).setOnClickListener {
            searchNearbyFoodStoresInApp()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // ✅ 保留右上角的定位按鈕（不要關掉）
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        requestLocationPermissionIfNeeded()
    }

    private fun requestLocationPermissionIfNeeded() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            enableMyLocationLayer()
            setupMyLocationButtonBehavior()
            moveToMyLocation(showToast = false)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /** 開啟地圖的 MyLocation 圖層（藍點 & 右上角按鈕才會真的有作用） */
    private fun enableMyLocationLayer() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    /**
     * ✅ 讓右上角「定位按鈕」按下去一定有反應：
     * - 我們攔截 click
     * - 主動把鏡頭移到目前位置
     */
    private fun setupMyLocationButtonBehavior() {
        googleMap?.setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener {
            moveToMyLocation(showToast = true)
            true // true = 我們自己處理（確保有反應）
        })
    }

    /** 把鏡頭移到目前位置（拿不到就提示） */
    private fun moveToMyLocation(showToast: Boolean) {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            requestLocationPermissionIfNeeded()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    if (showToast) {
                        Toast.makeText(this, "目前無法取得定位（請開啟定位/GPS）", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.5f))
            }
            .addOnFailureListener {
                if (showToast) {
                    Toast.makeText(this, "取得定位失敗：${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * ✅ App 內搜尋附近店家（Places Nearby Search）
     * 不跳出 Google Maps App，結果直接 Marker 在你的地圖上
     */
    private fun searchNearbyFoodStoresInApp() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            requestLocationPermissionIfNeeded()
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(this, "目前無法取得定位，無法搜尋附近店家", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val lat = location.latitude
                val lng = location.longitude
                val center = LatLng(lat, lng)

                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15.5f))

                // 你可改成只要 "全聯"
                val keyword = "全聯 超市 賣場 便利商店"
                val apiKey = getString(R.string.google_maps_key)

                val url = buildNearbySearchUrl(
                    lat = lat,
                    lng = lng,
                    radiusMeters = 3000,
                    keyword = keyword,
                    apiKey = apiKey
                )

                fetchPlacesAndDrawMarkers(url)
            }
            .addOnFailureListener {
                Toast.makeText(this, "取得定位失敗：${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buildNearbySearchUrl(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        keyword: String,
        apiKey: String
    ): String {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$lat,$lng" +
                "&radius=$radiusMeters" +
                "&keyword=$encodedKeyword" +
                "&language=zh-TW" +
                "&key=$apiKey"
    }

    private fun fetchPlacesAndDrawMarkers(url: String) {
        Toast.makeText(this, "搜尋附近店家中…", Toast.LENGTH_SHORT).show()

        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "搜尋失敗：${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MapActivity,
                            "搜尋失敗（HTTP ${response.code}）",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")

                    if (status != "OK") {
                        val msg = json.optString("error_message", "status=$status")
                        runOnUiThread {
                            Toast.makeText(this@MapActivity, "Places 回傳：$msg", Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    val results = json.getJSONArray("results")

                    runOnUiThread {
                        shopMarkers.forEach { it.remove() }
                        shopMarkers.clear()

                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val name = item.optString("name", "未命名店家")
                            val vicinity = item.optString("vicinity", "")

                            val loc = item.getJSONObject("geometry")
                                .getJSONObject("location")

                            val lat = loc.getDouble("lat")
                            val lng = loc.getDouble("lng")

                            val marker = googleMap?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lng))
                                    .title(name)
                                    .snippet(vicinity)
                            )
                            if (marker != null) shopMarkers.add(marker)
                        }

                        googleMap?.setOnMarkerClickListener { m ->
                            m.showInfoWindow()
                            true
                        }

                        Toast.makeText(
                            this@MapActivity,
                            "找到 ${results.length()} 間附近店家",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "解析資料失敗：${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
