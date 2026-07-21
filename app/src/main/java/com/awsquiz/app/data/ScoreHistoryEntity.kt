package com.awsquiz.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_history")
data class ScoreHistoryEntity(
    @PrimaryKey val id: String,
    val completionDate: Long,
    val percentage: Int,
    val passed: Boolean
)
