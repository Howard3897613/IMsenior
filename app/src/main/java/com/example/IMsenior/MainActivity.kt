    package com.example.IMsenior

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Bundle
    import android.widget.TextView
    import android.widget.Toast
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.appcompat.widget.SearchView
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.view.GravityCompat
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.drawerlayout.widget.DrawerLayout
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.firebase.ui.auth.AuthUI
    import com.google.android.material.bottomnavigation.BottomNavigationView
    import com.google.android.material.navigation.NavigationView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.ListenerRegistration
    import com.google.firebase.firestore.Query
    import com.example.IMsenior.fcm.FcmTokenUploader
    import com.example.IMsenior.MapActivity

    class MainActivity : AppCompatActivity() {

        companion object {
            private const val REQ_POST_NOTI = 1001
            // 頻道在 App.kt 建立，Manifest 也指定了 default channel「expiry」
            // private const val CHANNEL_ID = "expiry"
        }

        private lateinit var drawerLayout: DrawerLayout
        private lateinit var navView: NavigationView
        private lateinit var recyclerView: RecyclerView
        private lateinit var foodList: ArrayList<Food>
        private lateinit var foodAdapter: FoodAdapter
        private val db = FirebaseFirestore.getInstance()
        private var listenerRegistration: ListenerRegistration? = null

        private val apiHelper = ApiHelper()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // 只做 Android 13+ 通知權限；頻道由 App.kt 建立
            askPostNotificationsPermissionIfNeeded()

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
                return
            }
            //enableEdgeToEdge()
            //setContentView(R.layout.activity_main)
            // ✅ 上傳／刷新 FCM Token（docId=token，不會重覆長資料）
            FcmTokenUploader.uploadFcmTokenIfNeeded()

            // 如果是由通知點進來，可在這裡拿到資料做導頁或高亮
            val fromFcm = intent.getBooleanExtra("fromFcm", false)
            val foodDocPath = intent.getStringExtra("foodDocPath")
            // Log.d("MainActivity", "fromFcm=$fromFcm, foodDocPath=$foodDocPath")

            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)

            foodList = ArrayList()
            foodAdapter = FoodAdapter(this)
            recyclerView.adapter = foodAdapter

            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_sidepad -> {
                        drawerLayout.openDrawer(GravityCompat.START); true
                    }
                    R.id.nav_home -> {
                        Toast.makeText(this, getString(R.string.click_HP), Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.nav_add -> {
                        startActivity(Intent(this, activity_add::class.java)); true
                    }
                    R.id.nav_recipe -> {
                        startActivity(Intent(this, RecipeActivity::class.java)); true
                    }
                    else -> false
                }
            }

            drawerLayout = findViewById(R.id.drawer_layout)
            navView = findViewById(R.id.nav_view)

            // Header 顯示使用者名稱
            val headerView = navView.getHeaderView(0)
            val headerText = headerView.findViewById<TextView>(R.id.header_text)
            val userName = user.displayName ?: getString(R.string.anonymous_user)
            headerText.text = getString(R.string.greeting_user, userName)

            // 側欄選單
            navView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_relog -> {
                        Toast.makeText(this, "登出中…", Toast.LENGTH_SHORT).show()
                        AuthUI.getInstance().signOut(this).addOnCompleteListener {
                            stopFirestoreListener()
                            startActivity(Intent(this, SignInActivity::class.java))
                            finish()
                        }
                    }
                    // 🔹 新增的：附近賣食品的店
                    R.id.nav_nearby_shops -> {
                        startActivity(Intent(this, MapActivity::class.java))
                    }
                    R.id.nav_about -> {
                        val dialog = InfoDialogFragment.newInstance(
                            getString(R.string.about_title),
                            getString(R.string.about_text)
                        )
                        dialog.show(supportFragmentManager, "AboutDialog")
                        true
                    }
                    R.id.nav_help -> {
                        val dialog = InfoDialogFragment.newInstance(
                            getString(R.string.help_title),
                            getString(R.string.help_text)
                        )
                        dialog.show(supportFragmentManager, "HelpDialog")
                        true
                    }
                }
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }

            val searchView = findViewById<SearchView>(R.id.searchView)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    foodAdapter.filter.filter(query); return true
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    foodAdapter.filter.filter(newText); return true
                }
            })
        }

        override fun onResume() {
            super.onResume()
            FirebaseAuth.getInstance().currentUser?.let {
                listenToFirestoreChanges(it.uid)
            }
        }

        override fun onPause() {
            super.onPause()
            stopFirestoreListener()
        }

        private fun listenToFirestoreChanges(uid: String) {
            if (listenerRegistration != null) return
            listenerRegistration = db.collection("users")
                .document(uid)
                .collection("foods")
                .orderBy("endDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "監聽失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        foodList.clear()
                        for (doc in snapshot.documents) {
                            val endDateValue = doc.get("endDate")
                            val endDateInt = when (endDateValue) {
                                is Long -> endDateValue.toInt()
                                is String -> endDateValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                            val categoryValue = doc.get("category")
                            val categoryInt = when (categoryValue) {
                                is Long -> categoryValue.toInt()
                                is String -> categoryValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                            val foodWithId = Food(
                                id = doc.id,
                                productName = doc.getString("productName") ?: "",
                                brand = doc.getString("brand") ?: "",
                                category = categoryInt,
                                createDate = doc.getString("createDate") ?: "",
                                endDate = endDateInt,
                                quantityUnit = doc.getString("quantityUnit") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: ""
                            )
                            foodList.add(foodWithId)
                        }
                        foodAdapter.updateData(foodList)
                    } else {
                        Toast.makeText(this, "無資料", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        private fun stopFirestoreListener() {
            listenerRegistration?.remove()
            listenerRegistration = null
        }

        /** Android 13+ 動態請求通知權限 */
        private fun askPostNotificationsPermissionIfNeeded() {
            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQ_POST_NOTI
                    )
                }
            }
        }
    }
