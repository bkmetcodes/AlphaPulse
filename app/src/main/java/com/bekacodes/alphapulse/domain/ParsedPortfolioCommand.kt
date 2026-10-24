package com.bekacodes.alphapulse.domain

sealed interface ParsedPortfolioCommand {
    data class AddPosition(
        val symbol: String,
        val shares: Double,
        val avgCost: Double,
        val targetPortfolioName: String? = null
    ) : ParsedPortfolioCommand

    data class RemovePosition(val symbol: String) : ParsedPortfolioCommand
    data class AddToWatchlist(val symbol: String) : ParsedPortfolioCommand
    data class RemoveFromWatchlist(val symbol: String) : ParsedPortfolioCommand
    data class CreatePortfolio(val name: String) : ParsedPortfolioCommand
    data class Multiple(val commands: List<ParsedPortfolioCommand>) : ParsedPortfolioCommand
    data class Unrecognized(val hint: String) : ParsedPortfolioCommand
}