package com.mihuashi.paybyfinger.ui.activity

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.AppTask
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.hook.HookTool.Companion.convertTimestampToTime
import java.util.concurrent.Executor


class BiometricAuthActivity : FragmentActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private val resultIntent = Intent()
    var paytime :String = ""
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
        paytime = intent.getStringExtra("paytime").toString()
        if (BuildConfig.DEBUG) {
            Log.i("mihuashihook", "钱$rmb")
            Log.i("mihuashihook", "钱$open")
        }
        val systemService = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appTasks: List<AppTask> = systemService.getAppTasks()
        appTasks.get(0).setExcludeFromRecents(true);//设置activity是否隐藏
        // 判断是否传递过来数据
        if (open) {
            // 如果rmb不为null，表示有数据传递过来
            val promptInfo: BiometricPrompt.PromptInfo = getPromptInfo(
                title = "您将支付 $rmb 元",
                subtitle = "请验证您的身份, 请求时间为${convertTimestampToTime(paytime.toLong())}"
            )
            Handler(Looper.getMainLooper()).postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 5)
        } else {
            val promptInfo: BiometricPrompt.PromptInfo = getPromptInfo()
            Handler(Looper.getMainLooper()).postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 5)
        }
    }

    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // 认证成功，插入额外信息
            val values = ContentValues().apply {
                put("result", true)
                put("timestamp", System.currentTimeMillis()) // 保存认证时间戳
                put("paytime", paytime) // 回传启动时间校验

            }
            contentResolver.insert(Uri.parse("content://com.mihuashi.paybyfinger.provider/results"), values)
            resultIntent.putExtra("result", false)
            resultIntent.putExtra("timestamp", System.currentTimeMillis())
            // 认证成功
            val resultIntent = Intent("com.mihuashi.paybyfinger.AUTH_RESULT").apply {
                putExtra("result", true)
                putExtra("error_message", "null")
                putExtra("timestamp", System.currentTimeMillis())
                putExtra("paytime", paytime) // 回传启动时间校验
            }
            sendBroadcast(resultIntent) // 发送广播
            finish()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            // 认证错误
            Log.e("BiometricAuthActivity", "Authentication error: $errString code: $errorCode")
            val values = ContentValues().apply {
                put("result", false)
                put("error_message", errString.toString()) // 保存错误信息
                put("timestamp", System.currentTimeMillis()) // 保存认证时间戳
                put("paytime", paytime) // 回传启动时间校验
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
                putExtra("paytime", paytime) // 回传启动时间校验
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
                put("paytime", paytime) // 回传启动时间校验
            }
            contentResolver.insert(Uri.parse("content://com.mihuashi.paybyfinger.provider/results"), values)
            setResult(Activity.RESULT_CANCELED)
            resultIntent.putExtra("result", false)
            resultIntent.putExtra("error_message", "认证失败")
            val resultIntent = Intent("com.mihuashi.paybyfinger.AUTH_RESULT").apply {
                putExtra("result", false)
                putExtra("error_message", "未知错误")
                putExtra("timestamp", System.currentTimeMillis())
                putExtra("paytime", paytime) // 回传启动时间校验
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
