package ru.fefu.activitiesfefu

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import android.text.TextPaint
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fefu.activitiesfefu.databinding.ActivityRegisterBinding
import androidx.core.graphics.toColorInt

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userManager = UserManager.getInstance(this)

        // возвращаемся по стрелке
        binding.btnBack.setOnClickListener { finish() }

        // выпадающий список «Пол»
        val genders = listOf("Мужской", "Женский")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actGender.setAdapter(adapter)

        makeAgreementSpan()

        // обработка кнопки
        binding.btnContinue.setOnClickListener {
            val username = binding.etLogin.text.toString().trim()
            val password = binding.etPass.text.toString().trim()
            val repeatPassword = binding.etRepeat.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val gender = binding.actGender.text.toString().trim()
            
            // Validate inputs
            var hasError = false
            
            if (username.isEmpty()) {
                binding.loginLayout.error = "Введите логин"
                hasError = true
            } else {
                binding.loginLayout.error = null
            }
            
            if (password.isEmpty()) {
                binding.passLayout.error = "Введите пароль"
                hasError = true
            } else {
                binding.passLayout.error = null
            }
            
            if (repeatPassword.isEmpty()) {
                binding.repeatLayout.error = "Повторите пароль"
                hasError = true
            } else if (password != repeatPassword) {
                binding.repeatLayout.error = "Пароли не совпадают"
                hasError = true
            } else {
                binding.repeatLayout.error = null
            }
            
            if (name.isEmpty()) {
                binding.nameLayout.error = "Введите имя"
                hasError = true
            } else {
                binding.nameLayout.error = null
            }
            
            if (gender.isEmpty()) {
                binding.genderLayout.error = "Выберите пол"
                hasError = true
            } else {
                binding.genderLayout.error = null
            }
            
            if (hasError) {
                return@setOnClickListener
            }
            
            // Register user
            lifecycleScope.launch {
                val result = userManager.register(username, password, name, gender)
                result.fold(
                    onSuccess = {
                        Toast.makeText(
                            this@RegistrationActivity, 
                            "Регистрация успешна", 
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                        finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            this@RegistrationActivity, 
                            e.message ?: "Ошибка регистрации", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun makeAgreementSpan() {
        val full = getString(R.string.agreement_full)
        val privacy = getString(R.string.privacy)
        val agreement = getString(R.string.agreement)

        val span = SpannableString(full)
        val color = "#6200EE".toColorInt()

        // Политика конфиденциальности
        val startPrivacy = full.indexOf(privacy)
        val endPrivacy = startPrivacy + privacy.length

        val privacySpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(this@RegistrationActivity, "Политика", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = color
                ds.isUnderlineText = false
            }
        }
        span.setSpan(privacySpan, startPrivacy, endPrivacy, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Пользовательское соглашение
        val startAgreement = full.indexOf(agreement)
        val endAgreement = startAgreement + agreement.length

        val agreementSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(this@RegistrationActivity, "Соглашение", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = color
                ds.isUnderlineText = false
            }
        }
        span.setSpan(agreementSpan, startAgreement, endAgreement, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvAgreement.text = span
        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
    }
}
