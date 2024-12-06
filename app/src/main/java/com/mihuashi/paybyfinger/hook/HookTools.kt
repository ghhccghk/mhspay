package com.mihuashi.paybyfinger.hook

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputFilter
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.github.kyuubiran.ezxhelper.Log
import org.json.JSONObject
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

val alias = "my_secure_password_key"
const val CHANNEL_ID: String = "channel_id_focusNotifpay"
class HookTool {
    companion object    {
    private fun createKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM) // 使用 GCM 模式
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) // 无填充
            .setUserAuthenticationRequired(false) // 需要用户认证（如指纹/密码）
            .setKeySize(256) // 密钥大小
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encryptData(alias: String, password: String,context: Context): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv // 初始化向量（需要保存下来以便解密时使用）
        val encryptedData = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

        // 存储加密数据和 IV
        saveEncryptedDataToStorage(iv, encryptedData, context)

        return encryptedData
    }

    private fun saveEncryptedDataToStorage(iv: ByteArray, encryptedData: ByteArray, context: Context) {
        // 将 IV 和加密数据安全存储（可以用 SharedPreferences 或文件）
        val sharedPreferences = context.getSharedPreferences("SecureStorage", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("iv", Base64.encodeToString(iv, Base64.DEFAULT))
            putString("encryptedPassword", Base64.encodeToString(encryptedData, Base64.DEFAULT))
            apply()
        }
    }

    fun decryptData(alias: String,context: Context): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKey = keyStore.getKey(alias, null) as SecretKey

        // 获取存储的 IV 和加密数据
        val sharedPreferences = context.getSharedPreferences("SecureStorage", Context.MODE_PRIVATE)
        val iv = Base64.decode(sharedPreferences.getString("iv", ""), Base64.DEFAULT)
        val encryptedData = Base64.decode(sharedPreferences.getString("encryptedPassword", ""), Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedData = cipher.doFinal(encryptedData)

        return String(decryptedData, Charsets.UTF_8)
    }

private fun savePassword(password: String, context: Context): Boolean {
    return try {
        // 加密密码并存储
        val alias = "my_secure_password_key"
        createKey(alias) // 如果密钥不存在，先生成密钥
        encryptData(alias, password, context)
        // 如果上述操作成功，返回 true
        true
    } catch (e: Exception) {
        e.printStackTrace()
        // 如果保存失败，返回 false
        false
    }
}

interface PasswordSaveCallback {
    fun onSuccess()
    fun onFailure(error: String)
}

private fun savePasswordWithCallback(context: Context, password: String, callback: PasswordSaveCallback) {
    try {
        val alias = "my_secure_password_key"
        createKey(alias)
        encryptData(alias, password,context)
        validatePasswordSave(context,password)
        // 保存成功，触发成功回调
        callback.onSuccess()
    } catch (e: Exception) {
        e.printStackTrace()
        // 保存失败，触发失败回调
        callback.onFailure("Failed to save password: ${e.message}")
    }
}



private fun validatePasswordSave(context: Context, inputPassword: String) {
    try {
        // 先保存密码
        savePassword(inputPassword,context)
        // 尝试解密以验证保存成功
        val decryptedPassword = decryptData(alias,context)
        if (inputPassword == decryptedPassword) {
            Toast.makeText(context, "Password saved and verified successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Password save failed: verification mismatch!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "An error occurred while verifying the password.", Toast.LENGTH_SHORT).show()
    }
}



@SuppressLint("DiscouragedApi")
    fun getresId (resources:android.content.res.Resources, name: String, def:String): Int {
        // 获取资源ID的示例，获取名为"name"的字符串资源ID
        val resId = resources.getIdentifier(name, def, "com.qixin.mihuas")
        return resId
    }

    private fun dp2Px(context: Context, dp: Float): Int {
        return (dpToPx(context, dp) + 0.5f).toInt()
    }

    private fun dpToPx(context: Context, dp: Float): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    ///mhs设置圆角的，未找到触发函数待定
    fun getCornerRadiiFromRange(z: Boolean, z2: Boolean, context: Context): FloatArray {
        val dp2Px = if (z) dp2Px(context, 10f) else 0
        val dp2Px2 = if (z) dp2Px(context, 10f) else 0
        val dp2Px3 = if (z2) dp2Px(context, 10f) else 0
        val dp2Px4 = if (z2) dp2Px(context, 10f) else 0
        return floatArrayOf(dp2Px.toFloat(), dp2Px.toFloat(), dp2Px2.toFloat(), dp2Px2.toFloat(),
            dp2Px4.toFloat(), dp2Px4.toFloat(), dp2Px3.toFloat(), dp2Px3.toFloat())
    }

    fun findParentByChild(view: View): ViewGroup? {
        var parent = view.parent
        while (parent != null && parent !is LinearLayout) {
            parent = parent.parent
        }
        return parent as? LinearLayout
    }



     fun logAllViews(view: View) {
        // 输出当前视图的信息
        Log.i("View: ${view.javaClass.simpleName}, ID: ${view.id}, Tag: ${view.tag}")

        // 如果视图是一个 ViewGroup，继续递归遍历子视图
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val childView = view.getChildAt(i)
                logAllViews(childView)  // 递归遍历子视图
            }
        }
    }


    fun convertTimestampToTime(timestamp: Long): String {
        // 创建 SimpleDateFormat 格式化时间
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        // 转换时间戳为 Date 对象
        val date = Date(timestamp)
        // 格式化为字符串
        return sdf.format(date)
    }

    fun showMaterialPasswordDialog(context: Context) {
        // 设置密码是否默认可见
        val isPasswordInitiallyVisible = false

        // 创建容器 Layout
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12) // 设置内边距（单位：px）
        }

        // 创建密码输入框
        val passwordInput = EditText(context).apply {
            hint = "输入密码 (6 位)"
            //inputType = android.text.InputType.TYPE_CLASS_NUMBER // 限制为数字
            inputType = if (isPasswordInitiallyVisible) {
                android.text.InputType.TYPE_CLASS_NUMBER // 初始状态为明文显示
            } else {
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD // 初始状态为密码隐藏
            }
            filters = arrayOf(InputFilter.LengthFilter(6)) // 限制长度为6位
            setPadding(10, 16, 10, 16) // 内部边距（上下留白）
        }

        // 创建确认密码输入框
        val confirmPasswordInput = EditText(context).apply {
            hint = "确认密码 (6 位)"
            //inputType = android.text.InputType.TYPE_CLASS_NUMBER // 限制为数字
            inputType = if (isPasswordInitiallyVisible) {
                android.text.InputType.TYPE_CLASS_NUMBER // 初始状态为明文显示
            } else {
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD // 初始状态为密码隐藏
            }
            filters = arrayOf(InputFilter.LengthFilter(6)) // 限制长度为6位
            setPadding(10, 16, 10, 16) // 内部边距
        }

        // 创建显示/隐藏密码按钮
        val togglePasswordVisibilityButton = Button(context).apply {
            // 根据初始状态设置按钮文本
            text = if (isPasswordInitiallyVisible) "隐藏密码" else "显示密码"
            setOnClickListener {
                // 切换密码显示状态
                val isPasswordVisible = passwordInput.inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD == 0
                if (isPasswordVisible) {
                    // 当前是明文显示，改为隐藏密码
                    passwordInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    confirmPasswordInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    text = "显示密码"
                } else {
                    // 当前是隐藏状态，改为明文显示
                    passwordInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    confirmPasswordInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    text = "隐藏密码"
                }
                // 保持光标位置
                passwordInput.setSelection(passwordInput.text.length)
                confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
            }
        }

        // 将输入框添加到容器
        container.addView(passwordInput)
        container.addView(confirmPasswordInput)
        container.addView(togglePasswordVisibilityButton)

        // 创建 AlertDialog
        val dialog = AlertDialog.Builder(context)
            .setTitle("设置密码") // 标题
            .setView(container)// 设置自定义 View（容器）
            .setPositiveButton("确定") { _, _ ->
                val password = passwordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()
                if (password.length == 6 && password == confirmPassword) {
                    val callback = object : PasswordSaveCallback {
                        override fun onSuccess() {
                            Toast.makeText(context, "密码设置成功!", Toast.LENGTH_SHORT).show()
                        }
                        override fun onFailure(error: String) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    savePasswordWithCallback(context,confirmPassword, callback)
                } else if (password.length != 6) {
                    Toast.makeText(context, "密码需要6位数字", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "密码设置失败！", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("关闭") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // 设置对话框为安全对话框
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // 在对话框关闭时移除安全标志
        dialog.setOnDismissListener {
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // 显示对话框
        dialog.show()
    }


        @SuppressLint("NotificationPermission")
        fun sendNotification(text: String,context: Context) {
            //  logE("sendNotification: " + context.packageName + ": " + text)
            createNotificationChannel(context)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val bitmap = context.packageManager.getActivityIcon(launchIntent!!).toBitmap()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            builder.setContentTitle(text)
            builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
            builder.setTicker(text).setPriority(NotificationCompat.PRIORITY_LOW)
            builder.setOngoing(true) // 设置为常驻通知
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context, 0, launchIntent, PendingIntent.FLAG_MUTABLE
                )
            )
            val jSONObject = JSONObject()
            val jSONObject3 = JSONObject()
            val jSONObject4 = JSONObject()
            jSONObject4.put("type", 1)
            jSONObject4.put("title", text)
            jSONObject3.put("baseInfo", jSONObject4)
            jSONObject3.put("ticker", text)
            jSONObject3.put("tickerPic", "miui.focus.pic_ticker")
            jSONObject3.put("tickerPicDark", "miui.focus.pic_ticker_dark")

            jSONObject.put("param_v2", jSONObject3)
            val bundle = Bundle()
            bundle.putString("miui.focus.param", jSONObject.toString())
            val bundle3 = Bundle()
            bundle3.putParcelable(
                "miui.focus.pic_ticker", Icon.createWithBitmap(bitmap)
            )
            bundle3.putParcelable(
                "miui.focus.pic_ticker_dark", Icon.createWithBitmap(bitmap)
            )
            bundle.putBundle("miui.focus.pics", bundle3)
            builder.addExtras(bundle)
            val notification = builder.build()
            (context.getSystemService("notification") as NotificationManager).notify(
                CHANNEL_ID.hashCode(), notification
            )
        }

        private fun createNotificationChannel(context: Context) {
            val notificationManager = context.getSystemService("notification") as NotificationManager
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, "mhspay", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }


        @SuppressLint("NotificationPermission")
        fun cancelNotification(context: Context) {
            (context.getSystemService("notification") as NotificationManager).cancel(CHANNEL_ID.hashCode())
        }
    }
}


