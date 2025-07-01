package ru.fefu.activitiesfefu

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.fefu.activitiesfefu.databinding.FragmentActiveActivityBinding
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import com.google.android.material.snackbar.Snackbar

class ActiveActivityFragment : Fragment() {
    private var _binding: FragmentActiveActivityBinding? = null
    private val binding get() = _binding!!
    
    private var startTimeMillis = System.currentTimeMillis()
    private var elapsedTimeMillis = 0L
    private var isPaused = false
    private var distance = 0.0f
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                updateActivityInfo()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Восстанавливаем состояние, если оно есть
        savedInstanceState?.let { bundle ->
            startTimeMillis = bundle.getLong("startTimeMillis", System.currentTimeMillis())
            elapsedTimeMillis = bundle.getLong("elapsedTimeMillis", 0L)
            isPaused = bundle.getBoolean("isPaused", false)
            distance = bundle.getFloat("distance", 0f)
        }
        
        val activityType = arguments?.getString("activityType") ?: "Активность"
        binding.activityType.text = activityType
        
        if (savedInstanceState == null) {
            startTimeMillis = System.currentTimeMillis()
        }
        
        // Обновляем UI с текущими значениями
        updateActivityInfo()
        handler.post(updateRunnable)
        
        binding.pauseButton.text = if (isPaused) "▶" else "||"

        binding.pauseButton.setOnClickListener {
            isPaused = !isPaused
            binding.pauseButton.text = if (isPaused) "▶" else "||"
        }

        binding.finishButton.setOnClickListener {
            handler.removeCallbacks(updateRunnable)
            
            // Показываем сообщение о завершении
            Snackbar.make(binding.root, "Активность завершена!", Snackbar.LENGTH_SHORT).show()
            
            // Возвращаемся к основному экрану активности
            val activityFragment = ActivityFragment()
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, activityFragment, "ACTIVITY_TAG")
                .commit()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("startTimeMillis", startTimeMillis)
        outState.putLong("elapsedTimeMillis", elapsedTimeMillis)
        outState.putBoolean("isPaused", isPaused)
        outState.putFloat("distance", distance)
    }
    
    override fun onPause() {
        super.onPause()
        // Приостанавливаем обновление, когда фрагмент не виден
        isPaused = true
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем UI при возвращении к фрагменту
        updateActivityInfo()
    }
    
    private fun updateActivityInfo() {
        if (!isPaused) {
            elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
            
            // Форматируем время в формат HH:MM:SS
            val hours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis) % 60
            val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            
            // Симулируем увеличение дистанции
            distance += random.nextFloat() * 0.01f
            val distanceString = String.format("%.2f км", distance)
            
            binding.activityDuration.text = timeString
            binding.activityDistance.text = distanceString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
} 