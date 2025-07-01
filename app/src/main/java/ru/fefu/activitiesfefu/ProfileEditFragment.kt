package ru.fefu.activitiesfefu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.fefu.activitiesfefu.databinding.FragmentProfileEditBinding

class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var userManager: UserManager

    companion object {
        private const val CHANGE_PASSWORD_TAG = "CHANGE_PASSWORD_TAG"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userManager = UserManager.getInstance(requireContext())
        
        // Load current user information
        loadUserInfo()

        // Hide save button since fields are not editable
        binding.toolbar.findViewById<TextView>(R.id.save_button).visibility = View.GONE

        binding.changePasswordButton.setOnClickListener {
            val changePasswordFragment = parentFragmentManager.findFragmentByTag(CHANGE_PASSWORD_TAG)
            parentFragmentManager.beginTransaction().apply {
                setReorderingAllowed(true)
                parentFragmentManager.fragments.forEach { hide(it) }
                if (changePasswordFragment == null) {
                    add(R.id.fragment_container, ChangePasswordFragment(), CHANGE_PASSWORD_TAG)
                } else {
                    show(changePasswordFragment)
                }
                addToBackStack(null)
                commit()
            }
        }

        binding.logoutButton.setOnClickListener {
            userManager.logout()
            // Redirect to welcome screen
            startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            requireActivity().finish()
        }
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            val currentUser = userManager.getCurrentUser()
            if (currentUser != null) {
                binding.editLogin.setText(currentUser.username)
                binding.editLogin.isEnabled = false
                binding.editNickname.setText(currentUser.name)
                binding.editNickname.isEnabled = false
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 