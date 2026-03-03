package com.richapps.cyphertext

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.richapps.cyphertext.activities.MainActivity
import com.richapps.cyphertext.databinding.FragmentUserDetailBinding
import com.richapps.cyphertext.models.Users
import com.richapps.cyphertext.viewmodels.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

class UserDetail : Fragment() {

    private lateinit var binding : FragmentUserDetailBinding
    private val viewModel: AuthViewModel by viewModels()
    var username = ""
    var userNumber = ""
    //var userID = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserDetailBinding.inflate(inflater, container, false)

        setupObservers()
        getDetails()

        return binding.root
    }

    private fun setupObservers() {
        // Observe registration success/failure
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.registrationResult.collectLatest { result ->
                result?.onSuccess {
                    // Navigate to MainActivity only when registration is confirmed
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finishAffinity()
                }?.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe loading state to show/hide a progress bar if you have one
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.continueButton.isEnabled = !isLoading
            }
        }
    }


    private fun getDetails() {
        val bundle = arguments
        userNumber = bundle!!.getString("number").toString()

        binding.continueButton.setOnClickListener {
            username = binding.etUsername.text.toString()
            if (username.isEmpty()) {
                binding.etUsername.error = "Username cannot be empty"
            }
            else {
                // TODO: 1. Generate phoneHash (SHA-256) from userNumber
                // TODO: 2. GEt real fcmToken (FirebaseMessaging.getInstance().token)
                val mockFcmToken = "mock_token_123"
                val phoneHash = userNumber // Replace with actual phone hash generation
                viewModel.registerOnBackend(phoneHash, username, mockFcmToken)
            }

            /*
                val user = Users(userNumber, username)
                // save user to PostgreSQL database
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finishAffinity()
            }
            else {
                val user = Users(userID, userNumber, username)
                // save user to PostgreSQL database
            }
            */
        }
        //userID = Utils.getUserID()
        //binding.etUsername
    }
}