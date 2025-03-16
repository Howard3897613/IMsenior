package com.example.IMsenior

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okio.IOException
import org.json.JSONObject
import okhttp3.Response
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    private val apiHelper = ApiHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // 測試條碼 (用實際條碼代替 737628064502)
        val testBarcode = "737628064502"
        apiHelper.fetchFoodData(testBarcode) { productName, genericName ->
            runOnUiThread {
                Toast.makeText(this, "名稱: $productName\n分類: $genericName", Toast.LENGTH_LONG)
                    .show()
                val ProductEd = findViewById<EditText>(R.id.editProdect)
                val GenericEd = findViewById<EditText>(R.id.editGeneric)

                ProductEd.setText(productName)
                GenericEd.setText(genericName)
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
