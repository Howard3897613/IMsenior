package com.example.IMsenior

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import okhttp3.Response
import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private val apiHelper = ApiHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 測試條碼 (用實際條碼代替 737628064502)
        val testBarcode = "737628064502"
        apiHelper.fetchFoodData(testBarcode) { productName, genericName ->
            runOnUiThread {
                Toast.makeText(this, "名稱: $productName\n分類: $genericName", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class ApiHelper {
    private val client = OkHttpClient()

    fun fetchFoodData(barcode: String, callback: (String, String) -> Unit) {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_ERROR", "無法取得資料: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    try {
                        val jsonObject = JSONObject(jsonResponse)
                        val product = jsonObject.optJSONObject("product")
                        val productName = product?.optString("product_name", "未知") ?: "未知"
                        val genericName = product?.optString("generic_name", "未知") ?: "未知"

                        // 回傳結果
                        callback(productName, genericName)
                    } catch (e: Exception) {
                        Log.e("JSON_ERROR", "解析失敗: ${e.message}")
                    }
                }
            }
        })
    }
}

/*
class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance().reference.child("foods")

        // 呼叫相機初始化
        startCamera()
    }

    private fun startCamera() {
        // 獲取相機提供者實例
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // 透過 addListener 確保處理異步操作
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 建立 Preview 用來顯示相機畫面
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }

            // 建立條碼掃描器
            val barcodeScanner = BarcodeScanning.getClient()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processBarcode(imageProxy, barcodeScanner)
                    }
                }

            // 選擇後置攝像頭
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 綁定相機與相機生命周期
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))  // 在主線程執行
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processBarcode(imageProxy: ImageProxy, scanner: BarcodeScanner) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val barcodeValue = barcode.rawValue
                        if (!barcodeValue.isNullOrEmpty()) {
                            fetchFoodData(barcodeValue)
                        }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun fetchFoodData(barcode: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_ERROR", "Failed to fetch data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonResponse ->
                    try {
                        val jsonObject = JSONObject(jsonResponse)
                        val product = jsonObject.getJSONObject("product")
                        val productName = product.optString("product_name", "未知")
                        val genericName = product.optString("generic_name", "未知")

                        runOnUiThread {
                            showSaveDialog(productName, genericName)
                        }
                    } catch (e: JSONException) {
                        Log.e("JSON_ERROR", "Failed to parse JSON: ${e.message}")
                    }
                }
            }
        })
    }

    private fun showSaveDialog(productName: String, genericName: String) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_DATETIME

        AlertDialog.Builder(this)
            .setTitle("輸入保存期限")
            .setView(input)
            .setPositiveButton("儲存") { _, _ ->
                val endDate = input.text.toString()
                saveToFirebase(productName, genericName, endDate)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveToFirebase(productName: String, genericName: String, endDate: String) {
        val createDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val food = mapOf(
            "category" to 1,
            "product_name" to productName,
            "generic_name" to genericName,
            "create_date" to createDate,
            "end_date" to endDate
        )

        database.push().setValue(food)
            .addOnSuccessListener { Toast.makeText(this, "儲存成功！", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Log.e("DB_ERROR", "儲存失敗: ${e.message}") }
    }
}
*/