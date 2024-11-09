package com.mihuashi.paybyfinger.ui.activity

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mihuashi.paybyfinger.service.FingerprintService
import java.util.concurrent.Executor

class BiometricAuthActivity : FragmentActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executor: Executor = ContextCompat.getMainExecutor(this)

        // 初始化 BiometricPrompt
        biometricPrompt = BiometricPrompt(this, executor, authenticationCallback)

    }

    override fun onResume() {
        super.onResume()
        val open = intent.getBooleanExtra("open", false)
        val rmb = intent.getIntExtra("rmb",0)
        Log.i("mihuashihook", "钱$rmb")
        Log.i("mihuashihook", "钱$open")
        // 判断是否传递过来数据
        if (open) {
            // 如果rmb不为null，表示有数据传递过来
            val promptInfo: BiometricPrompt.PromptInfo = getPromptInfo(
                title = "您将支付 $rmb 元",
                subtitle = "请验证您的身份"
            )
            Handler(Looper.getMainLooper()).postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 50)
        } else {
            val promptInfo: BiometricPrompt.PromptInfo = getPromptInfo()
            Handler(Looper.getMainLooper()).postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 50)
        }
    }

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // 认证成功，插入额外信息
            Toast.makeText(this@BiometricAuthActivity, "认证成功", Toast.LENGTH_SHORT).show()
            val values = ContentValues().apply {
                put("result", true)
                put("timestamp", System.currentTimeMillis()) // 保存认证时间戳
            }
            contentResolver.insert(Uri.parse("content://com.mihuashi.paybyfinger.provider/results"), values)
            resultIntent.putExtra("result", false)
            resultIntent.putExtra("timestamp", System.currentTimeMillis())
            // 认证成功
            val resultIntent = Intent("com.mihuashi.paybyfinger.AUTH_RESULT").apply {
                putExtra("result", true)
                putExtra("error_message", "null")
                putExtra("timestamp", System.currentTimeMillis())
            }
            sendBroadcast(resultIntent) // 发送广播
            finish()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            // 认证错误
            Log.e("BiometricAuthActivity", "Authentication error: $errString")
            Log.e("BiometricAuthActivity", "Authentication error code: $errorCode")
            //Toast.makeText(this@BiometricAuthActivity, "认证错误: $errString", Toast.LENGTH_SHORT).show()
            val values = ContentValues().apply {
                put("result", false)
                put("error_message", errString.toString()) // 保存错误信息
                put("timestamp", System.currentTimeMillis()) // 保存认证时间戳
            }
            contentResolver.insert(Uri.parse("content://com.mihuashi.paybyfinger.provider/results"), values)
            resultIntent.putExtra("result", false)
            resultIntent.putExtra("error_message", errString.toString())
            resultIntent.putExtra("timestamp", System.currentTimeMillis())
            setResult(Activity.RESULT_CANCELED, Intent().putExtra("error", errString.toString()))

            val resultIntent = Intent("com.mihuashi.paybyfinger.AUTH_RESULT").apply {
                putExtra("result", false)
                putExtra("error_message", errString.toString())
                putExtra("timestamp", System.currentTimeMillis())
            }
            sendBroadcast(resultIntent) // 发送广播
            finish()
        }

        override fun onAuthenticationFailed() {
            // 认证失败
            //Toast.makeText(this@BiometricAuthActivity, "认证失败", Toast.LENGTH_SHORT).show()
            val values = ContentValues().apply {
                put("result", false)
                put("error_message", "认证失败") // 保存错误信息
                put("timestamp", System.currentTimeMillis()) // 保存认证时间戳
            }
            contentResolver.insert(Uri.parse("content://com.mihuashi.paybyfinger.provider/results"), values)
            setResult(Activity.RESULT_CANCELED)
            resultIntent.putExtra("result", false)
            resultIntent.putExtra("error_message", "认证失败")
            val resultIntent = Intent("com.mihuashi.paybyfinger.AUTH_RESULT").apply {
                putExtra("result", false)
                putExtra("error_message", "未知错误")
                putExtra("timestamp", System.currentTimeMillis())
            }
            sendBroadcast(resultIntent) // 发送广播
            finish()
        }
    }
    // 封装函数，根据条件返回不同的 PromptInfo
    fun getPromptInfo(
        title: String = "生物识别认证",
        subtitle: String = "请验证您的身份",
        negativeButtonText: String = "取消"
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()
    }
}
