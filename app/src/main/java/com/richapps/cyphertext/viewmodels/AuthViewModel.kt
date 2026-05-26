package com.richapps.cyphertext.viewmodels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.Utils.getFirebaseAuthInstance
import com.richapps.cyphertext.models.RegisterResponse
import com.richapps.cyphertext.network.RegistrationRepository
import com.richapps.cyphertext.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {

    private val repository = RegistrationRepository(RetrofitClient.apiService)
    private val _verificationId = MutableStateFlow<String?>(null)
    private val _otpSent = MutableStateFlow<Boolean>(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    private val _isCurrentUser = MutableStateFlow<Boolean?>(null)
    val isCurrentUser= _isCurrentUser

    init {
        Utils.getFirebaseAuthInstance().addAuthStateListener { auth -> // Listen for auth state changes
            _isCurrentUser.value = auth.currentUser != null
            Log.d("AuthViewModel", "Auth state changed: ${auth.currentUser?.phoneNumber ?: "null"}")
        }
    }

    private val _isSignedIn = MutableStateFlow<Boolean>(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _registrationResult = MutableStateFlow<Result<RegisterResponse>?>(null)
    val registrationResult: StateFlow<Result<RegisterResponse?>?> = _registrationResult

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendOTP(phoneNumber: String, activity: Activity) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(p0: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthViewModel", "Verification failed: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken) {
                    _verificationId.value = verificationId
                    _otpSent.value = true
                }
        }

        val options = PhoneAuthOptions.newBuilder(Utils.getFirebaseAuthInstance())
            .setPhoneNumber("+1$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    fun signInWithPhoneAuthCredential(otp: String, activity: Activity) {
        val credential = PhoneAuthProvider.getCredential(_verificationId.value.toString(), otp)
        Utils.getFirebaseAuthInstance().signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    _isSignedIn.value = true
                }
            }
    }

    fun registerOnBackend(phoneNumber: String, username: String, fcmToken: String, firebaseUid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.register(phoneNumber, username, fcmToken, firebaseUid)
            _registrationResult.value = result
            _isLoading.value = false
        }
    }
}