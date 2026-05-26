package com.richapps.cyphertext

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.crypto.tink.aead.AeadConfig
import com.google.firebase.auth.FirebaseAuth
import com.richapps.cyphertext.security.KeyStoreManager

class CypherTextApp : Application() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)

        FirebaseAuth.getInstance().firebaseAuthSettings
            .setAppVerificationDisabledForTesting(false)

        AeadConfig.register()
        KeyStoreManager.generateIdentityKeyPair() // Generates identity key on first launch
    }
}