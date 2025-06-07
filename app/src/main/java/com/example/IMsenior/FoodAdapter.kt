package com.example.IMsenior

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context

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
        val btnClick: Button = itemView.findViewById(R.id.btnClick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.name.text = food.productName
        holder.brand.text = food.brand
        holder.category.text = if (food.category == 1) "食材" else "食物"
        holder.createDate.text = food.createDate
        holder.endDate.text = food.endDate.toString()
        holder.quantity.text = food.quantityUnit

        holder.btnClick.setOnClickListener {
            Toast.makeText(context, "你點了 ${food.productName}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = foodList.size
}
