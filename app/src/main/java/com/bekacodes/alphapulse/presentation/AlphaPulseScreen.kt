package com.bekacodes.alphapulse.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.alphapulse.presentation.components.CommandBar
import com.bekacodes.alphapulse.presentation.components.EmptyListPlaceholder
import com.bekacodes.alphapulse.presentation.components.NewPortfolioDialog
import com.bekacodes.alphapulse.presentation.components.NewsCard
import com.bekacodes.alphapulse.presentation.components.Pill
import com.bekacodes.alphapulse.presentation.components.PortfolioChart
import com.bekacodes.alphapulse.presentation.components.PortfolioChip
import com.bekacodes.alphapulse.presentation.components.PositionRow
import com.bekacodes.alphapulse.presentation.components.TabHeaderItem
import com.bekacodes.alphapulse.presentation.components.TimeframeChip
import com.bekacodes.alphapulse.presentation.components.WatchlistRow
import com.bekacodes.alphapulse.presentation.models.AlphaPulseIntent
import com.bekacodes.alphapulse.presentation.models.AlphaPulseUiState
import com.bekacodes.alphapulse.presentation.models.ChartPoint
import com.bekacodes.alphapulse.presentation.models.HoldingsTab
import com.bekacodes.alphapulse.presentation.models.NewsArticleUiModel
import com.bekacodes.alphapulse.presentation.models.PositionUiModel
import com.bekacodes.alphapulse.presentation.models.Timeframe
import com.bekacodes.alphapulse.ui.theme.Background
import com.bekacodes.alphapulse.ui.theme.Border
import com.bekacodes.alphapulse.ui.theme.Primary
import com.bekacodes.alphapulse.ui.theme.SurfaceColor
import com.bekacodes.alphapulse.ui.theme.TextPrimary
import com.bekacodes.alphapulse.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun AlphaPulseScreen(
    uiState: AlphaPulseUiState,
    onIntent: (AlphaPulseIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        bottomBar = {
            CommandBar(
                inputText = uiState.commandInput,
                isProcessing = uiState.isProcessingAiCommand,
                onInputChanged = { onIntent(AlphaPulseIntent.UpdateCommandInput(it)) },
                onSend = { onIntent(AlphaPulseIntent.SubmitAiCommand) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AlphaPulse ✦",
                    color = TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onIntent(AlphaPulseIntent.ToggleNewPortfolioDialog(true)) },
                    shape = CircleShape,
                    color = SurfaceColor,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = "+ New Portfolio",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. AI Execution Toast Banner (Appears when AI updates portfolio)
            AnimatedVisibility(visible = uiState.aiStatusBanner != null) {
                uiState.aiStatusBanner?.let { bannerText ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { onIntent(AlphaPulseIntent.DismissAiBanner) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF191D38),
                        border = BorderStroke(1.dp, Primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("✦", color = Primary, fontSize = 16.sp)
                            Text(
                                text = bannerText,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text("✕", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. Portfolio Switcher Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.portfolios.forEach { portfolio ->
                    PortfolioChip(
                        label = portfolio.name,
                        selected = uiState.selectedPortfolioId == portfolio.id && uiState.selectedTab == HoldingsTab.POSITIONS,
                        onClick = {
                            onIntent(AlphaPulseIntent.SelectHoldingsTab(HoldingsTab.POSITIONS))
                            onIntent(AlphaPulseIntent.SelectPortfolio(portfolio.id))
                        }
                    )
                }
                PortfolioChip(
                    label = "★ Watchlist",
                    selected = uiState.selectedTab == HoldingsTab.WATCHLIST,
                    onClick = { onIntent(AlphaPulseIntent.SelectHoldingsTab(HoldingsTab.WATCHLIST)) }
                )
            }

            // 4. Google Finance Hero Header & Touch-Scrubbing Chart
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = formatCurrency(uiState.displayedBalance),
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sign = if (uiState.isPortfolioPositive) "+↑ " else "-↓ "
                    val pct = String.format("%.2f%%", kotlin.math.abs(uiState.periodChangePercent))
                    Pill(
                        text = "$sign${formatCurrency(abs(uiState.periodChangeDollar))} ($pct)",
                        positive = uiState.isPortfolioPositive
                    )
                    Text(
                        text = uiState.scrubbedPoint?.timestampLabel ?: "Today · Live",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                PortfolioChart(
                    points = uiState.chartPoints,
                    previousClose = uiState.previousCloseBalance,
                    scrubbedPoint = uiState.scrubbedPoint,
                    isPositive = uiState.isPortfolioPositive,
                    onScrub = { onIntent(AlphaPulseIntent.ScrubChart(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Timeframe.entries.forEach { tf ->
                        TimeframeChip(
                            label = tf.label,
                            selected = uiState.selectedTimeframe == tf,
                            onClick = { onIntent(AlphaPulseIntent.SelectTimeframe(tf)) }
                        )
                    }
                }
            }

            // 5. Positions & Watchlist Section
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    TabHeaderItem(
                        title = "Positions (${uiState.positions.size})",
                        selected = uiState.selectedTab == HoldingsTab.POSITIONS,
                        onClick = { onIntent(AlphaPulseIntent.SelectHoldingsTab(HoldingsTab.POSITIONS)) }
                    )
                    TabHeaderItem(
                        title = "Watchlist (${uiState.watchlist.size})",
                        selected = uiState.selectedTab == HoldingsTab.WATCHLIST,
                        onClick = { onIntent(AlphaPulseIntent.SelectHoldingsTab(HoldingsTab.WATCHLIST)) }
                    )
                }

                if (uiState.selectedTab == HoldingsTab.POSITIONS) {
                    if (uiState.positions.isEmpty()) {
                        EmptyListPlaceholder("No positions in this portfolio yet.\nTry typing: \"Add 5 shares of SNDK with avg. cost of $1400\"")
                    } else {
                        uiState.positions.forEach { PositionRow(it) }
                    }
                } else {
                    if (uiState.watchlist.isEmpty()) {
                        EmptyListPlaceholder("Your watchlist is empty.\nTry typing: \"Watch NVDA\" in the AI command bar.")
                    } else {
                        uiState.watchlist.forEach { WatchlistRow(it) }
                    }
                }
            }

            // 6. In the News Section
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    text = "In the news",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.news.forEach { article ->
                        NewsCard(
                            article = article,
                            onClick = { onIntent(AlphaPulseIntent.OpenNewsArticle(article)) }
                        )
                    }
                }
            }
        }

        // "+ New Portfolio" Creation Dialog
        if (uiState.isNewPortfolioDialogOpen) {
            NewPortfolioDialog(
                onDismiss = { onIntent(AlphaPulseIntent.ToggleNewPortfolioDialog(false)) },
                onCreate = { name -> onIntent(AlphaPulseIntent.CreatePortfolio(name)) }
            )
        }
    }

    // Full-Screen In-App News Article WebView Overlay
    AnimatedVisibility(
        visible = uiState.activeArticleWebView != null,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it / 4 }) +
                androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 4 }) +
                androidx.compose.animation.fadeOut()
    ) {
        uiState.activeArticleWebView?.let { article ->
            NewsArticleWebViewScreen(
                article = article,
                onClose = { onIntent(AlphaPulseIntent.OpenNewsArticle(null)) }
            )
        }
    }
}

fun formatCurrency(value: Double): String =
    "$%,.2f".format(value)

@Preview
@Composable
private fun AlphaPulseScreenPreview() {
    val samplePoints = listOf(
        ChartPoint("9:30 AM", 47500.0),
        ChartPoint("10:30 AM", 47820.0),
        ChartPoint("11:30 AM", 47690.0),
        ChartPoint("12:30 PM", 48210.0),
        ChartPoint("1:30 PM", 48640.0),
        ChartPoint("2:15 PM", 49110.0),
        ChartPoint("3:15 PM", 48790.0),
        ChartPoint("4:00 PM", 48920.50)
    )
    val previewState = AlphaPulseUiState(
        totalBalance = 48920.50,
        previousCloseBalance = 47499.70,
        chartPoints = samplePoints,
        positions = listOf(
            PositionUiModel(
                "1",
                "semis",
                "SNDK",
                5.0,
                1400.0,
                1485.0,
                6.07,
                listOf(.2f, .4f, .3f, .7f, .5f, .9f, .7f, 1f)
            ),
            PositionUiModel(
                "2",
                "semis",
                "MU",
                40.0,
                108.50,
                124.50,
                14.74,
                listOf(.3f, .2f, .5f, .4f, .7f, .6f, .9f, .8f, 1f)
            ),
            PositionUiModel(
                "3",
                "growth",
                "RKLB",
                150.0,
                25.06,
                24.60,
                -1.85,
                listOf(1f, .8f, .9f, .55f, .7f, .35f, .5f, .2f, .35f, .1f)
            )
        ),
        news = listOf(
            NewsArticleUiModel(
                id = "n1",
                source = "Reuters",
                publishedAgo = "2h ago",
                title = "Enterprise NAND & HBM Demand Lifts Q4 Semiconductor Guidance",
                url = "https://www.reuters.com/technology/",
                relatedSymbol = "SNDK",
                symbolChangePercent = 6.0
            )
        )
    )
    MaterialTheme {
        AlphaPulseScreen(uiState = previewState, onIntent = {})
    }
}