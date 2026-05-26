package com.richapps.cyphertext.security

import android.content.Context
import android.util.Base64
import android.util.Log
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.kdf.HKDF
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DoubleRatchet {

    private const val TAG = "DoubleRatchet"

    // Ratchet state stored per chat room
    data class RatchetState(
        val rootKey: ByteArray,
        val sendingChainKey: ByteArray,
        val receivingChainKey: ByteArray,
        val sendingRatchetKeyPair: ECKeyPair,
        val receivingRatchetPublicKey: ByteArray?,
        val sendMessageCount: Int = 0,
        val receiveMessageCount: Int = 0
    )

    fun initialize(
        sharedSecret: ByteArray,
        isInitiator: Boolean,
        theirRatchetKey: ByteArray? = null
    ): RatchetState {

        val ourRatchetKeyPair = Curve.generateKeyPair()

        val (rootKey, chainKey) = deriveRootAndChainKey(
            sharedSecret,
            ByteArray(32)
        )

        return if (isInitiator) {
            RatchetState(
                rootKey = rootKey,
                sendingChainKey = chainKey,
                receivingChainKey = ByteArray(32),
                sendingRatchetKeyPair = ourRatchetKeyPair,
                receivingRatchetPublicKey = theirRatchetKey
            )
        } else {
            RatchetState(
                rootKey = rootKey,
                sendingChainKey = ByteArray(32),
                receivingChainKey = chainKey,
                sendingRatchetKeyPair = ourRatchetKeyPair,
                receivingRatchetPublicKey = theirRatchetKey
            )
        }
    }

    fun encryptMessage(
        state: RatchetState,
        plaintext: String
    ): Pair<RatchetState, EncryptedMessage>? {
        return try {
            val messageKey = deriveMessageKey(state.sendingChainKey)
            val nextChainKey = deriveNextChainKey(state.sendingChainKey)

            val encrypted = MessageEncryption.encrypt(plaintext, messageKey)
                ?: throw Exception("Encryption failed")

            val newState = state.copy(
                sendingChainKey = nextChainKey,
                sendMessageCount = state.sendMessageCount + 1
            )

            val encryptedMessage = EncryptedMessage(
                ciphertext = encrypted,
                senderRatchetKey = Base64.encodeToString(
                    state.sendingRatchetKeyPair.publicKey.serialize(),
                    Base64.NO_WRAP
                ),
                messageIndex = state.sendMessageCount
            )

            Log.d(TAG, "Message encrypted with ratchet key #${state.sendMessageCount}")
            Pair(newState, encryptedMessage)

        } catch (e: Exception) {
            Log.e(TAG, "Ratchet encrypt failed: ${e.message}")
            null
        }
    }

    fun decryptMessage(
        state: RatchetState,
        encryptedMessage: EncryptedMessage
    ): Pair<RatchetState, String>? {
        return try {
            val messageKey = deriveMessageKey(state.receivingChainKey)
            val nextChainKey = deriveNextChainKey(state.receivingChainKey)

            val plaintext = MessageEncryption.decrypt(encryptedMessage.ciphertext, messageKey)
                ?: throw Exception("Decryption failed")

            val newState = state.copy(
                receivingChainKey = nextChainKey,
                receiveMessageCount = state.receiveMessageCount + 1
            )

            Log.d(TAG, "Message decrypted with ratchet key #${state.receiveMessageCount}")
            Pair(newState, plaintext)

        } catch (e: Exception) {
            Log.e(TAG, "Ratchet decrypt failed: ${e.message}")
            null
        }
    }

    fun dhRatchetStep(
        state: RatchetState,
        theirNewRatchetKey: ByteArray
    ): RatchetState {
        val theirPublicKey = Curve.decodePoint(theirNewRatchetKey, 0)
        val dhOutput = Curve.calculateAgreement(
            theirPublicKey,
            state.sendingRatchetKeyPair.privateKey
        )
        val (newRootKey, newReceivingChainKey) = deriveRootAndChainKey(
            state.rootKey,
            dhOutput
        )

        val newSendingKeyPair = Curve.generateKeyPair()

        val newDhOutput = Curve.calculateAgreement(
            theirPublicKey,
            newSendingKeyPair.privateKey
        )
        val (finalRootKey, newSendingChainKey) = deriveRootAndChainKey(
            newRootKey,
            newDhOutput
        )

        Log.d(TAG, "DH ratchet step performed")

        return state.copy(
            rootKey = finalRootKey,
            sendingChainKey = newSendingChainKey,
            receivingChainKey = newReceivingChainKey,
            sendingRatchetKeyPair = newSendingKeyPair,
            receivingRatchetPublicKey = theirNewRatchetKey,
            sendMessageCount = 0,
            receiveMessageCount = 0
        )
    }

    private fun deriveRootAndChainKey(
        rootKey: ByteArray,
        dhOutput: ByteArray
    ): Pair<ByteArray, ByteArray> {
        val derived = HKDF.deriveSecrets(
            dhOutput,
            rootKey,
            "CypherTextRatchet".toByteArray(),
            64
        )
        return Pair(
            derived.copyOfRange(0, 32),
            derived.copyOfRange(32, 64)
        )
    }

    private fun deriveMessageKey(chainKey: ByteArray): ByteArray {
        return hmacSha256(chainKey, byteArrayOf(0x01))
    }

    private fun deriveNextChainKey(chainKey: ByteArray): ByteArray {
        return hmacSha256(chainKey, byteArrayOf(0x02))
    }

    private fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(input)
    }
}

data class EncryptedMessage(
    val ciphertext: String,
    val senderRatchetKey: String,
    val messageIndex: Int
)