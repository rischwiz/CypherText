package com.richapps.cyphertext

import com.google.firebase.auth.FirebaseAuth

object Utils {
    private var firebaseAuthInstance : FirebaseAuth? = null
    fun getFirebaseAuthInstance() : FirebaseAuth {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance()
        }
        return firebaseAuthInstance!!
    }
}