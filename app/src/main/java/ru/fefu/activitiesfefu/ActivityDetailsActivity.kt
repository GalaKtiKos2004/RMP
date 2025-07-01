package ru.fefu.activitiesfefu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.fefu.activitiesfefu.data.ActivityDao
import ru.fefu.activitiesfefu.data.ActivityEntity
import ru.fefu.activitiesfefu.data.ActivityType
import ru.fefu.activitiesfefu.databinding.FragmentActivityDetailsBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Активность для отображения деталей активности
 */
class ActivityDetailsActivity : AppCompatActivity() {
    private lateinit var binding: FragmentActivityDetailsBinding
    private lateinit var activityDao: ActivityDao
    private var currentActivityId: Int = -1
    private var isCurrentUserActivity: Boolean = false
    private lateinit var userManager: UserManager
    private var currentComment: String = ""
    private var commentChanged: Boolean = false

    companion object {
        private const val TAG = "ActivityDetailsActivity"
        private const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        private const val EXTRA_DISTANCE = "extra_distance"
        private const val EXTRA_DURATION = "extra_duration"
        private const val EXTRA_TIME_AGO = "extra_time_ago"
        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_USERNAME = "extra_username"
        private const val EXTRA_COMMENT = "extra_comment"

        /**
         * Создает Intent для запуска активности с деталями
         */
        fun createIntent(context: Context, activity: ActivityListItem.Activity): Intent {
            return Intent(context, ActivityDetailsActivity::class.java).apply {
                putExtra(EXTRA_ACTIVITY_ID, activity.id)
                putExtra(EXTRA_DISTANCE, activity.distance)
                putExtra(EXTRA_DURATION, activity.duration)
                putExtra(EXTRA_TIME_AGO, activity.timeAgo)
                putExtra(EXTRA_TYPE, when (activity.type) {
                    ActivityType.BIKE -> "Велосипед 🚴‍♂️"
                    ActivityType.RUN -> "Бег 🏃‍♂️"
                    ActivityType.STEP -> "Шаг 🚶‍♂️"
                })
                activity.username?.let { putExtra(EXTRA_USERNAME, "@$it") }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = FragmentActivityDetailsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Получаем DAO для работы с базой данных
            activityDao = (application as ActivityApplication).database.activityDao()
            userManager = UserManager.getInstance(this)
            
            // Настраиваем Toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            
            // Скрываем нижнюю навигацию, так как мы в отдельной активности
            binding.bottomNavigation.visibility = View.GONE
            
            // Получаем ID активности из Intent
            currentActivityId = intent.getIntExtra(EXTRA_ACTIVITY_ID, -1)
            
            // Логируем для отладки
            Log.d(TAG, "Loading activity with ID: $currentActivityId")
            
            if (currentActivityId != -1) {
                loadActivityDetails(currentActivityId)
            } else {
                // Если ID не передан, используем данные из Intent
                setupFromIntent()
            }
            
            // Настраиваем кнопку поделиться
            binding.detailsShare.setOnClickListener {
                // Реализация функции "поделиться"
            }
            
            // Настраиваем кнопку удаления
            binding.detailsDelete.setOnClickListener {
                showDeleteConfirmationDialog()
            }
            
            // Настраиваем кнопку сохранения комментария
            binding.saveCommentButton.setOnClickListener {
                saveComment()
            }
            
            // Настраиваем слушатель изменений в поле комментария
            binding.commentEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    val newComment = s.toString()
                    if (newComment != currentComment) {
                        commentChanged = true
                        // Показываем кнопку сохранения комментария
                        showSaveCommentButton()
                    }
                }
            })
            
            // Добавляем обработчик потери фокуса для поля комментария
            binding.commentEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && commentChanged) {
                    showSaveCommentDialog()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // В случае ошибки используем резервные данные из Intent
            setupFromIntent()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Обработка нажатия на кнопку "назад" в Toolbar
        if (item.itemId == android.R.id.home) {
            // Если комментарий был изменен, спрашиваем о сохранении
            if (commentChanged) {
                showSaveCommentDialog()
                return true
            }
            finish() // Закрываем активность
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        // Если комментарий был изменен, спрашиваем о сохранении
        if (commentChanged) {
            showSaveCommentDialog()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun loadActivityDetails(activityId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val activity = activityDao.getActivityById(activityId)
                withContext(Dispatchers.Main) {
                    if (activity != null) {
                        // Проверяем, принадлежит ли активность текущему пользователю
                        isCurrentUserActivity = activity.userId == userManager.getCurrentUserId()
                        
                        // Показываем кнопку удаления только для активностей текущего пользователя
                        binding.detailsDelete.visibility = if (isCurrentUserActivity) View.VISIBLE else View.GONE
                        
                        // Устанавливаем текущий комментарий
                        currentComment = activity.comment
                        binding.commentEditText.setText(activity.comment)
                        
                        // Делаем поле комментария доступным для редактирования только для активностей текущего пользователя
                        binding.commentEditText.isEnabled = isCurrentUserActivity
                        
                        displayActivityDetails(activity)
                    } else {
                        // Если активность не найдена в базе, используем резервные данные из Intent
                        Log.d(TAG, "Activity not found in database, using intent extras")
                        setupFromIntent()
                    }
                }
            } catch (e: Exception) {
                // В случае ошибки используем резервные данные из Intent
                Log.e(TAG, "Error loading activity details", e)
                withContext(Dispatchers.Main) {
                    setupFromIntent()
                }
            }
        }
    }
    
    private fun displayActivityDetails(activity: ActivityEntity) {
        try {
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
            findViewById<TextView>(R.id.start_time)?.text = " ${timeFormat.format(activity.startDate)}"
            findViewById<TextView>(R.id.finish_time)?.text = " ${timeFormat.format(activity.endDate)}"
            
            // Устанавливаем тип активности в заголовок
            val activityTypeEmoji = when (activity.type) {
                ActivityType.BIKE -> "Велосипед 🚴‍♂️"
                ActivityType.RUN -> "Бег 🏃‍♂️"
                ActivityType.STEP -> "Шаг 🚶‍♂️"
            }
            binding.toolbar.title = activityTypeEmoji
            
            Log.d(TAG, "Activity details displayed successfully")
        } catch (e: Exception) {
            // В случае ошибки используем резервные данные из Intent
            Log.e(TAG, "Error displaying activity details", e)
            setupFromIntent()
        }
    }
    
    private fun setupFromIntent() {
        try {
            intent?.let { intent ->
                binding.detailsDistance.text = intent.getStringExtra(EXTRA_DISTANCE) ?: ""
                binding.detailsDuration.text = intent.getStringExtra(EXTRA_DURATION) ?: ""
                binding.toolbar.title = intent.getStringExtra(EXTRA_TYPE) ?: ""
                binding.detailsTimeAgo.text = intent.getStringExtra(EXTRA_TIME_AGO) ?: ""
                
                // Для обратной совместимости - если есть данные о пользователе
                intent.getStringExtra(EXTRA_USERNAME)?.let { user ->
                    findViewById<TextView>(R.id.user_text)?.text = user
                    findViewById<TextView>(R.id.user_text)?.visibility = View.VISIBLE
                } ?: run {
                    findViewById<TextView>(R.id.user_text)?.visibility = View.GONE
                }
                
                // Временные данные для старта и финиша
                findViewById<TextView>(R.id.start_time)?.text = " 14:49"
                findViewById<TextView>(R.id.finish_time)?.text = " 16:31"
                
                // Скрываем кнопку удаления для демо-активностей
                binding.detailsDelete.visibility = View.GONE
                
                // Делаем поле комментария недоступным для редактирования для демо-активностей
                binding.commentEditText.isEnabled = false
                
                Log.d(TAG, "Setup from intent completed successfully")
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при настройке из Intent
            Log.e(TAG, "Error setting up from intent", e)
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удаление активности")
            .setMessage("Вы уверены, что хотите удалить эту активность?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteActivity()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun deleteActivity() {
        if (currentActivityId != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = activityDao.deleteActivityById(currentActivityId)
                    withContext(Dispatchers.Main) {
                        if (result > 0) {
                            // Успешно удалено
                            Toast.makeText(this@ActivityDetailsActivity, "Активность удалена", Toast.LENGTH_SHORT).show()
                            finish() // Закрываем активность
                        } else {
                            // Ничего не удалено
                            Toast.makeText(this@ActivityDetailsActivity, "Не удалось удалить активность", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting activity", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ActivityDetailsActivity, "Ошибка при удалении активности", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Невозможно удалить демо-активность", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSaveCommentButton() {
        // Показываем кнопку сохранения комментария
        binding.saveCommentButton.visibility = View.VISIBLE
    }
    
    private fun showSaveCommentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Сохранение комментария")
            .setMessage("Хотите сохранить изменения в комментарии?")
            .setPositiveButton("Сохранить") { _, _ ->
                saveComment()
            }
            .setNegativeButton("Не сохранять") { _, _ ->
                // Отменяем изменения
                binding.commentEditText.setText(currentComment)
                commentChanged = false
                finish()
            }
            .show()
    }
    
    private fun saveComment() {
        if (currentActivityId != -1) {
            val newComment = binding.commentEditText.text.toString()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = activityDao.updateActivityComment(currentActivityId, newComment)
                    withContext(Dispatchers.Main) {
                        if (result > 0) {
                            // Успешно обновлено
                            Toast.makeText(this@ActivityDetailsActivity, "Комментарий сохранен", Toast.LENGTH_SHORT).show()
                            currentComment = newComment
                            commentChanged = false
                            // Скрываем кнопку сохранения
                            binding.saveCommentButton.visibility = View.GONE
                        } else {
                            // Ничего не обновлено
                            Toast.makeText(this@ActivityDetailsActivity, "Не удалось сохранить комментарий", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving comment", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ActivityDetailsActivity, "Ошибка при сохранении комментария", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Невозможно сохранить комментарий для демо-активности", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
} 