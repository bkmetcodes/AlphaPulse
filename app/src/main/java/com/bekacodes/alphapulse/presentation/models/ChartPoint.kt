package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class ChartPoint(
    val timestampLabel: String,
    val value: Double
)