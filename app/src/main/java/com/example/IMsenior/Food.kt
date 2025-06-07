package com.example.IMsenior

import com.google.firebase.firestore.PropertyName // Optional: if your Firestore field names differ

data class Food(
    // It's good practice for the ID to be mutable if you set it after fetching,
    // or ensure it's handled correctly during deserialization if it comes from the document itself.
    // If 'id' is purely the document ID from Firestore and not a field *within* the document data,
    // you might not even need it in the data class for Firestore's toObject() method,
    // as you often get the ID separately.
    // However, if you *do* store 'id' as a field in the document, it needs a default.
    val id: String = "", // Default value
    val productName: String = "", // Default value
    val brand: String = "", // Default value
    val category: Int = 0, // Default value
    val createDate: String = "", // Default value
    val endDate: Int = 0, // Default value (consider if String is more appropriate if it's a date)
    val quantityUnit: String = "" // Default value
) {
    // If you prefer not to add default values to the primary constructor,
    // or if some properties MUST be initialized via the primary constructor
    // without defaults, you would uncomment and use this secondary constructor:
    constructor() : this("", "", "", 0, "", 0, "")
}