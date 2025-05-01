package com.example.IMsenior

data class Food(
    val id: String,              //這是 Firestore 文件 ID
    val productName: String,
    val brand: String,
    val category: Int,
    val createDate: String,
    val endDate: Int,
    val quantityUnit: String
)
