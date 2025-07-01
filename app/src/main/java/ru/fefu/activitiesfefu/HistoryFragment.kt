package ru.fefu.activitiesfefu

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fefu.activitiesfefu.data.ActivityDao
import ru.fefu.activitiesfefu.data.ActivityType
import ru.fefu.activitiesfefu.data.UserEntity
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HistoryFragment : BaseActivityListFragment() {
    private lateinit var activityDao: ActivityDao
    private lateinit var userManager: UserManager
    private val userCache = mutableMapOf<Int, UserEntity>() // Кэш для хранения информации о пользователях

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityDao = (requireActivity().application as ActivityApplication).database.activityDao()
        userManager = UserManager.getInstance(requireContext())
        // Это фрагмент "Пользователей", поэтому не показываем пустое состояние
        showEmptyStateForMyActivities = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Скрываем пустое состояние для активностей пользователей
        binding.emptyStateLayout.visibility = View.GONE
        binding.activityRecyclerView.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        val currentUserId = userManager.getCurrentUserId()
        if (currentUserId != -1) {
            // Получаем активности всех пользователей, кроме текущего
            activityDao.getActivitiesExceptUserId(currentUserId).observe(viewLifecycleOwner, Observer { activities ->
                Log.d("HistoryFragment", "Received other users activities: ${activities.size}")

                if (activities.isEmpty()) {
                    // Если нет активностей других пользователей, показываем демо-данные
                    showDemoActivities()
                } else {
                    // Загружаем информацию о пользователях
                    lifecycleScope.launch {
                        val userIds = activities.map { it.userId }.distinct()
                        loadUserInfo(userIds)
                        
                        val now = System.currentTimeMillis()
                        val activityListItems = activities.map { activityEntity ->
                            val username = userCache[activityEntity.userId]?.let { 
                                // Используем имя пользователя или username, если имя не задано
                                if (it.name.isNotBlank()) it.name else it.username
                            } ?: "user_${activityEntity.userId}"
                            
                            ActivityListItem.Activity(
                                id = activityEntity.id,
                                type = activityEntity.type,
                                startDate = activityEntity.startDate,
                                endDate = activityEntity.endDate,
                                distance = String.format("%.2f км", activityEntity.distance),
                                duration = formatDuration(activityEntity.durationMillis),
                                timeAgo = formatTimeAgo(activityEntity.startDate.time, now),
                                username = username
                            )
                        }
                        updateActivityList(activityListItems)
                    }
                }
            })
        } else {
            // Если пользователь не авторизован, показываем демо-данные
            showDemoActivities()
        }
    }
    
    private suspend fun loadUserInfo(userIds: List<Int>) {
        withContext(Dispatchers.IO) {
            for (userId in userIds) {
                if (!userCache.containsKey(userId)) {
                    try {
                        val userDao = (requireActivity().application as ActivityApplication).database.userDao()
                        val user = userDao.getUserById(userId)
                        user?.let { userCache[userId] = it }
                    } catch (e: Exception) {
                        Log.e("HistoryFragment", "Error loading user info: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showDemoActivities() {
        val dummyActivities = mutableListOf<ActivityListItem>()
        val now = Date()
        
        // Секция "Вчера"
        dummyActivities.add(ActivityListItem.Section("Вчера"))

        // Пользовательские имена
        val usernames = listOf("van_darkholme", "techniquepasha", "morgen_shtern")

        // Создаем активности с информацией из скриншота, но только с разрешенными типами
        dummyActivities.add(
            ActivityListItem.Activity(
                id = 1,
                type = ActivityType.BIKE,
                startDate = Date(now.time - 14 * 60 * 60 * 1000),
                endDate = Date(now.time - 12 * 60 * 60 * 1000),
                distance = "14.32 км",
                duration = "2 часа 46 минут",
                timeAgo = "14 часов назад",
                username = usernames[0]
            )
        )
        
        dummyActivities.add(
            ActivityListItem.Activity(
                id = 2,
                type = ActivityType.STEP,
                startDate = Date(now.time - 14 * 60 * 60 * 1000),
                endDate = Date(now.time - 14 * 60 * 60 * 1000 + 48 * 60 * 1000),
                distance = "228 м",
                duration = "14 часов 48 минут",
                timeAgo = "14 часов назад",
                username = usernames[1]
            )
        )
        
        dummyActivities.add(
            ActivityListItem.Activity(
                id = 3,
                type = ActivityType.RUN,
                startDate = Date(now.time - 14 * 60 * 60 * 1000),
                endDate = Date(now.time - 14 * 60 * 60 * 1000 + 1 * 60 * 60 * 1000 + 10 * 60 * 1000),
                distance = "10 км",
                duration = "1 час 10 минут",
                timeAgo = "14 часов назад",
                username = usernames[2]
            )
        )

        updateActivityList(dummyActivities)
    }

    override fun updateActivityList(items: List<ActivityListItem>) {
        val groupedItems = groupActivitiesByDate(items.filterIsInstance<ActivityListItem.Activity>())
        val finalItems = mutableListOf<ActivityListItem>()
        
        // Добавляем секции и активности
        groupedItems.forEach { (sectionTitle, activities) ->
            finalItems.add(ActivityListItem.Section(sectionTitle))
            finalItems.addAll(activities)
        }
        
        activityListAdapter?.submitList(finalItems)
    }
    
    private fun groupActivitiesByDate(activities: List<ActivityListItem.Activity>): Map<String, List<ActivityListItem.Activity>> {
        val result = mutableMapOf<String, MutableList<ActivityListItem.Activity>>()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        // Создаем свой формат для отображения месяца в именительном падеже
        val months = arrayOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", 
                            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        
        activities.sortedByDescending { it.startDate }.forEach { activity ->
            val activityDate = Calendar.getInstance().apply { time = activity.startDate }
            
            val sectionTitle = when {
                isSameDay(activityDate, today) -> "Сегодня"
                isSameDay(activityDate, yesterday) -> "Вчера"
                else -> {
                    val month = months[activityDate.get(Calendar.MONTH)]
                    val year = activityDate.get(Calendar.YEAR)
                    "$month $year года"
                }
            }
            
            if (!result.containsKey(sectionTitle)) {
                result[sectionTitle] = mutableListOf()
            }
            result[sectionTitle]?.add(activity)
        }
        
        return result
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
    
    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> String.format("%d ч %d мин", hours, minutes)
            else -> String.format("%d мин", minutes)
        }
    }
    
    private fun formatTimeAgo(timestamp: Long, now: Long): String {
        val diff = now - timestamp
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "только что"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} минут назад"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} часов назад"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} дней назад"
            else -> dateFormat.format(Date(timestamp))
        }
    }
}