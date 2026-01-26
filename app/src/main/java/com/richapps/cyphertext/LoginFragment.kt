package com.richapps.cyphertext

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.richapps.cyphertext.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater)
        binding.continueButton.setOnClickListener {
            val phoneNumber = binding.phoneNumber.text.toString()
            if(phoneNumber.isEmpty() || phoneNumber.length != 10) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a valid phone number",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                val bundle = Bundle()
                bundle.putString("phoneNumber", phoneNumber)
                findNavController().navigate(R.id.action_loginFragment_to_OTPFragment, bundle)
                }
        }

        return binding.root
    }



}