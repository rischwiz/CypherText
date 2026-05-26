package com.richapps.cyphertext.security

import android.content.Context
import android.util.Base64
import android.util.Log
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kdf.HKDF
import java.security.KeyStore
import javax.crypto.KeyAgreement

object X3DHManager {

    private const val TAG = "X3DHManager"

    fun performX3DHAsSender(recipientBundle: RecipientKeyBundle): X3DHResult? {
        return try {
            val ephemeralKeyPair = Curve.generateKeyPair()
            Log.d(TAG, "Ephemeral key pair generated")

            val bobSignedPreKey = Curve.decodePoint(recipientBundle.signedPreKey, 0)
            val bobOneTimePreKey = recipientBundle.oneTimePreKey?.let {
                Curve.decodePoint(it, 0)
            }

            val dh1 = Curve.calculateAgreement(bobSignedPreKey, ephemeralKeyPair.privateKey)
            val dh2 = Curve.calculateAgreement(bobSignedPreKey, ephemeralKeyPair.privateKey)
            val dh3 = if (bobOneTimePreKey != null) {
                Curve.calculateAgreement(bobOneTimePreKey, ephemeralKeyPair.privateKey)
            } else ByteArray(0)

            val dhInput = dh1 + dh2 + dh3
            val sharedSecret = HKDF.deriveSecrets(
                dhInput,
                "CypherTextX3DH".toByteArray(),
                null,
                32
            )

            Log.d(TAG, "Sender shared secret: ${Base64.encodeToString(sharedSecret, Base64.NO_WRAP)}")

            X3DHResult(
                sharedSecret = sharedSecret,
                ephemeralPublicKey = ephemeralKeyPair.publicKey.serialize(),
                oneTimePreKeyId = recipientBundle.oneTimePreKeyId
            )

        } catch (e: Exception) {
            Log.e(TAG, "X3DH sender failed: ${e.message}")
            null
        }
    }

    fun performX3DHAsReceiver(
        context: Context,
        senderEphemeralKeyBytes: ByteArray,
        oneTimePreKeyId: Int?
    ): ByteArray? {
        return try {
            val bobSignedPreKeyPair = LocalKeyStore.getSignedPreKeyPair(context)
                ?: throw Exception("Signed prekey not found")

            val bobOneTimePreKeyPair = oneTimePreKeyId?.let {
                LocalKeyStore.getAndConsumeOneTimePreKey(context, it)
            }

            val aliceEphemeralKey = Curve.decodePoint(senderEphemeralKeyBytes, 0)

            val dh1 = Curve.calculateAgreement(aliceEphemeralKey, bobSignedPreKeyPair.privateKey)
            val dh2 = Curve.calculateAgreement(aliceEphemeralKey, bobSignedPreKeyPair.privateKey)
            val dh3 = if (bobOneTimePreKeyPair != null) {
                Curve.calculateAgreement(aliceEphemeralKey, bobOneTimePreKeyPair.privateKey)
            } else ByteArray(0)

            val dhInput = dh1 + dh2 + dh3
            val sharedSecret = HKDF.deriveSecrets(
                dhInput,
                "CypherTextX3DH".toByteArray(),
                null,
                32
            )

            Log.d(TAG, "Receiver shared secret: ${Base64.encodeToString(sharedSecret, Base64.NO_WRAP)}")
            sharedSecret

        } catch (e: Exception) {
            Log.e(TAG, "X3DH receiver failed: ${e.message}")
            null
        }
    }

    private fun convertDerToSignalKey(derKey: ByteArray): ECPublicKey {
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val keySpec = java.security.spec.X509EncodedKeySpec(derKey)
        val ecPublicKey = keyFactory.generatePublic(keySpec) as java.security.interfaces.ECPublicKey

        val xCoord = ecPublicKey.w.affineX.toByteArray()
        val xBytes = when {
            xCoord.size == 32 -> xCoord
            xCoord.size == 33 -> xCoord.copyOfRange(1, 33)
            xCoord.size < 32 -> ByteArray(32 - xCoord.size) + xCoord
            else -> xCoord.copyOfRange(xCoord.size - 32, xCoord.size)
        }

        val signalKey = ByteArray(33)
        signalKey[0] = 0x05
        xBytes.copyInto(signalKey, 1)
        return Curve.decodePoint(signalKey, 0)
    }
}

data class X3DHResult(
    val sharedSecret: ByteArray,
    val ephemeralPublicKey: ByteArray,
    val oneTimePreKeyId: Int?
)