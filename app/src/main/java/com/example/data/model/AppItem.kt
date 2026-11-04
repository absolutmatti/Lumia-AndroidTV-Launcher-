package com.example.data.model

data class CardColorResult(
    val backgroundColor: Long, // 0xAARRGGBB
    val textColor: Long        // 0xAARRGGBB
)

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val hasBanner: Boolean,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
    val dynamicColors: CardColorResult
)
