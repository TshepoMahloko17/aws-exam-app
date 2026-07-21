package com.awsquiz.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val questionOrderJson: String,
    val currentIndex: Int,
    val answersJson: String,
    val isCompleted: Boolean,
    val createdAt: Long
)
