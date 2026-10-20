package com.bekacodes.alphapulse.presentation.models

import androidx.compose.runtime.Immutable

@Immutable
data class AlphaPulseUiState(
    val portfolios: List<PortfolioTabUiModel> = listOf(
        PortfolioTabUiModel(id = "all", name = "All Holdings"),
        PortfolioTabUiModel(id = "semis", name = "Semis & AI"),
        PortfolioTabUiModel(id = "growth", name = "Growth")
    ),
    val selectedPortfolioId: String = "all",
    val selectedTimeframe: Timeframe = Timeframe.ONE_DAY,
    val selectedTab: HoldingsTab = HoldingsTab.POSITIONS,
    val totalBalance: Double = 16095.00,
    val previousCloseBalance: Double = 15210.00,
    val chartPoints: List<ChartPoint> = emptyList(),
    val scrubbedPoint: ChartPoint? = null,
    val positions: List<PositionUiModel> = emptyList(),
    val watchlist: List<WatchlistItemUiModel> = emptyList(),
    val news: List<NewsArticleUiModel> = emptyList(),
    val commandInput: String = "",
    val isProcessingAiCommand: Boolean = false,
    val aiStatusBanner: String? = null,
    val isNewPortfolioDialogOpen: Boolean = false,
    val activeArticleWebView: NewsArticleUiModel? = null
) {
    val displayedBalance: Double
        get() = scrubbedPoint?.value ?: totalBalance

    val periodChangeDollar: Double
        get() = displayedBalance - previousCloseBalance

    val periodChangePercent: Double
        get() = if (previousCloseBalance > 0) (periodChangeDollar / previousCloseBalance) * 100.0 else 0.0

    val isPortfolioPositive: Boolean
        get() = periodChangeDollar >= 0.0
}