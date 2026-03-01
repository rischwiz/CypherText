package com.richapps.cyphertext.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.richapps.cyphertext.viewmodels.AuthViewModel
import com.richapps.cyphertext.R
import com.richapps.cyphertext.databinding.FragmentOTPBinding
import kotlinx.coroutines.launch

class OTPFragment : Fragment() {

    lateinit var binding : FragmentOTPBinding
    private val viewModel : AuthViewModel by viewModels()
    lateinit var number : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOTPBinding.inflate(layoutInflater)
        getUserNumber()
        Toast.makeText(requireContext(),"Sending OTP...", Toast.LENGTH_SHORT).show()
        sendOTP()
        onLoginButtonClicked()
        onBackButtonPressed()
        return binding.root
    }

    private fun onBackButtonPressed() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_OTPFragment_to_loginFragment)
        }
    }
    
    private fun onLoginButtonClicked() {
        binding.continueButton.setOnClickListener {
            val otp = binding.etOtp.text.toString()
            if (otp.length != 6) {
                Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(requireContext(), "Verifying OTP...", Toast.LENGTH_SHORT).show()
                verifyOTP(otp)
            }
        }
    }

    private fun verifyOTP(otp : String) {
        viewModel.apply {
            signInWithPhoneAuthCredential(otp, requireActivity())
            lifecycleScope.launch {
                isSignedIn.collect {
                    if (it) {
                        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                        val bundle = Bundle()
                        bundle.putString("number", number)
                        findNavController().navigate(R.id.action_loginFragment_to_userDetail, bundle)
                        // navigate to other screen
                    }
                    else {
                        Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendOTP() {
        viewModel.apply {
            sendOTP(number, requireActivity())
            lifecycleScope.launch {
                otpSent.collect {
                    if(it) {
                        Toast.makeText(requireContext(), "OTP Sent", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(requireContext(), "OTP Not Sent", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getUserNumber() {
        val bundle = arguments
        if (bundle != null) {
            number = bundle.getString("number").toString()
            binding.number.text = "+1 $number"
        }
    }

}
