package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_app")
data class FavoriteApp(
    @PrimaryKey val packageName: String,
    val position: Int // 0..5, continuous
)

@Entity(tableName = "hidden_app")
data class HiddenApp(
    @PrimaryKey val packageName: String
)
