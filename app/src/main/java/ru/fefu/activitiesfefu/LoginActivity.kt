package ru.fefu.activitiesfefu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fefu.activitiesfefu.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userManager = UserManager.getInstance(this)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnContinue.setOnClickListener {
            val username = binding.etLogin.text.toString().trim()
            val password = binding.etPass.text.toString().trim()
            
            if (username.isEmpty()) {
                binding.loginLayout.error = "Введите логин"
                return@setOnClickListener
            } else {
                binding.loginLayout.error = null
            }
            
            if (password.isEmpty()) {
                binding.passLayout.error = "Введите пароль"
                return@setOnClickListener
            } else {
                binding.passLayout.error = null
            }
            
            // Attempt login
            lifecycleScope.launch {
                val success = userManager.login(username, password)
                if (success) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@LoginActivity, 
                        "Неверный логин или пароль", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
