package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LanguageMode
import com.example.data.model.UpiProvider

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val amount: Double,
    val formattedAmount: String,
    val provider: String, // String representation of UpiProvider
    val rawText: String,
    val language: String, // LanguageMode code e.g. "en", "hi", "gu"
    val timestamp: Long = System.currentTimeMillis()
)
