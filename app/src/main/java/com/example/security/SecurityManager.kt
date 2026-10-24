package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityManager {

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    fun hashPin(pin: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(hexToBytes(saltHex))
        val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashedBytes)
    }

    fun verifyPin(enteredPin: String, storedSaltHex: String, storedHashHex: String): Boolean {
        val computed = hashPin(enteredPin, storedSaltHex)
        return computed.equals(storedHashHex, ignoreCase = true)
    }

    fun hashSecret(secret: String, saltHex: String): String = hashPin(secret, saltHex)

    fun verifySecret(enteredSecret: String, storedSaltHex: String, storedHashHex: String): Boolean =
        verifyPin(enteredSecret, storedSaltHex, storedHashHex)

    fun canAuthenticateBiometrics(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            canAuth == BiometricManager.BIOMETRIC_SUCCESS
        } catch (t: Throwable) {
            false
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock OfflineQR",
        subtitle: String = "Authenticate using fingerprint or face",
        negativeButtonText: String = "Use Passcode",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Authentication failed (e.g. wrong finger), prompt remains open
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (t: Throwable) {
            onError(t.localizedMessage ?: "Biometric prompt unavailable")
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
