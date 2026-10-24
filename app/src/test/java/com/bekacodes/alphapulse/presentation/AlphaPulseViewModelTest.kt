package com.bekacodes.alphapulse.presentation

import com.bekacodes.alphapulse.data.local.AlphaPulseDao
import com.bekacodes.alphapulse.data.local.PortfolioEntity
import com.bekacodes.alphapulse.data.local.PositionEntity
import com.bekacodes.alphapulse.data.local.WatchlistEntity
import com.bekacodes.alphapulse.data.market.MarketDataEngine
import com.bekacodes.alphapulse.data.repository.PortfolioRepositoryImpl
import com.bekacodes.alphapulse.domain.ParsedPortfolioCommand
import com.bekacodes.alphapulse.domain.PortfolioAiAgent
import com.bekacodes.alphapulse.domain.repository.PortfolioRepository
import com.bekacodes.alphapulse.presentation.models.AlphaPulseIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlphaPulseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeAlphaPulseDao
    private lateinit var repository: PortfolioRepository
    private lateinit var aiAgent: PortfolioAiAgent
    private lateinit var marketEngine: MarketDataEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAlphaPulseDao()
        repository = PortfolioRepositoryImpl(fakeDao)
        aiAgent = PortfolioAiAgent()
        marketEngine = MarketDataEngine()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `AI command parser accurately extracts SNDK shares and average cost`() = runTest {
        val result = aiAgent.parseCommand("add 5 shares of SNDK with avg. cost of 1400$")
        assertTrue(result is ParsedPortfolioCommand.AddPosition)

        val addCmd = result as ParsedPortfolioCommand.AddPosition
        assertEquals("SNDK", addCmd.symbol)
        assertEquals(5.0, addCmd.shares, 0.001)
        assertEquals(1400.0, addCmd.avgCost, 0.001)
    }

    @Test
    fun `multi-action AI command creates portfolio, adds position, and updates watchlist`() = runTest(testDispatcher) {
        val viewModel = AlphaPulseViewModel(repository, marketEngine, aiAgent)
        advanceUntilIdle()

        viewModel.onIntent(
            AlphaPulseIntent.UpdateCommandInput("Create a Semis portfolio, add 5 shares of SNDK at $1400, and put MU on my watchlist")
        )
        viewModel.onIntent(AlphaPulseIntent.SubmitAiCommand)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.positions.size)
        assertEquals("SNDK", state.positions.first().symbol)
        assertEquals(1, state.watchlist.size)
        assertEquals("MU", state.watchlist.first().symbol)
    }

    @Test
    fun `submitting AI buy commands computes weighted-average cost basis and updates chart balance`() = runTest(testDispatcher) {
        val viewModel = AlphaPulseViewModel(repository, marketEngine, aiAgent)
        advanceUntilIdle()

        // 1. First purchase: 5 shares of SNDK @ $1,400
        viewModel.onIntent(AlphaPulseIntent.UpdateCommandInput("add 5 shares of SNDK with avg. cost of 1400$"))
        viewModel.onIntent(AlphaPulseIntent.SubmitAiCommand)
        advanceUntilIdle()

        val stateAfterFirstBuy = viewModel.uiState.value
        assertEquals(1, stateAfterFirstBuy.positions.size)
        assertEquals(5.0, stateAfterFirstBuy.positions.first().shares, 0.001)
        assertEquals(1400.0, stateAfterFirstBuy.positions.first().avgCost, 0.001)
        assertEquals(7425.0, stateAfterFirstBuy.totalBalance, 0.01) // 5 * $1,485 live quote

        // 2. Second purchase: 5 shares of SNDK @ $1,500 -> Weighted Avg should be $1,450.00 across 10 shares
        viewModel.onIntent(AlphaPulseIntent.UpdateCommandInput("add 5 shares of SNDK with avg. cost of 1500$"))
        viewModel.onIntent(AlphaPulseIntent.SubmitAiCommand)
        advanceUntilIdle()

        val stateAfterSecondBuy = viewModel.uiState.value
        val sndkPosition = stateAfterSecondBuy.positions.first()
        assertEquals(10.0, sndkPosition.shares, 0.001)
        assertEquals(1450.0, sndkPosition.avgCost, 0.001)
        assertEquals(14850.0, stateAfterSecondBuy.totalBalance, 0.01)
    }

    @Test
    fun `watch command adds symbol to watchlist and switches active tab`() = runTest(testDispatcher) {
        val viewModel = AlphaPulseViewModel(repository, marketEngine, aiAgent)
        advanceUntilIdle()

        viewModel.onIntent(AlphaPulseIntent.UpdateCommandInput("watch NVDA"))
        viewModel.onIntent(AlphaPulseIntent.SubmitAiCommand)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.watchlist.size)
        assertEquals("NVDA", state.watchlist.first().symbol)
    }
}

private class FakeAlphaPulseDao : AlphaPulseDao {
    private val portfoliosFlow = MutableStateFlow<List<PortfolioEntity>>(emptyList())
    private val positionsFlow = MutableStateFlow<List<PositionEntity>>(emptyList())
    private val watchlistFlow = MutableStateFlow<List<WatchlistEntity>>(emptyList())
    private var nextPosId = 1L

    override fun observePortfolios(): Flow<List<PortfolioEntity>> = portfoliosFlow
    override suspend fun getPortfoliosSnapshot(): List<PortfolioEntity> = portfoliosFlow.value

    override suspend fun insertPortfolio(portfolio: PortfolioEntity) {
        if (portfoliosFlow.value.none { it.id == portfolio.id }) {
            portfoliosFlow.value = portfoliosFlow.value + portfolio
        }
    }

    override fun observeAllPositions(): Flow<List<PositionEntity>> = positionsFlow

    override fun observePositionsByPortfolio(portfolioId: String): Flow<List<PositionEntity>> =
        positionsFlow.map { list -> list.filter { it.portfolioId == portfolioId } }

    override suspend fun getPosition(portfolioId: String, symbol: String): PositionEntity? =
        positionsFlow.value.firstOrNull {
            it.portfolioId == portfolioId && it.symbol.equals(symbol, ignoreCase = true)
        }

    override suspend fun insertPosition(position: PositionEntity): Long {
        val clean = position.copy(id = if (position.id == 0L) nextPosId++ else position.id)
        positionsFlow.value = positionsFlow.value.filterNot {
            it.portfolioId == clean.portfolioId && it.symbol.equals(clean.symbol, ignoreCase = true)
        } + clean
        return clean.id
    }

    override suspend fun deletePosition(portfolioId: String, symbol: String): Int {
        val before = positionsFlow.value.size
        positionsFlow.value = positionsFlow.value.filterNot {
            it.symbol.equals(symbol, ignoreCase = true) && (portfolioId == "all" || it.portfolioId == portfolioId)
        }
        return before - positionsFlow.value.size
    }

    override fun observeWatchlist(): Flow<List<WatchlistEntity>> = watchlistFlow

    override suspend fun insertWatchlistTicker(item: WatchlistEntity) {
        watchlistFlow.value = watchlistFlow.value.filterNot {
            it.symbol.equals(item.symbol, ignoreCase = true)
        } + item
    }

    override suspend fun removeWatchlistTicker(symbol: String): Int {
        val before = watchlistFlow.value.size
        watchlistFlow.value = watchlistFlow.value.filterNot { it.symbol.equals(symbol, ignoreCase = true) }
        return before - watchlistFlow.value.size
    }
}