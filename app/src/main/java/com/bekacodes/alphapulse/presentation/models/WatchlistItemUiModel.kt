package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class WatchlistItemUiModel(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val dayChangePercent: Double,
    val sparkline: List<Float>
) {
    val isRising: Boolean get() = dayChangePercent >= 0.0
}