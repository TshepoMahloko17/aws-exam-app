package com.awsquiz.app.di

import android.content.Context
import android.content.res.AssetManager
import androidx.room.Room
import com.awsquiz.app.data.AppDatabase
import com.awsquiz.app.data.JsonQuestionRepository
import com.awsquiz.app.data.RoomScoreHistoryRepository
import com.awsquiz.app.data.RoomSessionRepository
import com.awsquiz.app.data.ScoreHistoryDao
import com.awsquiz.app.data.SessionDao
import com.awsquiz.app.domain.DefaultQuestionShuffler
import com.awsquiz.app.domain.QuestionRepository
import com.awsquiz.app.domain.QuestionShuffler
import com.awsquiz.app.domain.ScoreHistoryRepository
import com.awsquiz.app.domain.SessionRepository
import dagger.Binds
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aws_quiz_database"
        ).build()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideScoreHistoryDao(database: AppDatabase): ScoreHistoryDao {
        return database.scoreHistoryDao()
    }

    @Provides
    @Singleton
    fun provideQuestionRepository(@ApplicationContext context: Context): QuestionRepository {
        return JsonQuestionRepository(context.assets)
    }

    @Provides
    fun provideQuestionShuffler(): QuestionShuffler {
        return DefaultQuestionShuffler()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    abstract fun bindSessionRepository(
        impl: RoomSessionRepository
    ): SessionRepository

    @Binds
    abstract fun bindScoreHistoryRepository(
        impl: RoomScoreHistoryRepository
    ): ScoreHistoryRepository
}
