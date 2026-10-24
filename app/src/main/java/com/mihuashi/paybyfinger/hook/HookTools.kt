package com.mihuashi.paybyfinger.hook

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.hyperfocus.api.FocusApi
import com.hyperfocus.api.IslandApi
import io.github.kyuubiran.ezxhelper.android.logging.Logger
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
const val CHANNEL_ID: String = "mhspay"

class HookTool {
    companion object {
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

        fun encryptData(alias: String, password: String, context: Context): ByteArray {
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

        private fun saveEncryptedDataToStorage(
            iv: ByteArray,
            encryptedData: ByteArray,
            context: Context
        ) {
            // 将 IV 和加密数据安全存储（可以用 SharedPreferences 或文件）
            val sharedPreferences =
                context.getSharedPreferences("SecureStorage", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("iv", Base64.encodeToString(iv, Base64.DEFAULT))
                putString("encryptedPassword", Base64.encodeToString(encryptedData, Base64.DEFAULT))
                apply()
            }
        }
        /** 解密数据
         * @param alias 为别名
         * @param context 为应用的context
         * @param test 为测试选项
         * @return [String] 正常模式为密码,测试成功为ok，测试失败为错误信息 */
        fun decryptData(alias: String, context: Context,test:Boolean = false): String {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey = keyStore.getKey(alias, null) as SecretKey

            // 获取存储的 IV 和加密数据
            val sharedPreferences =
                context.getSharedPreferences("SecureStorage", Context.MODE_PRIVATE)
            val iv = Base64.decode(sharedPreferences.getString("iv", ""), Base64.DEFAULT)
            val encryptedData =
                Base64.decode(sharedPreferences.getString("encryptedPassword", ""), Base64.DEFAULT)

            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decryptedData = cipher.doFinal(encryptedData)

                if (test) {
                    "ok"
                } else {
                    String(decryptedData, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Toast.makeText(context,"解密失败 原因$e",Toast.LENGTH_SHORT).show()
                "解密失败: ${e.message}"
            }
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

        fun savePasswordWithCallback(
            context: Context,
            password: String,
            callback: PasswordSaveCallback
        ) {
            try {
                val alias = "my_secure_password_key"
                createKey(alias)
                encryptData(alias, password, context)
                validatePasswordSave(context, password)
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
                savePassword(inputPassword, context)
                // 尝试解密以验证保存成功
                val decryptedPassword = decryptData(alias, context)
                if (inputPassword == decryptedPassword) {
                    Toast.makeText(
                        context,
                        "Password saved and verified successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Password save failed: verification mismatch!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "An error occurred while verifying the password.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        /** 获取资源id的
         * @param resources 为资源实例
         * @param name 为资源名称
         * @param def 为资源类型
         * @return [Int]为资源id*/
        @SuppressLint("DiscouragedApi")
        fun getresId(resources: android.content.res.Resources, name: String, def: String): Int {
            val resId = resources.getIdentifier(name, def, "com.qixin.mihuas")
            return resId
        }

        /** mhs设置圆角的，未找到触发函数待定 */
        private fun dp2Px(context: Context, dp: Float): Int {
            return (dpToPx(context, dp) + 0.5f).toInt()
        }

        /** mhs设置圆角的，未找到触发函数待定 */
        private fun dpToPx(context: Context, dp: Float): Float {
            val density = context.resources.displayMetrics.density
            return dp * density
        }

        /** mhs设置圆角的，未找到触发函数待定 */
        fun getCornerRadiiFromRange(z: Boolean, z2: Boolean, context: Context): FloatArray {
            val dp2Px = if (z) dp2Px(context, 10f) else 0
            val dp2Px2 = if (z) dp2Px(context, 10f) else 0
            val dp2Px3 = if (z2) dp2Px(context, 10f) else 0
            val dp2Px4 = if (z2) dp2Px(context, 10f) else 0
            return floatArrayOf(
                dp2Px.toFloat(), dp2Px.toFloat(), dp2Px2.toFloat(), dp2Px2.toFloat(),
                dp2Px4.toFloat(), dp2Px4.toFloat(), dp2Px3.toFloat(), dp2Px3.toFloat()
            )
        }

        /** 搜索为子视图[LinearLayout] 的 [ViewGroup]
         * @param view 为要搜索的视图 */
        fun findParentByChild(view: View): ViewGroup? {
            var parent = view.parent
            while (parent != null && parent !is LinearLayout) {
                parent = parent.parent
            }
            return parent as? LinearLayout
        }

        /** LogAllView获取所有的[View]
         *  @param view 是要用来获取的View */
        fun logAllViews(view: View) {
            // 输出当前视图的信息
            Logger.i("View: ${view.javaClass.simpleName}, ID: ${view.id}, Tag: ${view.tag}")

            // 如果视图是一个 ViewGroup，继续递归遍历子视图
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val childView = view.getChildAt(i)
                    logAllViews(childView)  // 递归遍历子视图
                }
            }
        }

        /**
         * 将时间戳转换为指定格式的字符串
         * @param timestamp 为时间戳 */
        fun convertTimestampToTime(timestamp: Long): String {
            // 创建 SimpleDateFormat 格式化时间
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            // 转换时间戳为 Date 对象
            val date = Date(timestamp)
            // 格式化为字符串
            return sdf.format(date)
        }

        /** 密码框实现
         * @param context 为应用的context */
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
                    val isPasswordVisible =
                        passwordInput.inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD == 0
                    if (isPasswordVisible) {
                        // 当前是明文显示，改为隐藏密码
                        passwordInput.inputType =
                            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                        confirmPasswordInput.inputType =
                            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
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
                        savePasswordWithCallback(context, confirmPassword, callback)
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

        /**
         * 小米澎湃焦点通知构建器 (高度自定义 DSL)
         */
        class FocusNotificationBuilder(private val context: Context) {
            var ticker: String = ""
            var title: String? = "付款通知"
            var content: String? = ""
            var picticker: Icon? = null
            var pictickerdark: Icon? = null

            // FocusApi 核心控制
            var enableFloat: Boolean = true      // 是否弹出横幅
            var timeout: Int = 280               // 超时时间(秒)
            var isShowNotification: Boolean = true // 是否在通知栏显示
            var islandFirstFloat: Boolean = true // 小岛是否首次弹出

            // 内部存储的 API 对象
            private var bigIslandArea: JSONObject? = null
            private var smallIslandArea: JSONObject? = null
            private var islandTemplate: JSONObject? = null
            var focusbundle: Bundle? = null

            /**
             * 配置大岛区域 (展开态)
             * @param leftPic 左侧图标路径或 KEY
             * @param leftTitle 左侧标题
             * @param rightText 右侧文字内容
             * @param type 类型 (通常 1 为左, 2 为右)
             */
            fun bigIsland(
                leftPic: String = "miui.focus.pic_ticker",
                leftTitle: String = title ?: "",
                rightText: String = content ?: "",
                type: Int = 2
            ) {
                bigIslandArea = IslandApi.bigIslandArea(
                    imageTextInfoLeft = IslandApi.imageTextInfo(
                        picInfo = IslandApi.picInfo(pic = leftPic),
                        textInfo = IslandApi.TextInfo(title = leftTitle)
                    ),
                    imageTextInfoRight = IslandApi.imageTextInfo(
                        textInfo = IslandApi.TextInfo(title = rightText),
                        type = type
                    )
                )
            }

            /**
             * 配置小岛区域 (胶囊态)
             * @param pic 小岛图标路径或 KEY
             */
            fun smallIsland(pic: String = "miui.focus.pic_ticker") {
                smallIslandArea = IslandApi.SmallIslandArea(
                    picInfo = IslandApi.picInfo(pic = pic)
                )
            }

            /**
             * 高级配置：直接设置小岛模板参数
             */
            fun islandTemplate(block: () -> JSONObject) {
                islandTemplate = block()
            }

            @SuppressLint("NotificationPermission")
            fun buildAndNotify(
                notificationId: Int = CHANNEL_ID.hashCode(),
                channelId: String = CHANNEL_ID
            ) {
                createNotificationChannel(context, channelId)

                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                val appIcon = context.packageManager.getActivityIcon(launchIntent!!).toBitmap()
                val finalPicticker = picticker ?: Icon.createWithBitmap(appIcon)

                // 如果没配置大/小岛，提供默认配置
                if (bigIslandArea == null) bigIsland()
                if (smallIslandArea == null) smallIsland()

                val finalIsland = islandTemplate ?: IslandApi.IslandTemplate(
                    bigIslandArea = bigIslandArea,
                    smallIslandArea = smallIslandArea,
                    islandPriority = 2,
                    islandTimeout = timeout
                )

                val bundle = FocusApi.sendFocus(
                    title = title,
                    content = content,
                    ticker = ticker.ifEmpty { content ?: "" },
                    picticker = finalPicticker,
                    pictickerdark = pictickerdark,
                    island = finalIsland,
                    enableFloat = enableFloat,
                    timeout = timeout,
                    isShowNotification = isShowNotification,
                    islandFirstFloat = islandFirstFloat
                )

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(IconCompat.createFromIcon(context, finalPicticker)!!)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setTicker(ticker)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            context, 0, launchIntent, PendingIntent.FLAG_MUTABLE
                        )
                    )
                    .addExtras(bundle)

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationId, if (focusbundle != null ) builder.addExtras(focusbundle).build() else builder.build()) // 只有首次才显示系统通知，后续更新只更新小岛
            }
        }

        /** 发送小米澎湃焦点通知
         * @param text 为通知内容
         * @param context 为应用的context */
        @SuppressLint("NotificationPermission")
        fun sendNotification(text: String, context: Context) {
            FocusNotificationBuilder(context).apply {
                content = text
                ticker = "支付通知: $text"
                bigIsland(rightText = text)
            }.buildAndNotify()
        }

        /** 创建通知渠道
         * @param context 为应用的context*/
        private fun createNotificationChannel(context: Context, channelId: String = CHANNEL_ID) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                channelId, "mhspay", NotificationManager.IMPORTANCE_MIN
            )
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        /** 关闭通知
         * @param context 为应用的context */
        @SuppressLint("NotificationPermission")
        fun cancelNotification(context: Context) {
                (context.getSystemService("notification") as NotificationManager).cancel(CHANNEL_ID.hashCode())
        }

        /** 注销广播 */
        fun unregisterReceiver(context: Context,br: BroadcastReceiver ){
            context.unregisterReceiver(br)
        }

        fun isSixDigitNumber(input: String): Boolean {
            return input.matches(Regex("^\\d{6}$"))
        }

    }
}


