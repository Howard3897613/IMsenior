package com.example.IMsenior

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okio.IOException
import org.json.JSONObject
import okhttp3.Response
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult

class MainActivity : AppCompatActivity() {
    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        // Step12：判斷回傳結果是否為 RESULT_OK，若是則執行以下程式碼
        if (result.resultCode == Activity.RESULT_OK) {
            // Step13：取得回傳的 Intent，並從 Intent 中取得飲料名稱、甜度、冰塊的值
            val intent = result.data
            val productName = intent?.getStringExtra("productName")
            val genericName = intent?.getStringExtra("genericName")
            val category = intent?.getStringExtra("category")

            // Step14：設定 tvMeal 的文字
            val tvMeal = findViewById<TextView>(R.id.tvMeal)
            tvMeal.text = "產品：$productName\n通用名稱：$genericName\n類別：$category"
        }
    }

    private val apiHelper = ApiHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btntoAdd = findViewById<Button>(R.id.btntoAdd)
        btntoAdd.setOnClickListener {
            val intent = Intent(this, activity_add::class.java)
            startForResult.launch(intent)
        }


        // 測試條碼 (用實際條碼代替 737628064502)
        /*val testBarcode = "737628064502"
        apiHelper.fetchFoodData(testBarcode) { productName, genericName ->
            runOnUiThread {
                Toast.makeText(this, "名稱: $productName\n分類: $genericName", Toast.LENGTH_LONG)
                    .show()
                val ProductEd = findViewById<EditText>(R.id.editProdect)
                val GenericEd = findViewById<EditText>(R.id.editGeneric)

                ProductEd.setText(productName)
                GenericEd.setText(genericName)
            }
        }*/
    }
}

/*class ApiHelper {
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
*/