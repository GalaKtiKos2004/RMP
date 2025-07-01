package ru.fefu.activitiesfefu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fefu.activitiesfefu.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userManager = UserManager.getInstance(requireContext())
        
        // Setup back navigation
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.applyButton.setOnClickListener {
            val oldPassword = binding.editOldPassword.text.toString().trim()
            val newPassword = binding.editNewPassword.text.toString().trim()
            val repeatPassword = binding.editRepeatPassword.text.toString().trim()
            
            // Validate inputs
            if (oldPassword.isEmpty()) {
                showToast("Введите старый пароль")
                return@setOnClickListener
            }
            
            if (newPassword.isEmpty()) {
                showToast("Введите новый пароль")
                return@setOnClickListener
            }
            
            if (repeatPassword.isEmpty()) {
                showToast("Повторите новый пароль")
                return@setOnClickListener
            }
            
            if (newPassword != repeatPassword) {
                showToast("Пароли не совпадают")
                return@setOnClickListener
            }
            
            if (oldPassword == newPassword) {
                showToast("Новый пароль должен отличаться от старого")
                return@setOnClickListener
            }
            
            // Change password
            changePassword(oldPassword, newPassword)
        }
    }
    
    private fun changePassword(oldPassword: String, newPassword: String) {
        lifecycleScope.launch {
            val result = userManager.changePassword(oldPassword, newPassword)
            result.fold(
                onSuccess = {
                    showToast("Пароль успешно изменен")
                    parentFragmentManager.popBackStack()
                },
                onFailure = { e ->
                    showToast(e.message ?: "Ошибка при смене пароля")
                }
            )
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 