package com.bekacodes.alphapulse.data.repository

import com.bekacodes.alphapulse.data.local.AlphaPulseDao
import com.bekacodes.alphapulse.data.local.PortfolioEntity
import com.bekacodes.alphapulse.data.local.PositionEntity
import com.bekacodes.alphapulse.data.local.WatchlistEntity
import com.bekacodes.alphapulse.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PortfolioRepositoryImpl @Inject constructor(
    private val dao: AlphaPulseDao
) : PortfolioRepository {

    override fun observePortfolios(): Flow<List<PortfolioEntity>> =
        dao.observePortfolios()

    override suspend fun getPortfoliosSnapshot(): List<PortfolioEntity> =
        dao.getPortfoliosSnapshot()

    override suspend fun insertPortfolio(portfolio: PortfolioEntity) =
        dao.insertPortfolio(portfolio)

    override fun observeAllPositions(): Flow<List<PositionEntity>> =
        dao.observeAllPositions()

    override fun observePositionsByPortfolio(portfolioId: String): Flow<List<PositionEntity>> =
        dao.observePositionsByPortfolio(portfolioId)

    override suspend fun getPosition(portfolioId: String, symbol: String): PositionEntity? =
        dao.getPosition(portfolioId, symbol)

    override suspend fun insertPosition(position: PositionEntity): Long =
        dao.insertPosition(position)

    override suspend fun deletePosition(portfolioId: String, symbol: String): Int =
        dao.deletePosition(portfolioId, symbol)

    override suspend fun upsertPositionWithWeightedAvg(
        portfolioId: String,
        symbol: String,
        addedShares: Double,
        purchasePrice: Double
    ): PositionEntity =
        dao.upsertPositionWithWeightedAvg(portfolioId, symbol, addedShares, purchasePrice)

    override fun observeWatchlist(): Flow<List<WatchlistEntity>> =
        dao.observeWatchlist()

    override suspend fun insertWatchlistTicker(item: WatchlistEntity) =
        dao.insertWatchlistTicker(item)

    override suspend fun removeWatchlistTicker(symbol: String): Int =
        dao.removeWatchlistTicker(symbol)
}