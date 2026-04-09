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


class SplashFragment : Fragment() {
    private lateinit var binding : FragmentSplashBinding
    private val viewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(layoutInflater)

        lifecycleScope.launch {

            lifecycleScope.launch {
                Log.d("Splash", "coroutine started")
                kotlinx.coroutines.delay(3000) // give Firebase more time to restore session
                Log.d("Splash", "delay finished")

                // Take first emission after delay
                val isLoggedIn = viewModel.isCurrentUser.value  // read current value directly
                Log.d("Splash", "isLoggedIn: $isLoggedIn")

                if (isLoggedIn) {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }

//            val minDelay = async { delay(2500) }
//
//            val isLoggedIn = viewModel.isCurrentUser
//                .drop(1)
//                .first()
//
//            minDelay.await()
//
//            if (isLoggedIn) {
//                startActivity(Intent(requireContext(), MainActivity::class.java))
//                requireActivity().finish()
//            }
//            else {
//                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
//            }
        }

        return binding.root
    }
}