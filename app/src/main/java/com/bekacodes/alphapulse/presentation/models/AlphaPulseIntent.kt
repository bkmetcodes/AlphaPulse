package com.bekacodes.alphapulse.presentation.models

sealed interface AlphaPulseIntent {
    data class SelectPortfolio(val portfolioId: String) : AlphaPulseIntent
    data class SelectTimeframe(val timeframe: Timeframe) : AlphaPulseIntent
    data class SelectHoldingsTab(val tab: HoldingsTab) : AlphaPulseIntent
    data class ScrubChart(val point: ChartPoint?) : AlphaPulseIntent
    data class UpdateCommandInput(val text: String) : AlphaPulseIntent
    data object SubmitAiCommand : AlphaPulseIntent
    data object DismissAiBanner : AlphaPulseIntent
    data class ToggleNewPortfolioDialog(val open: Boolean) : AlphaPulseIntent
    data class CreatePortfolio(val name: String) : AlphaPulseIntent
    data class OpenNewsArticle(val article: NewsArticleUiModel?) : AlphaPulseIntent
}