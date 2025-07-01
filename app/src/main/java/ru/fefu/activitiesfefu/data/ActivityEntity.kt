package ru.fefu.activitiesfefu.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int, // ID пользователя, которому принадлежит активность
    val type: ActivityType,
    val startDate: Date,
    val endDate: Date,
    val distance: Float,
    val durationMillis: Long,
    val coordinates: List<Coordinates>,
    val comment: String = "" // Комментарий к активности
) 