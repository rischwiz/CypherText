package com.richapps.cyphertext

import android.R.attr.phoneNumber
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import android.util.Base64
import com.richapps.cyphertext.Utils.SERVER_URL
import com.richapps.cyphertext.activities.MainActivity
import com.richapps.cyphertext.databinding.FragmentUserDetailBinding
import com.richapps.cyphertext.models.Users
import com.richapps.cyphertext.network.KeyBundleUploader
import com.richapps.cyphertext.security.KeyStoreManager
import com.richapps.cyphertext.viewmodels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection

class UserDetail : Fragment() {

    private lateinit var binding : FragmentUserDetailBinding
    private val viewModel: AuthViewModel by viewModels()
    var username = ""
    var userNumber = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserDetailBinding.inflate(inflater, container, false)

        setupObservers()
        getDetails()

        return binding.root
    }
    private fun setupObservers() { // Observe registration success/failure
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.registrationResult.collectLatest { result ->
                result?.onSuccess { response ->
                    Log.d("UserDetail", "Registration successful!")
                    Log.d("UserDetail", "userId from server: ${response?.userId}")
                    response?.userId?.let { id ->
                        Utils.saveCurrentUserDbId(id)
                        Log.d("UserDetail", "Saved userId to SharedPreferences: $id")

                        // Confirm it was saved correctly
                        Log.d("UserDetail", "Retrieved userId from SharedPreferences: ${Utils.getCurrentUserDbId()}")

                        uploadIdentityKey(id)

                    }
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finishAffinity()
                }?.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

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
                val phoneNumber = userNumber
                val firebaseUid = Utils.getUserID()

                viewModel.registerOnBackend(phoneNumber, username, mockFcmToken, firebaseUid)
            }
        }
    }

    private fun uploadIdentityKey(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val publicKey = KeyStoreManager.getIdentityPublicKey()
                val base64Key = Base64.encodeToString(publicKey, Base64.NO_WRAP)

                val signedPreKey = KeyStoreManager.generateAndSaveSignedPreKey(requireContext())
                val base64SignedPreKey = Base64.encodeToString(
                    signedPreKey.publicKey.serialize(), Base64.NO_WRAP
                )

                val url = java.net.URL("${Utils.SERVER_URL}/keys/identity")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                val body = JSONObject().apply {
                    put("userId", userId)
                    put("publicKey", base64Key)
                    put("signedPreKey", base64SignedPreKey)
                }

                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("UserDetail", "Identity key uploaded successfully")
                    KeyBundleUploader.uploadOneTimePreKeys(userId, requireContext())
                } else {
                    Log.e("UserDetail", "Failed to upload identity key: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("UserDetail", "Error uploading identity key: ${e.message}")
            }
        }
    }
}