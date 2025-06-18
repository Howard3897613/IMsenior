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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var foodList: ArrayList<Food>
    private lateinit var foodAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()  // Firestore 實例

    /*private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        // Step12：判斷回傳結果是否為 RESULT_OK，若是則執行以下程式碼
        if (result.resultCode == Activity.RESULT_OK) {
            // Step13：取得回傳的 Intent，並從 Intent 中取得飲料名稱、甜度、冰塊的值
            val intent = result.data
            val productName = intent?.getStringExtra("productName")
            val genericName = intent?.getStringExtra("genericName")
            val category = intent?.getStringExtra("category")

        }
    }*/

    private val apiHelper = ApiHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        /*val btntoAdd = findViewById<Button>(R.id.btntoAdd)
        btntoAdd.setOnClickListener {
            val intent = Intent(this, activity_add::class.java)
            startForResult.launch(intent)
        }*/
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        foodList = ArrayList()

        foodAdapter = FoodAdapter(foodList, this)
        recyclerView.adapter = foodAdapter

        listenToFirestoreChanges()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "這是一個提示訊息，首頁", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_add -> {
                    Toast.makeText(this, "+++", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, activity_add::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_recipe -> {
                    Toast.makeText(this, "菜單", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

    }
    /*
    private fun loadDataFromFirestore() {
        db.collection("foods")
            .get()
            .addOnSuccessListener { snapshot ->
                foodList.clear()
                for (doc in snapshot.documents) {
                    val food = doc.toObject(Food::class.java)
                    if (food != null) {
                        val foodWithId = food.copy(id = doc.id)
                        foodList.add(foodWithId)
                    }
                }
                foodAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "讀取失敗", Toast.LENGTH_SHORT).show()
            }
    }*/
    private fun listenToFirestoreChanges() {
        db.collection("foods")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "監聽失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    foodList.clear()
                    for (doc in snapshot.documents) {
                        val food = doc.toObject(Food::class.java)
                        if (food != null) {
                            val foodWithId = food.copy(id = doc.id)
                            foodList.add(foodWithId)
                        }
                    }
                    foodAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "無資料", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
