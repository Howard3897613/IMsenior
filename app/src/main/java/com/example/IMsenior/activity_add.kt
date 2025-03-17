package com.example.IMsenior

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class activity_add : AppCompatActivity() {
    private val apiHelper = ApiHelper()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val comfirm_Barcode = findViewById<Button>(R.id.confirm_barcode)
        val ProductEd = findViewById<EditText>(R.id.editProdect)
        val GenericEd = findViewById<EditText>(R.id.editGeneric)
        val finish = findViewById<Button>(R.id.finish)
        val category = findViewById<RadioGroup>(R.id.category)
        comfirm_Barcode.setOnClickListener {
            val testBarcode = findViewById<EditText>(R.id.EtBarcode).text.toString()
            apiHelper.fetchFoodData(testBarcode) { productName, genericName ->
                runOnUiThread {

                    ProductEd.setText(productName)
                    GenericEd.setText(genericName)
                }
            }
        }

        finish.setOnClickListener {
            val b = bundleOf(
                "productName" to ProductEd.text.toString(),
                "genericName" to GenericEd.text.toString(),
                "category" to category.findViewById<RadioButton>(category.checkedRadioButtonId).text.toString()



            )
            val i = Intent().putExtras(b)
            setResult(RESULT_OK,i)
            finish()
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