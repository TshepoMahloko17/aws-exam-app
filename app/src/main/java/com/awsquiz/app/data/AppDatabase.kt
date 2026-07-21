package com.awsquiz.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, ScoreHistoryEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun scoreHistoryDao(): ScoreHistoryDao
}
