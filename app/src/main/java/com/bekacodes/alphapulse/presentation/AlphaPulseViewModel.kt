package com.bekacodes.alphapulse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bekacodes.alphapulse.data.local.AlphaPulseDao
import com.bekacodes.alphapulse.data.local.PortfolioEntity
import com.bekacodes.alphapulse.data.local.WatchlistEntity
import com.bekacodes.alphapulse.data.market.MarketDataEngine
import com.bekacodes.alphapulse.domain.ParsedPortfolioCommand
import com.bekacodes.alphapulse.domain.PortfolioAiAgent
import com.bekacodes.alphapulse.presentation.models.AlphaPulseIntent
import com.bekacodes.alphapulse.presentation.models.AlphaPulseUiState
import com.bekacodes.alphapulse.presentation.models.HoldingsTab
import com.bekacodes.alphapulse.presentation.models.PortfolioTabUiModel
import com.bekacodes.alphapulse.presentation.models.PositionUiModel
import com.bekacodes.alphapulse.presentation.models.Timeframe
import com.bekacodes.alphapulse.presentation.models.WatchlistItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AlphaPulseViewModel(
    private val dao: AlphaPulseDao,
    private val marketEngine: MarketDataEngine = MarketDataEngine(),
    private val aiAgent: PortfolioAiAgent = PortfolioAiAgent()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlphaPulseUiState())
    val uiState: StateFlow<AlphaPulseUiState> = _uiState.asStateFlow()

    private val selectedPortfolioIdFlow = MutableStateFlow("all")
    private val selectedTimeframeFlow = MutableStateFlow(Timeframe.ONE_DAY)

    init {
        viewModelScope.launch {
            ensureDefaultPortfoliosExist()
        }

        // Reactive pipeline combining Room SQLite tables + active portfolio filter + selected timeframe
        viewModelScope.launch {
            combine(
                dao.observePortfolios(),
                dao.observeAllPositions(),
                dao.observeWatchlist(),
                selectedPortfolioIdFlow,
                selectedTimeframeFlow
            ) { portfolioEntities, allPositions, watchlistEntities, selectedPortfolioId, timeframe ->

                val portfolioTabs = listOf(PortfolioTabUiModel("all", "All Holdings")) +
                    portfolioEntities.map { PortfolioTabUiModel(it.id, it.name) }

                // Filter positions by selected portfolio (or aggregate by symbol if "All Holdings" is selected)
                val filteredEntities = if (selectedPortfolioId == "all") {
                    allPositions
                        .groupBy { it.symbol.uppercase() }
                        .map { (symbol, rows) ->
                            val totalShares = rows.sumOf { it.shares }
                            val weightedAvg = rows.sumOf { it.shares * it.avgCost } / totalShares.coerceAtLeast(0.0001)
                            rows.first().copy(shares = totalShares, avgCost = weightedAvg)
                        }
                } else {
                    allPositions.filter { it.portfolioId == selectedPortfolioId }
                }

                val positionUiModels = filteredEntities.map { entity ->
                    val quote = marketEngine.getQuote(entity.symbol, entity.avgCost)
                    PositionUiModel(
                        id = "${entity.portfolioId}_${entity.symbol}",
                        portfolioId = entity.portfolioId,
                        symbol = entity.symbol,
                        shares = entity.shares,
                        avgCost = entity.avgCost,
                        currentPrice = quote.currentPrice,
                        dayChangePercent = quote.dayChangePercent,
                        sparkline = quote.sparkline
                    )
                }

                val watchlistUiModels = watchlistEntities.map { item ->
                    val quote = marketEngine.getQuote(item.symbol)
                    WatchlistItemUiModel(
                        symbol = quote.symbol,
                        companyName = quote.companyName,
                        currentPrice = quote.currentPrice,
                        dayChangePercent = quote.dayChangePercent,
                        sparkline = quote.sparkline
                    )
                }

                val totalBalance = positionUiModels.sumOf { it.marketValue }
                val (prevClose, curvePoints) = marketEngine.buildPortfolioCurve(positionUiModels, timeframe)

                val activeSymbols = (positionUiModels.map { it.symbol } + watchlistUiModels.map { it.symbol }).toSet()
                val curatedNews = marketEngine.getCuratedNews(activeSymbols)

                _uiState.value.copy(
                    portfolios = portfolioTabs,
                    selectedPortfolioId = selectedPortfolioId,
                    selectedTimeframe = timeframe,
                    totalBalance = totalBalance,
                    previousCloseBalance = prevClose,
                    chartPoints = curvePoints,
                    positions = positionUiModels,
                    watchlist = watchlistUiModels,
                    news = curatedNews
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onIntent(intent: AlphaPulseIntent) {
        when (intent) {
            is AlphaPulseIntent.SelectPortfolio -> {
                selectedPortfolioIdFlow.value = intent.portfolioId
                _uiState.update { it.copy(scrubbedPoint = null) }
            }
            is AlphaPulseIntent.SelectTimeframe -> {
                selectedTimeframeFlow.value = intent.timeframe
                _uiState.update { it.copy(scrubbedPoint = null) }
            }
            is AlphaPulseIntent.SelectHoldingsTab -> {
                _uiState.update { it.copy(selectedTab = intent.tab) }
            }
            is AlphaPulseIntent.ScrubChart -> {
                _uiState.update { it.copy(scrubbedPoint = intent.point) }
            }
            is AlphaPulseIntent.UpdateCommandInput -> {
                _uiState.update { it.copy(commandInput = intent.text) }
            }
            is AlphaPulseIntent.DismissAiBanner -> {
                _uiState.update { it.copy(aiStatusBanner = null) }
            }
            is AlphaPulseIntent.ToggleNewPortfolioDialog -> {
                _uiState.update { it.copy(isNewPortfolioDialogOpen = intent.open) }
            }
            is AlphaPulseIntent.CreatePortfolio -> {
                createPortfolio(intent.name)
            }
            is AlphaPulseIntent.SubmitAiCommand -> {
                executeAiCommand(_uiState.value.commandInput)
            }
            is AlphaPulseIntent.OpenNewsArticle -> {
                _uiState.update { it.copy(activeArticleWebView = intent.article) }
            }
        }
    }

    private fun executeAiCommand(rawCommand: String) {
        if (rawCommand.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAiCommand = true) }

            when (val parsed = aiAgent.parseCommand(rawCommand)) {
                is ParsedPortfolioCommand.AddPosition -> {
                    // Add to currently selected portfolio (or first user portfolio if on "All Holdings")
                    val currentSelected = selectedPortfolioIdFlow.value
                    val targetPortfolioId = if (currentSelected == "all") {
                        dao.getPortfoliosSnapshot().firstOrNull()?.id ?: "semis"
                    } else {
                        currentSelected
                    }

                    val updated = dao.upsertPositionWithWeightedAvg(
                        portfolioId = targetPortfolioId,
                        symbol = parsed.symbol,
                        addedShares = parsed.shares,
                        purchasePrice = parsed.avgCost
                    )

                    _uiState.update {
                        it.copy(
                            commandInput = "",
                            isProcessingAiCommand = false,
                            selectedTab = HoldingsTab.POSITIONS,
                            aiStatusBanner = "Added ${parsed.shares} sh of ${updated.symbol} @ $${String.format("%,.2f", parsed.avgCost)} (Avg $${String.format("%,.2f", updated.avgCost)})"
                        )
                    }
                }

                is ParsedPortfolioCommand.RemovePosition -> {
                    val deleted = dao.deletePosition(selectedPortfolioIdFlow.value, parsed.symbol)
                    _uiState.update {
                        it.copy(
                            commandInput = "",
                            isProcessingAiCommand = false,
                            aiStatusBanner = if (deleted > 0) "Removed ${parsed.symbol} from portfolio" else "${parsed.symbol} not found in current portfolio"
                        )
                    }
                }

                is ParsedPortfolioCommand.AddToWatchlist -> {
                    dao.insertWatchlistTicker(WatchlistEntity(symbol = parsed.symbol))
                    _uiState.update {
                        it.copy(
                            commandInput = "",
                            isProcessingAiCommand = false,
                            selectedTab = HoldingsTab.WATCHLIST,
                            aiStatusBanner = "Added ${parsed.symbol} to ★ Watchlist"
                        )
                    }
                }

                is ParsedPortfolioCommand.RemoveFromWatchlist -> {
                    dao.removeWatchlistTicker(parsed.symbol)
                    _uiState.update {
                        it.copy(
                            commandInput = "",
                            isProcessingAiCommand = false,
                            aiStatusBanner = "Removed ${parsed.symbol} from ★ Watchlist"
                        )
                    }
                }

                is ParsedPortfolioCommand.CreatePortfolio -> {
                    val id = "folio_${UUID.randomUUID().toString().take(6)}"
                    dao.insertPortfolio(PortfolioEntity(id = id, name = parsed.name))
                    selectedPortfolioIdFlow.value = id
                    _uiState.update {
                        it.copy(
                            commandInput = "",
                            isProcessingAiCommand = false,
                            aiStatusBanner = "Created and switched to portfolio '${parsed.name}'"
                        )
                    }
                }

                is ParsedPortfolioCommand.Unrecognized -> {
                    _uiState.update {
                        it.copy(
                            isProcessingAiCommand = false,
                            aiStatusBanner = parsed.hint
                        )
                    }
                }
            }
        }
    }

    private fun createPortfolio(name: String) {
        viewModelScope.launch {
            val id = "folio_${UUID.randomUUID().toString().take(6)}"
            dao.insertPortfolio(PortfolioEntity(id = id, name = name))
            selectedPortfolioIdFlow.value = id
            _uiState.update {
                it.copy(
                    isNewPortfolioDialogOpen = false,
                    selectedTab = HoldingsTab.POSITIONS,
                    aiStatusBanner = "Created portfolio '$name'"
                )
            }
        }
    }

    private suspend fun ensureDefaultPortfoliosExist() {
        if (dao.getPortfoliosSnapshot().isEmpty()) {
            dao.insertPortfolio(PortfolioEntity(id = "semis", name = "Semis & AI", createdAt = 1L))
            dao.insertPortfolio(PortfolioEntity(id = "growth", name = "Growth", createdAt = 2L))
        }
    }

    companion object {
        fun provideFactory(dao: AlphaPulseDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlphaPulseViewModel(dao) as T
                }
            }
    }
}