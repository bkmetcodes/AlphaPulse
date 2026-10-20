package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class PortfolioTabUiModel(
    val id: String,
    val name: String,
    val isWatchlistFilter: Boolean = false
)