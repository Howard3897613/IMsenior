package com.example.IMsenior

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class RecipeActivity : AppCompatActivity() {
    private lateinit var inputPrompt: EditText
    private lateinit var btnSend: Button
    private lateinit var tvResponse: TextView
    private lateinit var bottomNav: BottomNavigationView
    private val db = FirebaseFirestore.getInstance()

    private lateinit var generativeModel: GenerativeModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        inputPrompt = findViewById(R.id.inputPrompt)
        btnSend = findViewById(R.id.btnSend)
        tvResponse = findViewById(R.id.tvResponse)

        // 初始化 AI 模型
        generativeModel = Firebase.ai( backend = GenerativeBackend.googleAI() ).generativeModel("gemini-2.5-flash")

        bottomNav = findViewById(R.id.bottom_nav)

        // 點擊事件
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    // 這裡就能直接開你的 AI Activity
                    finish()
                    true
                }
                R.id.nav_copy -> {
                    val textToCopy = tvResponse.text.toString()

                    if (textToCopy.isNotBlank()) {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("CopiedText", textToCopy)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(this, getString(R.string.copyied), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.uncopied), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        btnSend.setOnClickListener {
            val prompt = inputPrompt.text.toString()
                fetchFirestoreDataAndAskAI(prompt)
            //askAI(prompt)

        }
    }



    private fun fetchFirestoreDataAndAskAI(userInput: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("foods")
            .whereEqualTo("category", 2)
            .get()
            .addOnSuccessListener { result ->
                val foods = mutableListOf<String>()
                for (doc in result) {
                    val name = doc.getString("productName") ?: "未知食材"
                    val endDateInt = doc.getLong("endDate")?.toInt() ?: 0  // 先用 getLong 再轉成 Int
                    val endDate = endDateInt.toString()
                    val quantity = doc.getString("quantityUnit") ?: "未設定數量"
                    foods.add("$name (保存期限: $endDate、數量: $quantity)")
                }

                val prompt = buildPrompt(userInput, foods)
                askAI(prompt)
            }
            .addOnFailureListener { e ->
                tvResponse.text = "讀取 Firestore 失敗: ${e.message}"
            }
    }

    private fun askAI(prompt: String) {
        tvResponse.text = "AI 思考中..."
        lifecycleScope.launch {
            try {
                val request = content {
                    text(prompt)
                }
                val response = generativeModel.generateContent(request)
                tvResponse.text = response.text ?: "AI 沒有回覆"
            } catch (e: Exception) {
                tvResponse.text = "錯誤: ${e.message}"
            }
        }
    }


    private fun buildPrompt(userInput: String, firestoreData: List<String>): String {
        val dataString = firestoreData.joinToString("\n") { "- $it" }
        val promt = getString(R.string.returnAI)
        return """
            $promt
            使用者問題: $userInput

            以下是目前使用者擁有的食材資料:
            $dataString

            請參考食材資料來生成一份具有詳細作法的食譜。請無視那些你認為不可用的項目。
        """.trimIndent()
    }
}