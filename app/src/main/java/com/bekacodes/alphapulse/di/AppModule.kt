package com.bekacodes.alphapulse.di

import android.content.Context
import com.bekacodes.alphapulse.data.local.AlphaPulseDao
import com.bekacodes.alphapulse.data.local.AlphaPulseDatabase
import com.bekacodes.alphapulse.data.market.MarketDataEngine
import com.bekacodes.alphapulse.data.repository.PortfolioRepositoryImpl
import com.bekacodes.alphapulse.domain.PortfolioAiAgent
import com.bekacodes.alphapulse.domain.repository.PortfolioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlphaPulseDatabase(
        @ApplicationContext context: Context
    ): AlphaPulseDatabase = AlphaPulseDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideAlphaPulseDao(
        database: AlphaPulseDatabase
    ): AlphaPulseDao = database.dao()

    @Provides
    @Singleton
    fun provideMarketDataEngine(): MarketDataEngine = MarketDataEngine()

    @Provides
    @Singleton
    fun providePortfolioAiAgent(): PortfolioAiAgent = PortfolioAiAgent()

    @Provides
    @Singleton
    fun providePortfolioRepository(
        dao: AlphaPulseDao
    ): PortfolioRepository = PortfolioRepositoryImpl(dao)
}