package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class PositionUiModel(
    val id: String,
    val portfolioId: String,
    val symbol: String,
    val shares: Double,
    val avgCost: Double,
    val currentPrice: Double,
    val dayChangePercent: Double,
    val sparkline: List<Float>
) {
    val marketValue: Double get() = shares * currentPrice
    val totalCostBasis: Double get() = shares * avgCost
    val totalReturnDollar: Double get() = marketValue - totalCostBasis
    val totalReturnPercent: Double
        get() = if (totalCostBasis > 0) (totalReturnDollar / totalCostBasis) * 100.0 else 0.0
    val isRising: Boolean get() = totalReturnPercent >= 0.0
}