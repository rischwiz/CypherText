package com.richapps.cyphertext.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.richapps.cyphertext.R
import com.richapps.cyphertext.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    lateinit var binding: FragmentChatBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(layoutInflater)



        return binding.root
    }

}