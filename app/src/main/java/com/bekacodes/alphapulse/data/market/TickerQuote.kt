package com.bekacodes.alphapulse.data.market

data class TickerQuote(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val dayChangePercent: Double,
    val sparkline: List<Float>
)