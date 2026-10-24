package com.bekacodes.alphapulse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bekacodes.alphapulse.data.local.PortfolioEntity
import com.bekacodes.alphapulse.data.local.WatchlistEntity
import com.bekacodes.alphapulse.data.market.MarketDataEngine
import com.bekacodes.alphapulse.domain.ParsedPortfolioCommand
import com.bekacodes.alphapulse.domain.PortfolioAiAgent
import com.bekacodes.alphapulse.domain.repository.PortfolioRepository
import com.bekacodes.alphapulse.presentation.models.AlphaPulseIntent
import com.bekacodes.alphapulse.presentation.models.AlphaPulseUiState
import com.bekacodes.alphapulse.presentation.models.HoldingsTab
import com.bekacodes.alphapulse.presentation.models.PortfolioTabUiModel
import com.bekacodes.alphapulse.presentation.models.PositionUiModel
import com.bekacodes.alphapulse.presentation.models.Timeframe
import com.bekacodes.alphapulse.presentation.models.WatchlistItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlphaPulseViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val marketEngine: MarketDataEngine,
    private val aiAgent: PortfolioAiAgent
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlphaPulseUiState())
    val uiState: StateFlow<AlphaPulseUiState> = _uiState.asStateFlow()

    private val selectedPortfolioIdFlow = MutableStateFlow("all")
    private val selectedTimeframeFlow = MutableStateFlow(Timeframe.ONE_DAY)

    init {
        viewModelScope.launch {
            ensureDefaultPortfoliosExist()
        }

        viewModelScope.launch {
            combine(
                repository.observePortfolios(),
                repository.observeAllPositions(),
                repository.observeWatchlist(),
                selectedPortfolioIdFlow,
                selectedTimeframeFlow
            ) { portfolioEntities, allPositions, watchlistEntities, selectedPortfolioId, timeframe ->

                val portfolioTabs = listOf(PortfolioTabUiModel("all", "All Holdings")) +
                    portfolioEntities.map { PortfolioTabUiModel(it.id, it.name) }

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

            val parsedCommand = aiAgent.parseCommand(rawCommand)
            val summaries = mutableListOf<String>()

            suspend fun processCmd(cmd: ParsedPortfolioCommand) {
                when (cmd) {
                    is ParsedPortfolioCommand.AddPosition -> {
                        val currentSelected = selectedPortfolioIdFlow.value
                        val targetPortfolioId = if (cmd.targetPortfolioName != null) {
                            val existing = repository.getPortfoliosSnapshot().firstOrNull {
                                it.name.equals(cmd.targetPortfolioName, ignoreCase = true)
                            }
                            existing?.id ?: run {
                                val newId = "folio_${UUID.randomUUID().toString().take(6)}"
                                repository.insertPortfolio(PortfolioEntity(id = newId, name = cmd.targetPortfolioName))
                                selectedPortfolioIdFlow.value = newId
                                newId
                            }
                        } else if (currentSelected == "all") {
                            repository.getPortfoliosSnapshot().firstOrNull()?.id ?: "semis"
                        } else {
                            currentSelected
                        }

                        val updated = repository.upsertPositionWithWeightedAvg(
                            portfolioId = targetPortfolioId,
                            symbol = cmd.symbol,
                            addedShares = cmd.shares,
                            purchasePrice = cmd.avgCost
                        )
                        summaries.add("Added ${cmd.shares} sh of ${updated.symbol} @ $${String.format("%,.2f", cmd.avgCost)}")
                    }

                    is ParsedPortfolioCommand.RemovePosition -> {
                        val deleted = repository.deletePosition(selectedPortfolioIdFlow.value, cmd.symbol)
                        if (deleted > 0) summaries.add("Removed ${cmd.symbol}") else summaries.add("${cmd.symbol} not found")
                    }

                    is ParsedPortfolioCommand.AddToWatchlist -> {
                        repository.insertWatchlistTicker(WatchlistEntity(symbol = cmd.symbol))
                        summaries.add("Added ${cmd.symbol} to Watchlist")
                    }

                    is ParsedPortfolioCommand.RemoveFromWatchlist -> {
                        repository.removeWatchlistTicker(cmd.symbol)
                        summaries.add("Removed ${cmd.symbol} from Watchlist")
                    }

                    is ParsedPortfolioCommand.CreatePortfolio -> {
                        val id = "folio_${UUID.randomUUID().toString().take(6)}"
                        repository.insertPortfolio(PortfolioEntity(id = id, name = cmd.name))
                        selectedPortfolioIdFlow.value = id
                        summaries.add("Created portfolio '${cmd.name}'")
                    }

                    is ParsedPortfolioCommand.Multiple -> {
                        cmd.commands.forEach { sub -> processCmd(sub) }
                    }

                    is ParsedPortfolioCommand.Unrecognized -> {
                        summaries.add(cmd.hint)
                    }
                }
            }

            processCmd(parsedCommand)

            _uiState.update {
                it.copy(
                    commandInput = "",
                    isProcessingAiCommand = false,
                    aiStatusBanner = summaries.joinToString(" • ")
                )
            }
        }
    }

    private fun createPortfolio(name: String) {
        viewModelScope.launch {
            val id = "folio_${UUID.randomUUID().toString().take(6)}"
            repository.insertPortfolio(PortfolioEntity(id = id, name = name))
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
        if (repository.getPortfoliosSnapshot().isEmpty()) {
            repository.insertPortfolio(PortfolioEntity(id = "semis", name = "Semis & AI", createdAt = 1L))
            repository.insertPortfolio(PortfolioEntity(id = "growth", name = "Growth", createdAt = 2L))
        }
    }
}