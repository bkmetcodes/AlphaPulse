package com.bekacodes.alphapulse.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "positions",
    indices = [Index(value = ["portfolioId", "symbol"], unique = true)]
)
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val portfolioId: String,
    val symbol: String,
    val shares: Double,
    val avgCost: Double
)