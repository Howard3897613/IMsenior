package com.example.IMsenior

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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

class activity_edit : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val productEd = findViewById<EditText>(R.id.editProduct)
        val enddateEd = findViewById<EditText>(R.id.editenddate)
        val brandEd = findViewById<EditText>(R.id.editBrand)
        val category = findViewById<RadioGroup>(R.id.category)
        val quantity = findViewById<EditText>(R.id.quantity)
        val db = Firebase.firestore
        val dateIcon = findViewById<ImageView>(R.id.dateIcon)
        val addbottomNav = findViewById<BottomNavigationView>(R.id.btmedit)

        //填入資料
        val foodId = intent.getStringExtra("foodId")
        val productName = intent.getStringExtra("productName")
        val brand = intent.getStringExtra("brand")
        val categoryValue = intent.getIntExtra("category", 0)
        val createDate = intent.getStringExtra("createDate")
        val endDate = intent.getIntExtra("endDate", 0)
        val quantityUnit = intent.getStringExtra("quantityUnit")

        // 填進去 UI
        productEd.setText(productName)
        brandEd.setText(brand)
        quantity.setText(quantityUnit)
        enddateEd.setText(endDate.toString())

        // category 用 RadioGroup 預選
        when (categoryValue) {
            1 -> category.check(R.id.food)
            2 -> category.check(R.id.source)
        }


        dateIcon.setOnClickListener {
            showDatePickerDialog(enddateEd)
        }

        addbottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_close -> {
                    finish()
                    true
                }

                R.id.nav_check -> {
                    Toast.makeText(this, "+++", Toast.LENGTH_SHORT).show()
                    val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
                    val data = hashMapOf<String, Any>(
                        "productName" to productEd.text.toString(),
                        "brand" to brandEd.text.toString(),
                        "category" to when (category.checkedRadioButtonId) {
                            R.id.food -> 1
                            R.id.source -> 2
                            else -> 0
                        },
                        "createDate" to formatter.format(Date()),
                        "endDate" to (enddateEd.text.toString().toIntOrNull() ?: 0),
                        "quantityUnit" to quantity.text.toString()
                    )
                    val user = FirebaseAuth.getInstance().currentUser
                    val uid = user?.uid
                    uid?.let {
                        db.collection("users")
                            .document(it)
                            .collection("foods")
                            .document(foodId!!)
                            .update(data)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "DocumentSnapshot updated!")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error editting document", e)
                            }
                    }
                    //val i = Intent().putExtras(b)
                    //setResult(RESULT_OK,i)
                    finish()
                    true
                }

                else -> false
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
}