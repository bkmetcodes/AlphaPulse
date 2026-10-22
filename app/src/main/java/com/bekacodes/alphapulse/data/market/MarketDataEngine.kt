package com.bekacodes.alphapulse.data.market

import com.bekacodes.alphapulse.presentation.models.ChartPoint
import com.bekacodes.alphapulse.presentation.models.NewsArticleUiModel
import com.bekacodes.alphapulse.presentation.models.PositionUiModel
import com.bekacodes.alphapulse.presentation.models.Timeframe
import kotlin.math.sin

class MarketDataEngine {

    private val referenceQuotes = mapOf(
        "SNDK" to TickerQuote("SNDK", "SanDisk Corp", 1485.00, 6.07, listOf(.2f, .4f, .3f, .7f, .5f, .9f, .7f, 1f)),
        "MU" to TickerQuote("MU", "Micron Technology", 124.50, 14.74, listOf(.3f, .2f, .5f, .4f, .7f, .6f, .9f, .8f, 1f)),
        "RKLB" to TickerQuote("RKLB", "Rocket Lab USA", 24.60, -1.85, listOf(1f, .8f, .9f, .55f, .7f, .35f, .5f, .2f, .35f, .1f)),
        "NVDA" to TickerQuote("NVDA", "NVIDIA Corp", 138.40, 2.45, listOf(.25f, .35f, .3f, .55f, .6f, .75f, .85f, 1f)),
        "NBIS" to TickerQuote("NBIS", "Nebius Group", 34.20, 5.12, listOf(.2f, .3f, .45f, .4f, .65f, .7f, .85f, 1f)),
        "ONTO" to TickerQuote("ONTO", "Onto Innovation", 212.10, -0.84, listOf(.9f, .85f, .8f, .7f, .75f, .6f, .55f, .5f)),
        "INOD" to TickerQuote("INOD", "Innodata Inc", 44.80, 8.30, listOf(.15f, .3f, .25f, .5f, .65f, .8f, .9f, 1f)),
        "AAPL" to TickerQuote("AAPL", "Apple Inc", 232.15, 1.15, listOf(.4f, .45f, .5f, .48f, .6f, .65f, .7f, .75f)),
        "TSLA" to TickerQuote("TSLA", "Tesla Inc", 258.90, -2.10, listOf(.95f, .8f, .85f, .7f, .6f, .5f, .45f, .4f))
    )

    fun getQuote(symbol: String, fallbackCostBasis: Double? = null): TickerQuote {
        val clean = symbol.trim().uppercase()
        referenceQuotes[clean]?.let { return it }

        // Deterministic quote for any custom ticker entered by the user
        val seed = clean.hashCode()
        val deltaPct = ((seed % 900) / 100.0).let { if (it == 0.0) 3.25 else it }
        val basePrice = fallbackCostBasis?.takeIf { it > 0 } ?: 100.0
        val simulatedCurrent = (basePrice * (1.0 + (deltaPct / 100.0))).coerceAtLeast(1.0)
        val rising = deltaPct >= 0.0
        val spark = if (rising) {
            listOf(.2f, .35f, .3f, .55f, .5f, .75f, .85f, 1f)
        } else {
            listOf(1f, .85f, .9f, .65f, .7f, .45f, .35f, .2f)
        }

        return TickerQuote(
            symbol = clean,
            companyName = "$clean Holdings",
            currentPrice = simulatedCurrent,
            dayChangePercent = deltaPct,
            sparkline = spark
        )
    }

    /**
     * Generates a 28-point Google Finance historical curve scaled to the active portfolio's value
     * and the selected timeframe (1D, 5D, 1M, 6M, YTD, 1Y, MAX).
     */
    fun buildPortfolioCurve(
        positions: List<PositionUiModel>,
        timeframe: Timeframe
    ): Pair<Double, List<ChartPoint>> {
        val currentTotal = positions.sumOf { it.marketValue }
        if (currentTotal <= 0.0) {
            val flatLabels = labelsForTimeframe(timeframe)
            return 0.0 to flatLabels.map { ChartPoint(it, 0.0) }
        }

        val totalCost = positions.sumOf { it.totalCostBasis }
        val timeframeMultiplier = when (timeframe) {
            Timeframe.ONE_DAY -> 0.35
            Timeframe.FIVE_DAYS -> 0.60
            Timeframe.ONE_MONTH -> 1.00
            Timeframe.SIX_MONTHS -> 1.45
            Timeframe.YTD -> 1.65
            Timeframe.ONE_YEAR -> 2.10
            Timeframe.MAX -> 2.60
        }

        val rawDelta = (currentTotal - totalCost) * timeframeMultiplier
        val startBaseline = (currentTotal - rawDelta).coerceAtLeast(currentTotal * 0.4)

        val labels = labelsForTimeframe(timeframe)
        val lastIdx = labels.lastIndex.coerceAtLeast(1)

        val points = labels.mapIndexed { index, label ->
            val progress = index.toDouble() / lastIdx
            if (index == lastIdx) {
                ChartPoint(label, currentTotal)
            } else {
                // Smooth organic market curve between startBaseline and currentTotal
                val wave = sin(index * 0.85 + timeframe.ordinal) * (currentTotal * 0.012)
                val interpolated = startBaseline + (currentTotal - startBaseline) * progress + wave
                ChartPoint(label, interpolated.coerceAtLeast(0.01))
            }
        }

        return startBaseline to points
    }

    private fun labelsForTimeframe(timeframe: Timeframe): List<String> = when (timeframe) {
        Timeframe.ONE_DAY -> listOf(
            "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM",
            "12:30 PM", "1:00 PM", "1:30 PM", "2:00 PM", "2:15 PM", "2:45 PM",
            "3:15 PM", "3:45 PM", "4:00 PM"
        )
        Timeframe.FIVE_DAYS -> listOf(
            "Mon 10AM", "Mon 2PM", "Tue 10AM", "Tue 2PM", "Wed 10AM", "Wed 2PM",
            "Thu 10AM", "Thu 2PM", "Fri 10AM", "Fri 1PM", "Fri 4PM"
        )
        Timeframe.ONE_MONTH -> (1..15).map { "Oct ${it * 2}" }
        Timeframe.SIX_MONTHS -> listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct 1", "Oct 10", "Today")
        Timeframe.YTD -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Today")
        Timeframe.ONE_YEAR -> listOf("Nov '25", "Jan", "Mar", "May", "Jul", "Sep", "Today")
        Timeframe.MAX -> listOf("2022", "2023", "2024", "2025", "H1 '26", "Q3 '26", "Today")
    }

    fun getCuratedNews(activeSymbols: Set<String>): List<NewsArticleUiModel> {
        val baseNews = listOf(
            NewsArticleUiModel(
                id = "news_sndk",
                source = "Yahoo Finance",
                publishedAgo = "2h ago",
                title = "Enterprise NAND & HBM Demand Lifts Q4 Semiconductor Storage Guidance",
                url = "https://finance.yahoo.com/quote/SNDK/",
                relatedSymbol = "SNDK",
                symbolChangePercent = 6.07
            ),
            NewsArticleUiModel(
                id = "news_mu",
                source = "Yahoo Finance",
                publishedAgo = "4h ago",
                title = "High-Bandwidth Memory HBM3E Supply Sold Out Through Next Calendar Year",
                url = "https://finance.yahoo.com/quote/MU/",
                relatedSymbol = "MU",
                symbolChangePercent = 14.74
            ),
            NewsArticleUiModel(
                id = "news_rklb",
                source = "Yahoo Finance",
                publishedAgo = "6h ago",
                title = "Rocket Lab Accelerates Electron Launch Cadence and Neutron Pad Testing",
                url = "https://finance.yahoo.com/quote/RKLB/",
                relatedSymbol = "RKLB",
                symbolChangePercent = -1.85
            ),
            NewsArticleUiModel(
                id = "news_nvda",
                source = "Yahoo Finance",
                publishedAgo = "8h ago",
                title = "Hyperscaler AI Capex Cycle Continues to Drive Next-Gen GPU Rack Deployments",
                url = "https://finance.yahoo.com/quote/NVDA/",
                relatedSymbol = "NVDA",
                symbolChangePercent = 2.45
            )
        )

        if (activeSymbols.isEmpty()) return baseNews

        val matched = baseNews.filter { it.relatedSymbol in activeSymbols }.toMutableList()

        // Generate a live news card for any custom ticker the user added that isn't in baseNews
        activeSymbols.forEach { symbol ->
            if (matched.none { it.relatedSymbol == symbol }) {
                val quote = getQuote(symbol)
                matched.add(
                    0,
                    NewsArticleUiModel(
                        id = "news_${symbol.lowercase()}",
                        source = "MarketWatch",
                        publishedAgo = "1h ago",
                        title = "$symbol Institutional Volume & Technical Momentum Analysis",
                        url = "https://finance.yahoo.com/quote/$symbol/",
                        relatedSymbol = symbol,
                        symbolChangePercent = quote.dayChangePercent
                    )
                )
            }
        }

        return matched
    }
}