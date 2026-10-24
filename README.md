# AlphaPulse ✦ — AI-Integrated Stock Portfolio & Market Tracker

A modern Android stock portfolio manager and market tracker inspired by **Google Finance**, built with **Kotlin**, **Jetpack Compose (`Canvas`)**, **Clean Architecture (MVI)**, **Dagger Hilt**, **Room**, and an **AI Natural-Language Command Engine**.

<p align="center">
  <!-- Paste your GitHub issue-uploaded GIF URL here -->
  <img src="https://github.com/user-attachments/assets/YOUR-GIF-ID-HERE" alt="AlphaPulse Demo" width="320"/>
</p>

---

## ✨ Key Engineering Highlights

1. **Interactive Google Finance `Canvas` Chart (`PortfolioChart`)**
   - Custom 2D `Canvas` rendering a smooth price curve, vertical gradient fill (`Brush.verticalGradient`), and dashed `Prev Close` baseline that dynamically adapts to positive (Emerald `#10B981`) or negative (Coral `#F43F5E`) period returns.
   - Supports **horizontal touch-drag and tap scrubbing** (`detectHorizontalDragGestures`) with frame-accurate crosshair snapping, haptic tick feedback (`LocalHapticFeedback`), and real-time hero balance updates across 7 timeframes (`1D`, `5D`, `1M`, `6M`, `YTD`, `1Y`, `MAX`).

2. **AI Natural-Language Portfolio Command Bar (`PortfolioAiAgent`)**
   - Replaces multi-screen transaction forms with a single bottom-docked command bar.
   - Understands natural English commands such as:
     - *"Add 5 shares of SNDK with avg. cost of $1400"*
     - *"Buy 40 MU at $108.50"*
     - *"Watch NVDA"* / *"Remove RKLB"*
     - *"Create portfolio Aerospace"*

3. **Relational Room SQLite Ledger & Weighted-Average Cost Basis**
   - Manages multiple custom portfolios (`Portfolios`, `Positions`, and `Watchlist` tables) via reactive `Flow.combine()` pipelines.
   - Uses `@Transaction upsertPositionWithWeightedAvg` to automatically recalculate true weighted-average cost basis on repeat purchases of the same ticker:
     $$\text{New Avg Cost} = \frac{(\text{Old Shares} \times \text{Old Avg}) + (\text{Added Shares} \times \text{Buy Price})}{\text{Old Shares} + \text{Added Shares}}$$

4. **Ticker-Matched News Feed & Leak-Free In-App `WebView` Reader**
   - Curates financial news dynamically filtered by the symbols held in the active portfolio and watchlist.
   - Clicking an article launches `NewsArticleWebViewScreen` with a live loading progress bar, domain verification header, tagged stock performance pill, system `BackHandler` navigation, and deterministic `onRelease { webView.destroy() }` memory cleanup.

---

## 🏗️ Reactive Architecture Overview

```mermaid
flowchart LR
    UI["Jetpack Compose UI\n(AlphaPulseScreen & Canvas Chart)"] -->|AlphaPulseIntent| VM["AlphaPulseViewModel\n(MVI StateFlow)"]
    VM -->|Natural Language Prompt| AI["PortfolioAiAgent\n(Command Parser)"]
    AI -->|ParsedPortfolioCommand| Repo["PortfolioRepository &\nAlphaPulseDao (Room SQLite)"]
    Repo -->|Flow.combine()| VM
    Market["MarketDataEngine\n(Quotes, Curves & News)"] -->|Flow.combine()| VM
    VM -->|Immutable AlphaPulseUiState| UI
```

---

## 🚀 Getting Started

1. **Clone & Run:**
   Open the project in Android Studio and press **Run**. The app starts with a clean `$0.00` ledger ready for your commands.
2. **Try Sample Commands in the Bottom AI Bar:**
   - `Add 5 shares of SNDK with avg. cost of $1400`
   - `Add 40 shares of MU with avg. cost of $108.50`
   - `Watch RKLB`
   - `Create portfolio High Growth`
3. **Run the Unit Test Suite:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
