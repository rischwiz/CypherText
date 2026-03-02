package com.richapps.cyphertext

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.richapps.cyphertext.activities.MainActivity
import com.richapps.cyphertext.databinding.FragmentUserDetailBinding
import com.richapps.cyphertext.models.Users

class UserDetail : Fragment() {

    private lateinit var binding : FragmentUserDetailBinding
    var username = ""
    var userNumber = ""
    //var userID = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserDetailBinding.inflate(layoutInflater)

        getDetails()

        return binding.root
    }

    private fun getDetails() {
        binding.continueButton.setOnClickListener {
            username = binding.etUsername.text.toString()
            if (username.isEmpty()) {
                binding.etUsername.error = "Username cannot be empty"
            }
            else {
                val user = Users(userNumber, username)
                // save user to PostgreSQL database
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finishAffinity()

            }
            /*
            else {
                val user = Users(userID, userNumber, username)
                // save user to PostgreSQL database
            }
            */
        }

        val bundle = arguments
        userNumber = bundle!!.getString("number").toString()
        //userID = Utils.getUserID()
        binding.etUsername
    }

}