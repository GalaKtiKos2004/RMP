package ru.fefu.activitiesfefu.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startDate DESC")
    fun getAllActivities(): LiveData<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY startDate DESC")
    fun getActivitiesByUserId(userId: Int): LiveData<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE userId != :userId ORDER BY startDate DESC")
    fun getActivitiesExceptUserId(userId: Int): LiveData<List<ActivityEntity>>
    
    @Query("SELECT * FROM activities WHERE id = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: Int): ActivityEntity?

    @Insert
    suspend fun insertActivity(activity: ActivityEntity): Long
    
    @Query("DELETE FROM activities WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: Int): Int
    
    @Query("UPDATE activities SET comment = :comment WHERE id = :activityId")
    suspend fun updateActivityComment(activityId: Int, comment: String): Int
} 