package ru.fefu.activitiesfefu

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fefu.activitiesfefu.data.UserEntity

class UserManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val userDao = ActivityApplication.getInstance().database.userDao()
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getInt(KEY_USER_ID, -1) != -1
    }
    
    // Get current user ID
    fun getCurrentUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }
    
    // Get current username
    fun getCurrentUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }
    
    // Get current user information from database
    suspend fun getCurrentUser(): UserEntity? {
        val userId = getCurrentUserId()
        if (userId == -1) return null
        
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }
    
    // Save user login state
    private fun saveUserLoginState(userId: Int, username: String) {
        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }
    
    // Clear user login state
    fun logout() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
    }
    
    // Login user
    suspend fun login(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByCredentials(username, password)
            if (user != null) {
                saveUserLoginState(user.id, user.username)
                true
            } else {
                false
            }
        }
    }
    
    // Register user
    suspend fun register(username: String, password: String, name: String, gender: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if username already exists
                val existingUser = userDao.getUserByUsername(username)
                if (existingUser != null) {
                    return@withContext Result.failure(Exception("Пользователь с таким именем уже существует"))
                }
                
                // Create new user
                val newUser = UserEntity(
                    username = username,
                    password = password,
                    name = name,
                    gender = gender
                )
                
                // Insert user and get ID
                val userId = userDao.insertUser(newUser)
                
                // Save login state
                saveUserLoginState(userId.toInt(), username)
                
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Change password
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("Пользователь не авторизован"))
                }
                
                val rowsUpdated = userDao.updateUserPassword(userId, oldPassword, newPassword)
                if (rowsUpdated > 0) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Неверный старый пароль"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        
        @Volatile
        private var instance: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return instance ?: synchronized(this) {
                instance ?: UserManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 