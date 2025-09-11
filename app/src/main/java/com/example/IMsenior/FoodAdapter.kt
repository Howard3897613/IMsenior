package com.example.IMsenior

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*
import android.content.ContentValues.TAG
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners



class FoodAdapter(
    //private val originalList: ArrayList<Food>,
    //private val foodList: List<Food>,
    private val context: Context
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>(), Filterable {
    private val originalList: ArrayList<Food> = ArrayList()
    private var filteredList: ArrayList<Food> = ArrayList(originalList)

    //private val filteredList: ArrayList<Food> = ArrayList()

    fun updateData(newList: List<Food>) {
        originalList.clear()
        originalList.addAll(newList)
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val brand: TextView = itemView.findViewById(R.id.tvBrand)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val createDate: TextView = itemView.findViewById(R.id.tvCreateDate)
        val endDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val quantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnClick: ImageButton = itemView.findViewById(R.id.btnClick)
        val btnedit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val foodImage: ImageView = itemView.findViewById(R.id.ivFoodItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = filteredList[position]
        val db = Firebase.firestore
        holder.name.text = food.productName
        holder.brand.text = food.brand
        holder.category.text = if (food.category == 1) {
            holder.itemView.context.getString(R.string.category_ingredient)
        } else {
            holder.itemView.context.getString(R.string.category_food)
        }
        holder.createDate.text = food.createDate
        holder.endDate.text = food.endDate.toString()
        holder.quantity.text = food.quantityUnit

        Glide.with(context)
            .load(food.imageUrl) // 食物圖片 URL
            //.placeholder(R.drawable.placeholder_image) // 載入中顯示的預設圖
            .placeholder(R.drawable.loading)  // 指向 GIF
            .error(R.drawable.empty_frame)             // 載入失敗顯示的圖片
            .transform(RoundedCorners(16))             // 圓角 16px (可調整)
            .into(holder.foodImage)

        // ========= 新增日期判斷 =========
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

// 今天日期
        val todayInt = sdf.format(Date()).toInt()

// 一週後
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val oneWeekLaterInt = sdf.format(calendar.time).toInt()

// 判斷 food.endDate
        val context = holder.itemView.context
        when {
            food.endDate < todayInt -> holder.name.setTextColor(
                ContextCompat.getColor(context, R.color.blood_red)
            )
            food.endDate <= oneWeekLaterInt -> holder.name.setTextColor(
                ContextCompat.getColor(context, R.color.warning_orange)
            )
            else -> holder.name.setTextColor(
                ContextCompat.getColor(context, R.color.normal_black)
            )
        }
        // ========= 日期判斷結束 =========

        holder.btnClick.setOnClickListener {
            Toast.makeText(context, "你點了 ${food.id}", Toast.LENGTH_SHORT).show()
         db.collection("foods").document(food.id)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "successfully deleted!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
        }
        holder.btnedit.setOnClickListener {
            Toast.makeText(context, "你點了編輯 ${food.id}", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, activity_edit::class.java).apply {
                putExtra("foodId", food.id)
                putExtra("productName", food.productName)
                putExtra("brand", food.brand)
                putExtra("category", food.category)
                putExtra("createDate", food.createDate)
                putExtra("endDate", food.endDate)   // 假設是 Int
                putExtra("quantityUnit", food.quantityUnit)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim()
                val results = FilterResults()

                results.values = if (query.isNullOrEmpty()) {
                    originalList
                } else {
                    originalList.filter {
                        it.productName.lowercase().contains(query) ||
                                it.brand.lowercase().contains(query)
                    }
                }

                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = ArrayList(results?.values as List<Food>)
                notifyDataSetChanged()
            }
        }
    }

}

