package ru.fefu.activitiesfefu

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Класс для безопасной навигации между фрагментами
 */
object FragmentNavigator {
    
    /**
     * Безопасно открывает фрагмент с деталями активности
     */
    fun openActivityDetails(
        fragmentManager: FragmentManager,
        activity: ActivityListItem.Activity,
        containerId: Int = R.id.fragment_container
    ) {
        try {
            // Создаем фрагмент с деталями активности
            val fragment = ActivityDetailsFragment.newInstance(activity)
            
            // Логируем для отладки
            Log.d("FragmentNavigator", "Opening activity details: ${activity.id}")
            
            // Безопасная транзакция фрагмента
            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(containerId, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("FragmentNavigator", "Error opening activity details", e)
        }
    }
    
    /**
     * Безопасно возвращается назад
     */
    fun goBack(fragmentManager: FragmentManager) {
        try {
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            }
        } catch (e: Exception) {
            Log.e("FragmentNavigator", "Error going back", e)
        }
    }
    
    /**
     * Безопасно открывает указанный фрагмент
     */
    fun openFragment(
        fragmentManager: FragmentManager,
        fragment: Fragment,
        containerId: Int = R.id.fragment_container,
        addToBackStack: Boolean = true
    ) {
        try {
            // Логируем для отладки
            Log.d("FragmentNavigator", "Opening fragment: ${fragment.javaClass.simpleName}")
            
            // Безопасная транзакция фрагмента
            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(containerId, fragment)
                .apply {
                    if (addToBackStack) {
                        addToBackStack(null)
                    }
                }
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("FragmentNavigator", "Error opening fragment", e)
        }
    }
} 