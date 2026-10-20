package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class NewsArticleUiModel(
    val id: String,
    val source: String,
    val publishedAgo: String,
    val title: String,
    val url: String,
    val relatedSymbol: String,
    val symbolChangePercent: Double
)