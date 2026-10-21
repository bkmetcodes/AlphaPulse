package com.bekacodes.alphapulse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlphaPulseDao {
    @Query("SELECT * FROM portfolios ORDER BY createdAt ASC")
    fun observePortfolios(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolios ORDER BY createdAt ASC")
    suspend fun getPortfoliosSnapshot(): List<PortfolioEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity)

    @Query("SELECT * FROM positions ORDER BY symbol ASC")
    fun observeAllPositions(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE portfolioId = :portfolioId ORDER BY symbol ASC")
    fun observePositionsByPortfolio(portfolioId: String): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE portfolioId = :portfolioId AND UPPER(symbol) = UPPER(:symbol) LIMIT 1")
    suspend fun getPosition(portfolioId: String, symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity): Long

    @Query("DELETE FROM positions WHERE UPPER(symbol) = UPPER(:symbol) AND (:portfolioId = 'all' OR portfolioId = :portfolioId)")
    suspend fun deletePosition(portfolioId: String, symbol: String): Int

    @Transaction
    suspend fun upsertPositionWithWeightedAvg(
        portfolioId: String,
        symbol: String,
        addedShares: Double,
        purchasePrice: Double
    ): PositionEntity {
        val cleanSymbol = symbol.trim().uppercase()
        val existing = getPosition(portfolioId, cleanSymbol)
        val updated = if (existing == null) {
            PositionEntity(
                portfolioId = portfolioId,
                symbol = cleanSymbol,
                shares = addedShares,
                avgCost = purchasePrice
            )
        } else {
            val totalShares = existing.shares + addedShares
            val weightedAvg =
                ((existing.shares * existing.avgCost) + (addedShares * purchasePrice)) / totalShares
            existing.copy(
                shares = totalShares,
                avgCost = weightedAvg
            )
        }
        insertPosition(updated)
        return updated
    }

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistTicker(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE UPPER(symbol) = UPPER(:symbol)")
    suspend fun removeWatchlistTicker(symbol: String): Int
}