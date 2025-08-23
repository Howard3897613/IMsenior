package com.example.IMsenior

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.util.Log
import android.content.ContentValues.TAG
import android.content.Intent
import android.widget.ImageButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FoodAdapter(
    private val foodList: List<Food>,
    private val context: Context
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val brand: TextView = itemView.findViewById(R.id.tvBrand)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val createDate: TextView = itemView.findViewById(R.id.tvCreateDate)
        val endDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val quantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnClick: ImageButton = itemView.findViewById(R.id.btnClick)
        val btnedit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        val db = Firebase.firestore
        holder.name.text = food.productName
        holder.brand.text = food.brand
        holder.category.text = if (food.category == 1) "食材" else "食物"
        holder.createDate.text = food.createDate
        holder.endDate.text = food.endDate.toString()
        holder.quantity.text = food.quantityUnit

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

    override fun getItemCount(): Int = foodList.size
}
