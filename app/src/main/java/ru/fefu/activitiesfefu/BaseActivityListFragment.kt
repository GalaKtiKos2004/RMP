package ru.fefu.activitiesfefu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.fefu.activitiesfefu.databinding.FragmentHistoryBinding
import android.util.Log
import android.widget.Toast

abstract class BaseActivityListFragment : Fragment() {
    protected var _binding: FragmentHistoryBinding? = null
    protected val binding get() = _binding!!

    protected var activityListAdapter: ActivityListAdapter? = null
    protected var showEmptyStateForMyActivities = true // По умолчанию показываем пустое состояние только для своих активностей

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    protected fun setupRecyclerView() {
        try {
            // Проверяем, что фрагмент все еще присоединен к активити
            if (!isAdded) return
            
            // Создаем адаптер с обработчиком клика
            activityListAdapter = ActivityListAdapter() { activity ->
                try {
                    // Показываем Toast для отладки
                    Toast.makeText(context, "Нажата активность: ${activity.id}", Toast.LENGTH_SHORT).show()
                    // Логируем для отладки
                    Log.d("BaseActivityListFragment", "Activity clicked: ${activity.id}")
                    // Переходим к деталям активности
                    navigateToActivityDetails(activity)
                } catch (e: Exception) {
                    Log.e("BaseActivityListFragment", "Error handling activity click", e)
                }
            }
            
            // Настраиваем RecyclerView
            binding.activityRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = activityListAdapter
                setHasFixedSize(true) // Оптимизация для RecyclerView
                
                // Добавляем декоратор для отступов между элементами
                // addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
            
            Log.d("BaseActivityListFragment", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("BaseActivityListFragment", "Error setting up RecyclerView", e)
        }
    }

    protected fun navigateToActivityDetails(activity: ActivityListItem.Activity) {
        try {
            // Проверяем, что фрагмент все еще присоединен к активити
            if (!isAdded) return
            
            // Вместо фрагмента используем отдельную активность
            val intent = ActivityDetailsActivity.createIntent(requireContext(), activity)
            startActivity(intent)
            
            // Логируем для отладки
            Log.d("BaseActivityListFragment", "Starting ActivityDetailsActivity for activity: ${activity.id}")
        } catch (e: Exception) {
            // Логируем ошибку
            Log.e("BaseActivityListFragment", "Error starting ActivityDetailsActivity", e)
            
            // Показываем Toast с ошибкой
            Toast.makeText(context, "Ошибка при открытии деталей активности", Toast.LENGTH_SHORT).show()
        }
    }

    abstract fun updateActivityList(items: List<ActivityListItem>)

    // Метод для управления отображением пустого состояния
    protected fun updateEmptyState(items: List<ActivityListItem>) {
        try {
            // Проверяем, что фрагмент все еще присоединен к активити
            if (!isAdded) return
            
            val hasActivities = items.any { it is ActivityListItem.Activity }
            
            if (!hasActivities && showEmptyStateForMyActivities) {
                binding.activityRecyclerView.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.activityRecyclerView.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при обновлении состояния
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 