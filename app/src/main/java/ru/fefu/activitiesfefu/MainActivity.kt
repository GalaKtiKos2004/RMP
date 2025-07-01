package ru.fefu.activitiesfefu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import ru.fefu.activitiesfefu.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var userManager: UserManager

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            super.onFragmentResumed(fm, f)
            // При возобновлении любого фрагмента, убеждаемся, что только верхний видимый фрагмент показан
            fm.fragments.forEach { fragment ->
                if (fragment != f && fragment.isVisible) {
                    fm.beginTransaction()
                        .setReorderingAllowed(true)
                        .hide(fragment)
                        .commitNowAllowingStateLoss()
                }
            }
            // Управляем видимостью FAB в зависимости от того, является ли текущий фрагмент основным табом
            when (f) {
                is ActivityFragment -> binding.fabNewActivity.show()
                is ProfileEditFragment -> binding.fabNewActivity.hide()
                is NewActivityFragment -> binding.fabNewActivity.hide()
                is ActiveActivityFragment -> binding.fabNewActivity.hide()
                else -> binding.fabNewActivity.hide()
            }
            
            // Обновляем выбранный элемент в нижней навигации в соответствии с текущим фрагментом
            when (f) {
                is ActivityFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_activity
                is ProfileEditFragment -> binding.bottomNavigation.selectedItemId = R.id.nav_profile
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userManager = UserManager.getInstance(this)
        
        // Check if user is logged in, if not redirect to welcome screen
        if (!userManager.isLoggedIn()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Регистрируем колбэки для управления видимостью фрагментов
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)

        // Первый запуск: показываем фрагмент "Активность"
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, ActivityFragment(), "ACTIVITY_TAG")
                .commit()
        }

        // Обработка нажатий в BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_activity -> switchFragment(ActivityFragment(), "ACTIVITY_TAG")
                R.id.nav_profile -> switchFragment(ProfileEditFragment(), "PROFILE_TAG")
                else -> false
            }
        }

        binding.fabNewActivity.setOnClickListener {
            val newActivityFragment = NewActivityFragment()
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, newActivityFragment, "NEW_ACTIVITY_TAG")
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        if (currentFragment is NewActivityFragment || currentFragment is ActiveActivityFragment) {
            // Если текущий фрагмент - создание или активная активность, возвращаемся к основному фрагменту
            switchFragment(ActivityFragment(), "ACTIVITY_TAG")
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    }

    private fun switchFragment(fragment: Fragment, tag: String): Boolean {
        val currentFragment = supportFragmentManager.findFragmentByTag(tag)

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                // Скрыть все фрагменты, кроме текущего
                supportFragmentManager.fragments.forEach { f ->
                    if (f != currentFragment) {
                        hide(f)
                    }
                }

                if (currentFragment == null) {
                    add(R.id.fragment_container, fragment, tag)
                } else {
                    show(currentFragment)
                }
            }
            .commit()
        return true
    }
}