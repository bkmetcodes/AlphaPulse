package com.bekacodes.alphapulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PortfolioEntity::class, PositionEntity::class, WatchlistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlphaPulseDatabase : RoomDatabase() {
    abstract fun dao(): AlphaPulseDao

    companion object {
        @Volatile
        private var INSTANCE: AlphaPulseDatabase? = null

        fun getInstance(context: Context): AlphaPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlphaPulseDatabase::class.java,
                    "alphapulse_portfolio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}