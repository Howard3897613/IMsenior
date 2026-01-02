package com.example.IMsenior

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
//圖片存儲
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class activity_add : AppCompatActivity() {
    private val apiHelper = ApiHelper()
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var generativeModel: GenerativeModel
    private var imageUri: Uri? = null


    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "掃描結果: ${result.contents}", Toast.LENGTH_LONG).show()
            val testBarcode = findViewById<EditText>(R.id.EtBarcode)
            testBarcode.setText(result.contents)
        } else {
            Toast.makeText(this, "取消掃描", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val jsonSchema = Schema.obj(
            mapOf(
                "productName" to Schema.string(),
                "brand" to Schema.string(),
                "quantityUnit" to Schema.string(),
                "endDate" to Schema.string(),  // 或 integer，看你怎麼想
            ),optionalProperties = listOf("endDate")
        )
        generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = jsonSchema
            })

        val comfirm_Barcode = findViewById<Button>(R.id.confirm_barcode)
        val ProductEd = findViewById<EditText>(R.id.editProdect)
        val enddateEd = findViewById<EditText>(R.id.editenddate)
        val brandEd = findViewById<EditText>(R.id.editBrand)
        //val finish = findViewById<Button>(R.id.finish)
        val category = findViewById<RadioGroup>(R.id.category)
        val quantity = findViewById<EditText>(R.id.quantity)
        val db = Firebase.firestore
        val dateIcon = findViewById<ImageView>(R.id.dateIcon)
        comfirm_Barcode.setOnClickListener {
            val testBarcode = findViewById<EditText>(R.id.EtBarcode).text.toString()
            showLoading(true)
            apiHelper.fetchFoodData(testBarcode) { productName, Brand, Quantity, imageUrl ->
                runOnUiThread {
                    ProductEd.setText(productName)
                    brandEd.setText(Brand)
                    quantity.setText(Quantity)

                    // 顯示圖片，增加 placeholder 和錯誤圖片
                    val foodImageView = findViewById<ImageView>(R.id.ivFoodImage)
                    Glide.with(this)
                        .load(imageUrl)
                        //.placeholder(R.drawable.placeholder_image) // 下載中或空白時顯示
                        .placeholder(R.drawable.loading)  // 指向 GIF
                        .error(R.drawable.error_image)             // 下載失敗時顯示
                        .into(foodImageView)
                    // 把 imageUrl 存到 ImageView tag，方便儲存到 Firebase
                    foodImageView.tag = imageUrl
                    showLoading(false)
                }
            }
        }
        val addbottomNav = findViewById<BottomNavigationView>(R.id.btmaddv)

        addbottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> {
                    Toast.makeText(this, "這是一個提示訊息，scan", Toast.LENGTH_SHORT).show()
                    val options = ScanOptions()
                    options.setPrompt("請將條碼置於畫面中央")
                    options.setBeepEnabled(true)
                    options.setOrientationLocked(true)
                    options.setCaptureActivity(CaptureAct::class.java) // 自訂掃描畫面（下一步會定義）
                    barcodeLauncher.launch(options)
                    true
                }

                R.id.nav_check -> {
                    Toast.makeText(this, "+++", Toast.LENGTH_SHORT).show()
                    val foodImageView = findViewById<ImageView>(R.id.ivFoodImage)
                    val imageUrl = foodImageView.tag?.toString() ?: ""  // 取出之前存的圖片 URL
                    val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
                    val data = hashMapOf(
                        "productName" to ProductEd.text.toString(),
                        "brand" to brandEd.text.toString(),
                        "category" to when (category.checkedRadioButtonId) {
                            R.id.food -> 1
                            R.id.source -> 2
                            else -> 0
                        },
                        "createDate" to formatter.format(Date()),
                        "endDate" to enddateEd.text.toString().toInt(),
                        "quantityUnit" to quantity.text.toString(),
                        "imageUrl" to imageUrl   // 新增圖片欄位
                    )
                    val user = FirebaseAuth.getInstance().currentUser
                    val uid = user?.uid
                    uid?.let {
                        db.collection("users")
                            .document(it)
                            .collection("foods")
                            .add(data)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding document", e)
                            }
                    }
                    finish()
                    true
                }

                R.id.nav_picture -> {
                    //pickImageForAI()
                    showImageSourceDialog()
                    true
                }

                else -> false
            }
        }


        dateIcon.setOnClickListener {
            showDatePickerDialog(enddateEd)
        }



    }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            analyzeImageWithAI(bitmap)
        }
    }

    //拍照
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let { uri ->
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    analyzeImageWithAI(bitmap)
                }
            } else {
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show()
            }
        }
    private fun showImageSourceDialog() {
        val options = arrayOf(getString(R.string.takpic), getString(R.string.picpic))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val uri = createImageUri()
        if (uri != null) {
            imageUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(this, "Cannot store", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openGallery() {
        imagePicker.launch("image/*")
    }

    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun pickImageForAI() {
        imagePicker.launch("image/*")
    }

    private fun analyzeImageWithAI(bitmap: Bitmap) {
        val promt = getString(R.string.returnAI)
        val today = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        println("今日日期: ${formatter.format(today)}")
        lifecycleScope.launch {
            showLoading(true)
            uploadImageToFirebase(bitmap)
            try {
                val request = content {
                    image(bitmap)
                    text("""
                    使用者要求：$promt
                    今日日期：${formatter.format(today)}
                    請分析這張食品圖片，並以 JSON 格式回覆，格式如下：
                    {
                        "productName": "...",
                        "brand": "...",
                        "quantityUnit": "...",
                        "endDate": "YYYYMMDD"
                    }
                    endDate若無法判斷可以推測方式進行，
                    quantityUnit以數量加單位回傳
                """.trimIndent())
                }

                val response = generativeModel.generateContent(request)
                val result = response.text ?: "{}"
                print(response.text)

                // 嘗試解析 JSON
                try {
                    val json = JSONObject(result)
                    val productName = json.optString("productName", "")
                    val brand = json.optString("brand", "")
                    val quantity = json.optString("quantityUnit", "")
                    val endDate = json.optString("endDate", "")

                    // 自動填入輸入框
                    findViewById<EditText>(R.id.editProdect).setText(productName)
                    findViewById<EditText>(R.id.editBrand).setText(brand)
                    findViewById<EditText>(R.id.quantity).setText(quantity)
                    findViewById<EditText>(R.id.editenddate).setText(endDate)

                } catch (e: Exception) {
                    Toast.makeText(this@activity_add, "解析失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("解析失敗", "AI 分析錯誤", e)
                }

            } catch (e: Exception) {
                Toast.makeText(this@activity_add, "AI 分析錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AI_ANALYSIS", "AI 分析錯誤", e)
            }finally {
                showLoading(false)
            }
        }
    }



    private fun showDatePickerDialog(enddateed: EditText) {
        // 獲取當前日期
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 創建並顯示 DatePickerDialog

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedMonth = String.format("%02d", selectedMonth + 1) // 月份從 0 開始，所以要 +1
            val formattedDay = String.format("%02d", selectedDay)
            val selectedDate = "$selectedYear$formattedMonth$formattedDay"
            enddateed.setText(selectedDate) // 更新 EditText 顯示選擇的日期
        }, year, month, day)

        datePickerDialog.show()
    }



    private fun showLoading(isLoading: Boolean) {
        val overlay = findViewById<View>(R.id.loadingOverlay)
        val animTime = 200L
        if (isLoading) {
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            overlay.animate().alpha(1f).setDuration(animTime).start()
        } else {
            overlay.animate().alpha(0f).setDuration(animTime).withEndAction {
                overlay.visibility = View.GONE
            }.start()
        }
    }

    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val storageRef = FirebaseStorage.getInstance().reference
        // 建立一個唯一的檔名，例如: images/userUID_timestamp.jpg
        val fileName = "images/${FirebaseAuth.getInstance().currentUser?.uid}_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child(fileName)

        // 將 Bitmap 壓縮為 JPEG
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos) // 品質 80
        val data = baos.toByteArray()

        // 上傳
        val uploadTask = imageRef.putBytes(data)

        // 顯示上傳中的 loading (可選，看你要不要跟 AI 分析共用 loading)
        // showLoading(true)

        uploadTask.addOnSuccessListener {
            // 上傳成功，取得下載 URL
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                Log.d(TAG, "圖片上傳成功: $downloadUrl")

                // *** 關鍵步驟 ***
                // 1. 用 Glide 顯示 (雖然本地已有 bitmap，但這裡確認上傳成功再顯示也可以，或維持原樣)
                val foodImageView = findViewById<ImageView>(R.id.ivFoodImage)
                Glide.with(this)
                    .load(downloadUrl) // 這裡改用網址載入，確保顯示的是雲端那張
                    .placeholder(R.drawable.loading)
                    .into(foodImageView)

                // 2. 將網址存入 Tag，讓你的「儲存按鈕」邏輯可以讀取到
                foodImageView.tag = downloadUrl
            }
        }.addOnFailureListener {
            Toast.makeText(this, "圖片上傳失敗", Toast.LENGTH_SHORT).show()
        }
    }


}

class ApiHelper {
    private val client = OkHttpClient()

    // callback 改成 4 個參數：產品名稱、品牌、數量、圖片 URL
    fun fetchFoodData(barcode: String, callback: (String, String, String, String) -> Unit) {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API_ERROR", "無法取得資料: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    try {
                        val jsonObject = JSONObject(jsonResponse)
                        val product = jsonObject.optJSONObject("product")
                        val productName = product?.optString("product_name", "未知") ?: "未知"
                        val Brand = product?.optString("brands", "未知") ?: "未知"
                        val quantityUnit = product?.optString("serving_size", "未知") ?: "未知"
                        val imageUrl = product?.optString("image_url", "") ?: ""  // 新增圖片 URL

                        // 回傳結果（產品名稱、品牌、數量、圖片 URL）
                        callback(productName, Brand, quantityUnit, imageUrl)
                    } catch (e: Exception) {
                        Log.e("JSON_ERROR", "解析失敗: ${e.message}")
                    }
                }
            }
        })
    }
}



