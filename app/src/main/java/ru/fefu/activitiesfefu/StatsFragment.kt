package ru.fefu.activitiesfefu

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.fefu.activitiesfefu.data.ActivityDao
import ru.fefu.activitiesfefu.data.ActivityEntity
import ru.fefu.activitiesfefu.data.ActivityType
import java.util.Date
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.Calendar

class StatsFragment : BaseActivityListFragment() {

    private lateinit var activityDao: ActivityDao
    private lateinit var userManager: UserManager
    private var isFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityDao = (requireActivity().application as ActivityApplication).database.activityDao()
        userManager = UserManager.getInstance(requireContext())
        // Это фрагмент "Моя" активность, поэтому показываем пустое состояние
        showEmptyStateForMyActivities = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Изначально показываем пустое состояние
        binding.activityRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        
        val currentUserId = userManager.getCurrentUserId()
        if (currentUserId != -1) {
            // Получаем только активности текущего пользователя
            activityDao.getActivitiesByUserId(currentUserId).observe(viewLifecycleOwner, Observer { activities ->
                Log.d("StatsFragment", "Received user activities: ${activities.size}")

                if (activities.isEmpty()) {
                    // Для пользователя без активностей показываем пустое состояние
                    updateEmptyState(emptyList())
                } else {
                    val now = System.currentTimeMillis()
                    val activityListItems = activities.map { activityEntity ->
                        ActivityListItem.Activity(
                            id = activityEntity.id,
                            type = activityEntity.type,
                            startDate = activityEntity.startDate,
                            endDate = activityEntity.endDate,
                            distance = String.format("%.2f км", activityEntity.distance),
                            duration = formatDuration(activityEntity.durationMillis),
                            timeAgo = formatTimeAgo(activityEntity.startDate.time, now)
                        )
                    }
                    updateActivityList(activityListItems)
                }
            })
        } else {
            // Пользователь не авторизован, показываем пустое состояние
            updateEmptyState(emptyList())
        }
    }

    override fun updateActivityList(newActivities: List<ActivityListItem>) {
        val items = mutableListOf<ActivityListItem>()
        val activitiesBySection = groupActivitiesByDate(newActivities.filterIsInstance<ActivityListItem.Activity>())
        
        // Добавляем секции и активности
        activitiesBySection.forEach { (sectionTitle, activities) ->
            items.add(ActivityListItem.Section(sectionTitle))
            items.addAll(activities)
        }

        activityListAdapter?.submitList(items)
        updateEmptyState(items)
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

    private fun generateDummyActivities(): List<ActivityEntity> {
        val dummyList = mutableListOf<ActivityEntity>()
        val random = Random(System.currentTimeMillis())
        val currentUserId = userManager.getCurrentUserId()

        val now = Date()
        val twentyFourHoursAgo = Date(now.time - 24 * 60 * 60 * 1000)
        val twoDaysAgo = Date(now.time - 2 * 24 * 60 * 60 * 1000)
        val oneMonthAgo = Date(now.time - 30 * 24 * 60 * 60 * 1000)

        // Активность за сегодня
        dummyList.add(ActivityEntity(
            id = 0,
            userId = currentUserId,
            type = ActivityType.RUN,
            startDate = Date(now.time - 2 * 60 * 60 * 1000), // 2 часа назад
            endDate = Date(now.time - 1 * 60 * 60 * 1000), // 1 час назад
            distance = random.nextFloat() * 10,
            durationMillis = 60 * 60 * 1000, // 1 час
            coordinates = emptyList()
        ))

        // Активность за вчера
        dummyList.add(ActivityEntity(
            id = 0,
            userId = currentUserId,
            type = ActivityType.BIKE,
            startDate = twentyFourHoursAgo,
            endDate = Date(twentyFourHoursAgo.time + random.nextLong(1800000, 7200000)), // 30 mins - 2 hours
            distance = random.nextFloat() * 50,
            durationMillis = random.nextLong(1800000, 7200000), // 30 mins - 2 hours
            coordinates = emptyList()
        ))

        // Активность за позавчера
        dummyList.add(ActivityEntity(
            id = 0,
            userId = currentUserId,
            type = ActivityType.RUN,
            startDate = twoDaysAgo,
            endDate = Date(twoDaysAgo.time + random.nextLong(3600000, 10800000)), // 1-3 hours
            distance = random.nextFloat() * 20,
            durationMillis = random.nextLong(3600000, 10800000), // 1-3 hours
            coordinates = emptyList()
        ))

        // Активность за месяц назад
        dummyList.add(ActivityEntity(
            id = 0,
            userId = currentUserId,
            type = ActivityType.STEP,
            startDate = oneMonthAgo,
            endDate = Date(oneMonthAgo.time + random.nextLong(1800000, 3600000)), // 30 mins - 1 hour
            distance = random.nextFloat() * 5,
            durationMillis = random.nextLong(1800000, 3600000), // 30 mins - 1 hour
            coordinates = emptyList()
        ))

        return dummyList
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