package com.bekacodes.alphapulse.domain

class PortfolioAiAgent {

    private val stopWords = setOf(
        "ADD", "BUY", "BOUGHT", "SHARES", "SHARE", "STOCK", "OF", "WITH",
        "AVG", "AVERAGE", "COST", "PRICE", "AT", "FOR", "TO", "IN", "MY",
        "PORTFOLIO", "WATCHLIST", "WATCH", "REMOVE", "DELETE", "SELL", "CREATE", "NEW"
    )

    /**
     * Parses natural-language portfolio commands such as:
     * - "add 5 shares of SNDK with avg. cost of 1400$"
     * - "buy 40 MU at $108.50"
     * - "add NVDA to watchlist"
     * - "create portfolio Aerospace"
     * - "remove RKLB"
     */
    fun parseCommand(rawInput: String): ParsedPortfolioCommand {
        val text = rawInput.trim()
        val lower = text.lowercase()

        // 1. Create Portfolio intent ("create portfolio Semis & AI")
        Regex("""(?:create|new)\s+portfolio\s+(?:called\s+|named\s+)?(.+)""", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty()) return ParsedPortfolioCommand.CreatePortfolio(name)
            }

        // 2. Watchlist intent ("watch NVDA" or "add RKLB to watchlist")
        if (lower.contains("watchlist") || lower.startsWith("watch ")) {
            val ticker = extractTickerSymbol(text)
            if (ticker != null) {
                return if (lower.contains("remove") || lower.contains("delete")) {
                    ParsedPortfolioCommand.RemoveFromWatchlist(ticker)
                } else {
                    ParsedPortfolioCommand.AddToWatchlist(ticker)
                }
            }
        }

        // 3. Remove / Sell position intent ("remove SNDK" or "sell RKLB")
        if (lower.startsWith("remove ") || lower.startsWith("delete ") || lower.startsWith("sell ")) {
            val ticker = extractTickerSymbol(text)
            if (ticker != null) {
                return ParsedPortfolioCommand.RemovePosition(ticker)
            }
        }

        // 4. Add / Buy position intent ("add 5 shares of SNDK with avg. cost of 1400$")
        val numbers = Regex("""(\d+(?:,\d{3})*(?:\.\d+)?)""")
            .findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .toList()

        val ticker = extractTickerSymbol(text)

        if (ticker != null && numbers.size >= 2) {
            val shares = numbers[0]
            val avgCost = numbers[1]
            if (shares > 0 && avgCost > 0) {
                return ParsedPortfolioCommand.AddPosition(
                    symbol = ticker,
                    shares = shares,
                    avgCost = avgCost
                )
            }
        }

        return ParsedPortfolioCommand.Unrecognized(
            "Try: \"Add 5 shares of SNDK with avg. cost of $1400\", \"Watch NVDA\", or \"Create portfolio Semiconductors\""
        )
    }

    private fun extractTickerSymbol(text: String): String? {
        // Explicit pattern first: "shares of SNDK" or "buy MU"
        Regex("""(?:shares\s+of|buy|add|watch|remove|sell)\s+([A-Za-z]{2,5})\b""", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val candidate = match.groupValues[1].uppercase()
                if (candidate !in stopWords) return candidate
            }

        // Fallback: scan uppercase-like 2..5 letter tokens not in stopWords
        return Regex("""\b([A-Za-z]{2,5})\b""")
            .findAll(text)
            .map { it.groupValues[1].uppercase() }
            .firstOrNull { it !in stopWords }
    }
}