package com.vahitkeskin.targetping.utils

import android.content.Context
// DİKKAT: Doğru import burasıdır. android.hardware DEĞİL, androidx.biometric OLMALI.
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class BiometricManager @Inject constructor(
    @ActivityContext private val context: Context
) {
    fun authenticate(
        activity: FragmentActivity,
        title: String = "TargetPing Güvenlik",
        subtitle: String = "Devam etmek için doğrulama yapın",
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        // Artık androidx import edildiği için bu constructor hata vermeyecek
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Kullanıcı iptal etti veya hata oluştu
                    onError()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Parmak izi okunamadı ama deneme hakkı devam ediyor
                }
            })

        // PromptInfo da artık kırmızı yanmayacak
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("İptal")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}