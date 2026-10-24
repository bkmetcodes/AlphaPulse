package com.bekacodes.alphapulse.domain

import com.bekacodes.alphapulse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PortfolioAiAgent {

    private val stopWords = setOf(
        "ADD", "BUY", "BOUGHT", "SHARES", "SHARE", "STOCK", "OF", "WITH",
        "AVG", "AVERAGE", "COST", "PRICE", "AT", "FOR", "TO", "IN", "MY",
        "PORTFOLIO", "WATCHLIST", "WATCH", "REMOVE", "DELETE", "SELL", "CREATE", "NEW",
        "PUT", "ON", "AND"
    )

    /**
     * Hybrid natural-language portfolio command parser.
     * 1. Runs zero-latency local regex/NLP parser.
     * 2. Supports multi-action prompts locally (e.g. "Create a Semis portfolio, add 5 shares of SNDK at $1400, and put MU on my watchlist").
     * 3. Falls back to Gemini API (via BuildConfig.GEMINI_API_KEY) if local parsing is unrecognized.
     */
    suspend fun parseCommand(rawInput: String): ParsedPortfolioCommand {
        val text = rawInput.trim()
        if (text.isBlank()) {
            return ParsedPortfolioCommand.Unrecognized("Please enter a command.")
        }

        // Fast zero-latency local parse
        val localResult = parseLocal(text)
        if (localResult !is ParsedPortfolioCommand.Unrecognized) {
            return localResult
        }

        // Optional Gemini API fallback if GEMINI_API_KEY is configured
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            val geminiResult = queryGeminiFallback(text, apiKey)
            if (geminiResult != null && geminiResult !is ParsedPortfolioCommand.Unrecognized) {
                return geminiResult
            }
        }

        return localResult
    }

    private fun parseLocal(text: String): ParsedPortfolioCommand {
        // Multi-clause splitting (e.g., "Create a Semis portfolio, add 5 shares of SNDK at $1400, and put MU on my watchlist")
        val clauses = text.split(Regex(""",|;|(?i)\band\b"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (clauses.size > 1) {
            val recognized = clauses.mapNotNull { clause ->
                val single = parseSingleClause(clause)
                if (single !is ParsedPortfolioCommand.Unrecognized) single else null
            }
            if (recognized.isNotEmpty()) {
                return if (recognized.size == 1) recognized.first() else ParsedPortfolioCommand.Multiple(recognized)
            }
        }

        return parseSingleClause(text)
    }

    private fun parseSingleClause(text: String): ParsedPortfolioCommand {
        val lower = text.lowercase()

        // 1. Create Portfolio intent ("create a Semis portfolio" or "create portfolio Aerospace")
        Regex("""(?:create|new)\s+(?:a\s+)?(.+?)\s+portfolio""", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val name = match.groupValues[1].replace(Regex("""(?i)\bportfolio\b"""), "").trim()
                if (name.isNotEmpty()) return ParsedPortfolioCommand.CreatePortfolio(name)
            }
        Regex("""(?:create|new)\s+portfolio\s+(?:called\s+|named\s+)?(.+)""", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty()) return ParsedPortfolioCommand.CreatePortfolio(name)
            }

        // 2. Watchlist intent ("watch NVDA", "add RKLB to watchlist", or "put MU on my watchlist")
        if (lower.contains("watchlist") || lower.startsWith("watch ") || lower.contains("put ") && lower.contains("watchlist")) {
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

        // 4. Add / Buy position intent ("add 5 shares of SNDK with avg. cost of 1400$" or "add 5 shares of SNDK at $1400")
        val numbers = Regex("""(\d+(?:,\d{3})*(?:\.\d+)?)""")
            .findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .toList()

        val ticker = extractTickerSymbol(text)

        if (ticker != null && numbers.isNotEmpty()) {
            val shares = numbers[0]
            val avgCost = if (numbers.size >= 2) numbers[1] else 100.0
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
        Regex("""(?:shares\s+of|buy|add|watch|remove|sell|put)\s+([A-Za-z]{2,5})\b""", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val candidate = match.groupValues[1].uppercase()
                if (candidate !in stopWords) return candidate
            }

        return Regex("""\b([A-Za-z]{2,5})\b""")
            .findAll(text)
            .map { it.groupValues[1].uppercase() }
            .firstOrNull { it !in stopWords }
    }

    private suspend fun queryGeminiFallback(prompt: String, apiKey: String): ParsedPortfolioCommand? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

                val systemPrompt = "Extract portfolio actions as JSON array. Actions: CREATE_PORTFOLIO (name), ADD_POSITION (symbol, shares, avgCost), REMOVE_POSITION (symbol), ADD_WATCHLIST (symbol), REMOVE_WATCHLIST (symbol). Return JSON only without codeblock backticks."
                val body = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", "$systemPrompt\nUser: $prompt")
                        ))
                    ))
                }

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                if (conn.responseCode == 200) {
                    val respText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val responseJson = JSONObject(respText)
                    val reply = responseJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanJson = reply.replace("```json", "").replace("```", "").trim()
                    val array = JSONArray(cleanJson)
                    val commands = mutableListOf<ParsedPortfolioCommand>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        when (obj.optString("action")) {
                            "CREATE_PORTFOLIO" -> commands.add(ParsedPortfolioCommand.CreatePortfolio(obj.getString("name")))
                            "ADD_POSITION" -> commands.add(
                                ParsedPortfolioCommand.AddPosition(
                                    symbol = obj.getString("symbol"),
                                    shares = obj.getDouble("shares"),
                                    avgCost = obj.getDouble("avgCost")
                                )
                            )
                            "REMOVE_POSITION" -> commands.add(ParsedPortfolioCommand.RemovePosition(obj.getString("symbol")))
                            "ADD_WATCHLIST" -> commands.add(ParsedPortfolioCommand.AddToWatchlist(obj.getString("symbol")))
                            "REMOVE_WATCHLIST" -> commands.add(ParsedPortfolioCommand.RemoveFromWatchlist(obj.getString("symbol")))
                        }
                    }

                    if (commands.isNotEmpty()) {
                        return@withContext if (commands.size == 1) commands.first() else ParsedPortfolioCommand.Multiple(commands)
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
}