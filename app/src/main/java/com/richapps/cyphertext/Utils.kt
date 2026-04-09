package com.richapps.cyphertext

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object Utils {
    const val SERVER_URL = "http://10.0.2.2:3000"
    private var firebaseAuthInstance : FirebaseAuth? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }
    fun getFirebaseAuthInstance() : FirebaseAuth {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance()
        }
        return firebaseAuthInstance!!
    }

    fun getUserID() : String{
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    fun saveCurrentUserDbId(id: Int) {
        appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .edit().putInt("current_user_db_id", id).apply()
    }

    fun getCurrentUserDbId(): Int {
        return appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .getInt("current_user_db_id", -1)
    }
}