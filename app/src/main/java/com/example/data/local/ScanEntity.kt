package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val rawFormat: String,
    val contentType: String, // URL, WIFI, PHONE, EMAIL, SMS, GEO, VCARD, TEXT, PRODUCT
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isSensitive: Boolean = false
)
