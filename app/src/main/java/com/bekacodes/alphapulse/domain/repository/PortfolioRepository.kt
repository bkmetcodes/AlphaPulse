package com.bekacodes.alphapulse.domain.repository

import com.bekacodes.alphapulse.data.local.PortfolioEntity
import com.bekacodes.alphapulse.data.local.PositionEntity
import com.bekacodes.alphapulse.data.local.WatchlistEntity
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun observePortfolios(): Flow<List<PortfolioEntity>>
    suspend fun getPortfoliosSnapshot(): List<PortfolioEntity>
    suspend fun insertPortfolio(portfolio: PortfolioEntity)

    fun observeAllPositions(): Flow<List<PositionEntity>>
    fun observePositionsByPortfolio(portfolioId: String): Flow<List<PositionEntity>>
    suspend fun getPosition(portfolioId: String, symbol: String): PositionEntity?
    suspend fun insertPosition(position: PositionEntity): Long
    suspend fun deletePosition(portfolioId: String, symbol: String): Int
    suspend fun upsertPositionWithWeightedAvg(
        portfolioId: String,
        symbol: String,
        addedShares: Double,
        purchasePrice: Double
    ): PositionEntity

    fun observeWatchlist(): Flow<List<WatchlistEntity>>
    suspend fun insertWatchlistTicker(item: WatchlistEntity)
    suspend fun removeWatchlistTicker(symbol: String): Int
}