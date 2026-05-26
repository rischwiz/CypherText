package com.richapps.cyphertext.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.richapps.cyphertext.databinding.FragmentSplashBinding
import com.richapps.cyphertext.R
import com.richapps.cyphertext.activities.MainActivity
import com.richapps.cyphertext.viewmodels.AuthViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterNotNull


class SplashFragment : Fragment() {
    private lateinit var binding : FragmentSplashBinding
    private val viewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            Log.d("Splash", "coroutine started")
            val minSplashTime = launch { delay(2500) }
            Log.d("Splash", "delay finished")

            val isLoggedIn = viewModel.isCurrentUser
                .filterNotNull()
                .first()

            minSplashTime.join()
            if (!isAdded) return@launch

            if (isLoggedIn) {
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }

        return binding.root
    }
}