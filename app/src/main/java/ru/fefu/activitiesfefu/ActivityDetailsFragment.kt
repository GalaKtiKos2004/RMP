package ru.fefu.activitiesfefu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import ru.fefu.activitiesfefu.databinding.FragmentActivityDetailsBinding
import ru.fefu.activitiesfefu.data.ActivityDao
import ru.fefu.activitiesfefu.data.ActivityEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import ru.fefu.activitiesfefu.data.ActivityType
import android.util.Log

class ActivityDetailsFragment : Fragment() {
    private var _binding: FragmentActivityDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityDao: ActivityDao

    companion object {
        // Статический метод для создания фрагмента с аргументами
        fun newInstance(activity: ActivityListItem.Activity): ActivityDetailsFragment {
            val fragment = ActivityDetailsFragment()
            fragment.arguments = Bundle().apply {
                putInt("activityId", activity.id)
                putString("distance", activity.distance)
                putString("duration", activity.duration)
                putString("timeAgo", activity.timeAgo)
                putString("type", when (activity.type) {
                    ActivityType.BIKE -> "Велосипед 🚴‍♂️"
                    ActivityType.RUN -> "Бег 🏃‍♂️"
                    ActivityType.STEP -> "Шаг 🚶‍♂️"
                })
                activity.username?.let { putString("user", "@$it") }
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            activityDao = (requireActivity().application as ActivityApplication).database.activityDao()
            
            // Настраиваем нижнюю навигацию
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_activity -> {
                        // Используем FragmentNavigator для безопасного возврата назад
                        FragmentNavigator.goBack(parentFragmentManager)
                        true
                    }
                    R.id.nav_profile -> {
                        // Создаем и открываем фрагмент профиля
                        val profileFragment = ProfileFragment()
                        FragmentNavigator.openFragment(parentFragmentManager, profileFragment)
                        true
                    }
                    else -> false
                }
            }
            binding.bottomNavigation.selectedItemId = R.id.nav_activity
            
            // Настраиваем кнопку назад
            binding.toolbar.setNavigationOnClickListener {
                // Используем FragmentNavigator для безопасного возврата назад
                FragmentNavigator.goBack(parentFragmentManager)
            }
            
            // Получаем ID активности из аргументов
            val activityId = arguments?.getInt("activityId", -1) ?: -1
            
            // Логируем для отладки
            Log.d("ActivityDetailsFragment", "Loading activity with ID: $activityId")
            
            if (activityId != -1) {
                loadActivityDetails(activityId)
            } else {
                // Если ID не передан, используем данные из аргументов
                setupFromArguments()
            }
            
            // Настраиваем кнопку поделиться
            binding.detailsShare.setOnClickListener {
                // Реализация функции "поделиться"
            }
        } catch (e: Exception) {
            Log.e("ActivityDetailsFragment", "Error in onViewCreated", e)
            // В случае ошибки используем резервные данные
            setupFromArguments()
        }
    }
    
    private fun loadActivityDetails(activityId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val activity = activityDao.getActivityById(activityId)
                withContext(Dispatchers.Main) {
                    if (activity != null) {
                        displayActivityDetails(activity)
                    } else {
                        // Если активность не найдена в базе, используем резервные данные из аргументов
                        Log.d("ActivityDetailsFragment", "Activity not found in database, using arguments")
                        setupFromArguments()
                    }
                }
            } catch (e: Exception) {
                // В случае ошибки используем резервные данные
                Log.e("ActivityDetailsFragment", "Error loading activity details", e)
                withContext(Dispatchers.Main) {
                    setupFromArguments()
                }
            }
        }
    }
    
    private fun displayActivityDetails(activity: ActivityEntity) {
        try {
            // Проверяем, что фрагмент все еще присоединен к активити
            if (!isAdded) return
            
            // Форматируем расстояние
            binding.detailsDistance.text = String.format("%.2f км", activity.distance)
            
            // Форматируем продолжительность
            val hours = activity.durationMillis / (1000 * 60 * 60)
            val minutes = (activity.durationMillis % (1000 * 60 * 60)) / (1000 * 60)
            binding.detailsDuration.text = String.format("%d ч %02d мин", hours, minutes)
            
            // Форматируем время с момента активности
            val now = System.currentTimeMillis()
            val diff = now - activity.startDate.time
            val hoursAgo = diff / (1000 * 60 * 60)
            binding.detailsTimeAgo.text = String.format("%d часов назад", hoursAgo)
            
            // Форматируем время старта и финиша
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            view?.findViewById<TextView>(R.id.start_time)?.text = " ${timeFormat.format(activity.startDate)}"
            view?.findViewById<TextView>(R.id.finish_time)?.text = " ${timeFormat.format(activity.endDate)}"
            
            // Устанавливаем тип активности в заголовок
            val activityTypeEmoji = when (activity.type) {
                ActivityType.BIKE -> "Велосипед 🚴‍♂️"
                ActivityType.RUN -> "Бег 🏃‍♂️"
                ActivityType.STEP -> "Шаг 🚶‍♂️"
            }
            binding.toolbar.title = activityTypeEmoji
            
            Log.d("ActivityDetailsFragment", "Activity details displayed successfully")
        } catch (e: Exception) {
            // В случае ошибки используем резервные данные
            Log.e("ActivityDetailsFragment", "Error displaying activity details", e)
            setupFromArguments()
        }
    }
    
    private fun setupFromArguments() {
        try {
            // Проверяем, что фрагмент все еще присоединен к активити
            if (!isAdded) return
            
            arguments?.let { args ->
                binding.detailsDistance.text = args.getString("distance", "")
                binding.detailsDuration.text = args.getString("duration", "")
                binding.toolbar.title = args.getString("type", "")
                binding.detailsTimeAgo.text = args.getString("timeAgo", "")
                
                // Для обратной совместимости - если есть данные о пользователе
                args.getString("user")?.let { user ->
                    view?.findViewById<TextView>(R.id.user_text)?.text = user
                    view?.findViewById<TextView>(R.id.user_text)?.visibility = View.VISIBLE
                } ?: run {
                    view?.findViewById<TextView>(R.id.user_text)?.visibility = View.GONE
                }
                
                // Временные данные для старта и финиша
                view?.findViewById<TextView>(R.id.start_time)?.text = " 14:49"
                view?.findViewById<TextView>(R.id.finish_time)?.text = " 16:31"
                
                Log.d("ActivityDetailsFragment", "Setup from arguments completed successfully")
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при настройке из аргументов
            Log.e("ActivityDetailsFragment", "Error setting up from arguments", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 