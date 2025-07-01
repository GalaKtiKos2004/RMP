package ru.fefu.activitiesfefu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.fefu.activitiesfefu.databinding.FragmentNewActivityBinding
import ru.fefu.activitiesfefu.data.ActivityDao
import ru.fefu.activitiesfefu.data.ActivityEntity
import ru.fefu.activitiesfefu.data.ActivityType
import java.util.Date
import kotlin.random.Random
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewActivityFragment : Fragment() {
    private var _binding: FragmentNewActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var activityTypeAdapter: ActivityTypeAdapter
    private var selectedActivityType: ActivityType? = null
    private lateinit var activityDao: ActivityDao
    private lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityDao = (requireActivity().application as ActivityApplication).database.activityDao()
        userManager = UserManager.getInstance(requireContext())

        // Используем только разрешенные типы активностей: бег, шаг и велосипед
        val allowedActivityTypes = listOf(
            ActivityType.BIKE,
            ActivityType.RUN,
            ActivityType.STEP
        )
        val activityTypes = allowedActivityTypes.map { it.displayName }
        
        activityTypeAdapter = ActivityTypeAdapter(activityTypes) { selectedTypeString ->
            selectedActivityType = ActivityType.values().find { it.displayName == selectedTypeString }
            binding.startButton.isEnabled = true
        }

        binding.activityTypeRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.activityTypeRecyclerView.adapter = activityTypeAdapter
        
        // Изначально кнопка неактивна
        binding.startButton.isEnabled = false

        // Обработка клика по кнопке "Начать"
        binding.startButton.setOnClickListener {
            val currentUserId = userManager.getCurrentUserId()
            if (currentUserId == -1) {
                Snackbar.make(binding.root, "Необходимо авторизоваться", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            selectedActivityType?.let { type ->
                binding.startButton.isEnabled = false // Предотвращаем двойные нажатия
                
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val random = Random(System.currentTimeMillis())
                        val startDate = Date()
                        val durationMillis = random.nextLong(600000, 3600000) // 10 mins - 1 hour
                        val endDate = Date(startDate.time + durationMillis)
                        val distance = random.nextFloat() * 10 // Random distance up to 10 km

                        val newActivity = ActivityEntity(
                            id = 0, // Room will auto-generate id
                            userId = currentUserId, // Привязываем активность к текущему пользователю
                            type = type,
                            startDate = startDate,
                            endDate = endDate,
                            distance = distance,
                            durationMillis = durationMillis,
                            coordinates = emptyList() // For now, empty list
                        )
                        activityDao.insertActivity(newActivity)
                        
                        withContext(Dispatchers.Main) {
                            val fragment = ActiveActivityFragment()
                            fragment.arguments = Bundle().apply {
                                putString("activityType", type.displayName)
                            }
                            parentFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Snackbar.make(binding.root, "Ошибка: ${e.message}", Snackbar.LENGTH_LONG).show()
                            binding.startButton.isEnabled = true
                        }
                    }
                }
            } ?: run {
                Snackbar.make(binding.root, "Пожалуйста, выберите тип активности", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 