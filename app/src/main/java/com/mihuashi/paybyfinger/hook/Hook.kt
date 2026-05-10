package com.mihuashi.paybyfinger.hook

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.mihuashi.paybyfinger.BaseHook
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.hook.HookTool.Companion.convertTimestampToTime
import com.mihuashi.paybyfinger.hook.HookTool.Companion.decryptData
import com.mihuashi.paybyfinger.hook.HookTool.Companion.findParentByChild
import com.mihuashi.paybyfinger.hook.HookTool.Companion.getresId
import com.mihuashi.paybyfinger.hook.HookTool.Companion.isSixDigitNumber
import com.mihuashi.paybyfinger.tools.ConfigTools.AUTH_RESULT_ACTION
import com.mihuashi.paybyfinger.tools.ConfigTools.BASE_FRAGMENT_CLASS
import com.mihuashi.paybyfinger.tools.ConfigTools.BIOMETRIC_ACTIVITY_CLASS
import com.mihuashi.paybyfinger.tools.ConfigTools.INPUT_PASSWORD_DIALOG_CLASS
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_ALL_SWITCH
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_MI_SWITCH
import com.mihuashi.paybyfinger.tools.ConfigTools.MINE_SETTING_ITEM_VIEW_CLASS
import com.mihuashi.paybyfinger.tools.ConfigTools.MODULE_PACKAGE
import com.mihuashi.paybyfinger.tools.ConfigTools.PREF_NAME
import com.mihuashi.paybyfinger.tools.ConfigTools.SETTING_FRAGMENT_CLASS
import com.mihuashi.paybyfinger.tools.ConfigTools.xConfig
import com.mihuashi.paybyfinger.tools.DialogLifecycleOwner
import com.mihuashi.paybyfinger.tools.DialogSavedStateOwner
import com.mihuashi.paybyfinger.tools.DialogViewModelStoreOwner
import com.mihuashi.paybyfinger.tools.utils.ResInjectTool.injectModuleRes
import com.mihuashi.paybyfinger.ui.hook.HookSettingUI
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findClass
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


/**
 * 米画师 Xposed Hook 模块
 *
 * 核心功能：
 * 1. 拦截支付密码输入对话框，替换为指纹认证流程
 * 2. 在"个人设置"页面注入"指纹认证"入口菜单
 * 3. 通过广播接收指纹认证结果，自动填入支付密码
 *
 * 通信流程：
 *   原App弹出密码框 → Hook拦截 → 启动 BiometricAuthActivity（指纹验证）
 *   → 验证结果通过广播 AUTH_RESULT 返回 → 本模块接收并自动填入密码
 */
object Hook : BaseHook() {

    // ==================== 模块状态 ====================

    override val name: String = "米画师hook"

    /** 当前拦截到的支付密码输入对话框实例，用于在指纹认证成功后调用其 onPasswordEdited 方法 */
    var passwordDialog: Any? = null

    /** 当前支付金额（分） */
    private var paymentAmount: Int = 0

    /** 指纹认证启动时的时间戳，用于校验广播回调的时效性 */
    var paymentTimestamp: String = ""

    /** SharedPreferences 配置存储 */
    lateinit var sharedPreferences: SharedPreferences

    // ==================== 广播接收器 ====================

    /**
     * 指纹认证结果广播接收器
     *
     * 接收 BiometricAuthActivity 发送的认证结果广播，处理以下流程：
     * 认证成功 → 解密本地存储的密码 → 校验时间戳 → 调用密码框的 onPasswordEdited 方法
     * 认证失败 → 显示失败原因
     */
    val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val context = context ?: return
                val isAuthenticated = intent?.getBooleanExtra("result", false) ?: false
                val errorMessage = intent?.getStringExtra("error_message")
                val authTime = intent?.getLongExtra("timestamp", 0) as Long
                val payFromIntent = intent.getStringExtra("paytime") ?: ""

                val isMiNotificationEnabled = sharedPreferences.getBoolean(KEY_MI_SWITCH, false)

                if (BuildConfig.DEBUG) {
                    Log.i("指纹认证时间：${convertTimestampToTime(authTime)} ${context.javaClass.name}")
                }

                if (isAuthenticated) {
                    handleAuthSuccess(context, payFromIntent)
                } else {
                    handleAuthFailure(context, errorMessage, authTime)
                }

                // 如果启用了小米焦点通知，取消通知栏提示
                if (isMiNotificationEnabled) {
                    HookTool.cancelNotification(context)
                }
            } catch (e: Exception) {
                Log.e("广播接收器异常: ${e.message}")
            }
        }
    }

    // ==================== 广播处理辅助方法 ====================

    /**
     * 处理指纹认证成功
     *
     * 流程：解密密码 → 校验时间戳匹配 → 校验密码格式（六位数字）→ 调用密码框填入密码
     */
    private fun handleAuthSuccess(context: Context, payFromIntent: String) {
        Toast.makeText(
            context,
            "指纹认证成功，时间：${convertTimestampToTime(System.currentTimeMillis())}",
            Toast.LENGTH_SHORT
        ).show()

        // 获取密码框的 onPasswordEdited 方法
        val method = passwordDialog?.javaClass?.getDeclaredMethod(
            "onPasswordEdited", String::class.java
        )?.apply { isAccessible = true }

        // 校验密码是否已正确存储
        if (decryptData(alias, context, true) != "ok") {
            Toast.makeText(context, "解密失败，无法认证", Toast.LENGTH_SHORT).show()
            context.unregisterReceiver(resultReceiver)
            return
        }

        // 解密获取密码
        val decryptedPassword = decryptData(alias, context)

        if (BuildConfig.DEBUG) {
            Log.i("广播pay: $payFromIntent, 当前paytime: $paymentTimestamp")
        }

        // 校验时间戳是否匹配（防止旧的广播被误处理）
        if (paymentTimestamp != payFromIntent) {
            Toast.makeText(context, "校验失败，无法认证，或者是测试调用", Toast.LENGTH_SHORT).show()
            context.unregisterReceiver(resultReceiver)
            return
        }

        // 校验密码格式：必须为六位数字
        if (!isSixDigitNumber(decryptedPassword)) {
            Toast.makeText(context, "密码不是六位数字，无法认证", Toast.LENGTH_SHORT).show()
            context.unregisterReceiver(resultReceiver)
            return
        }

        // 所有校验通过，自动填入密码
        method?.invoke(passwordDialog, decryptedPassword)
        context.unregisterReceiver(resultReceiver)
    }

    /**
     * 处理指纹认证失败
     */
    private fun handleAuthFailure(context: Context, errorMessage: String?, authTime: Long) {
        val timeStr = convertTimestampToTime(authTime)
        Log.i("指纹认证失败，错误信息：$errorMessage，时间：$timeStr")
        Toast.makeText(
            context,
            "指纹认证失败，错误信息：$errorMessage，时间：$timeStr",
            Toast.LENGTH_SHORT
        ).show()
        context.unregisterReceiver(resultReceiver)
    }

    // ==================== Hook 初始化 ====================

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun init() {
        super.init()

        // Hook 360加固壳的 attachBaseContext，获取真实 Context 和 ClassLoader
        loadClassOrNull("com.stub.StubApp").isNotNull {
            it.methodFinder().first { name == "attachBaseContext" }.createHook {
                after { param ->
                    val context = param.args[0] as Context
                    injectModuleRes(context)
                    initHooksForApp(context)
                }
            }
        }
    }

    /**
     * 初始化所有 Hook 逻辑
     *
     * 在 App 的 attachBaseContext 之后调用，此时 ClassLoader 已就绪。
     * 注册以下 Hook：
     * 1. BaseFragment.onCreateView → 注入设置页菜单入口
     * 2. InputPayingPasswordDialog.setPayingPasswordContent → 拦截支付密码框，启动指纹认证
     * 3. InputPayingPasswordDialog.onPasswordEdited → 记录密码（调试用）
     */
    fun initHooksForApp(context: Context) {
        val classLoader = context.classLoader

        // 初始化 SharedPreferences 配置
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val passwordDialogClass = loadClass(INPUT_PASSWORD_DIALOG_CLASS, classLoader)

        // ---- Hook 1：拦截设置页面，注入"指纹认证"入口 ----
        loadClass(BASE_FRAGMENT_CLASS, classLoader)
            .methodFinder()
            .first { name == "onCreateView" }
            .createHook {
                after { param ->
                    val fragmentInstance = param.thisObject
                    // 仅在"个人-雇主设置"页面注入入口
                    if (fragmentInstance::class.java.name == SETTING_FRAGMENT_CLASS) {
                        if (BuildConfig.DEBUG) {
                            Log.i("目标Fragment: $fragmentInstance，隐藏设置: ${xConfig.hidesetting}")
                        }
                        if (!xConfig.hidesetting) {
                            injectSettingsMenuItem(fragmentInstance, classLoader)
                        }
                    }
                }
            }

        // ---- Hook 2：拦截支付密码框弹出，启动指纹认证 ----
        passwordDialogClass
            .methodFinder()
            .first { name == "setPayingPasswordContent" }
            .createHook {
                after { param ->
                    val allSwitchEnabled = sharedPreferences.getBoolean(KEY_ALL_SWITCH, false)
                    val isMiNotificationEnabled = sharedPreferences.getBoolean(KEY_MI_SWITCH, false)
                    val amount = param.args[1] as Int
                    val dialogInstance = param.thisObject

                    if (!allSwitchEnabled) return@after

                    if (BuildConfig.DEBUG) {
                        Log.i("密码框对象: $dialogInstance")
                    }

                    // 保存密码框实例，用于认证成功后自动填入密码
                    passwordDialog = dialogInstance

                    // 注册广播接收器，监听指纹认证结果
                    ContextCompat.registerReceiver(
                        context,
                        resultReceiver,
                        IntentFilter(AUTH_RESULT_ACTION),
                        ContextCompat.RECEIVER_EXPORTED
                    )

                    paymentAmount = amount

                    if (BuildConfig.DEBUG) {
                        Log.i("支付金额: $amount")
                    }

                    // 小米焦点通知：在通知栏显示支付金额
                    if (isMiNotificationEnabled) {
                        HookTool.sendNotification("支付:${amount}元", context)
                    }

                    // 延迟 5ms 启动指纹验证（等待密码框动画完成）
                    Handler(Looper.getMainLooper()).postDelayed({
                        startBiometricAuth(context)
                    }, 5)
                }
            }

        // ---- Hook 3：记录密码输入（仅用于调试日志） ----
        passwordDialogClass
            .methodFinder()
            .first { name == "onPasswordEdited" }
            .createHook {
                after { param ->
                    val allSwitchEnabled = sharedPreferences.getBoolean(KEY_ALL_SWITCH, false)
                    if (allSwitchEnabled) {
                        val password = param.args[0] as String
                        if (BuildConfig.DEBUG) {
                            Log.i("密码输入: $password")
                        }
                    }
                }
            }
    }

    // ==================== 指纹认证启动 ====================

    /**
     * 生成并启动指纹认证 Activity 的 Intent
     */
    fun createBiometricIntent(): Intent {
        return Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            putExtra("open", true)
            putExtra("rmb", paymentAmount)
            putExtra("paytime", paymentTimestamp)
            setComponent(
                ComponentName(MODULE_PACKAGE, BIOMETRIC_ACTIVITY_CLASS)
            )
        }
    }

    /**
     * 启动指纹认证流程
     *
     * 记录当前时间戳用于后续校验，然后启动 BiometricAuthActivity
     */
    private fun startBiometricAuth(context: Context) {
        paymentTimestamp = System.currentTimeMillis().toString()
        val intent = createBiometricIntent()
        context.startActivity(intent)
    }

    // ==================== 设置页面菜单注入 ====================

    /**
     * 在米画师"个人-雇主设置"页面注入"指纹认证"入口
     *
     * 通过反射获取页面的 rootView，在已有的设置列表中追加一个
     * "指纹认证"入口项，点击后弹出功能菜单对话框。
     */
    @SuppressLint("ResourceType", "UseSwitchCompatOrMaterialCode")
    fun injectSettingsMenuItem(fragmentInstance: Any, classLoader: ClassLoader) {
        try {
            // 通过反射获取页面根视图和 Context
            val rootView = XposedHelpers.getObjectField(fragmentInstance, "rootView") as? View
            val context = XposedHelpers.callMethod(fragmentInstance, "requireContext") as? Context
                ?: return
            val resources = context.resources
            val settingContainerId = getresId(resources, "mineSettingEmployerRss", "id")

            if (rootView == null) {
                Log.e("Xposed rootView 字段不存在或为空")
                return
            }

            if (BuildConfig.DEBUG) {
                Log.i("Xposed 成功获取 rootView: ${rootView.accessibilityClassName}")
            }

            // 加载米画师自定义的设置项 View 类
            val settingItemViewClass = findClass(MINE_SETTING_ITEM_VIEW_CLASS, classLoader)
            val settingContainer = rootView.findViewById<ViewGroup>(settingContainerId) as? FrameLayout
                ?: return

            if (rootView !is FrameLayout) return

            // 反射创建"指纹认证"设置项 View
            val newSettingItem = XposedHelpers.newInstance(settingItemViewClass, context) as ViewGroup
            val iconResId = getresId(resources, "svg_icon_install_manage", "drawable")

            // 配置设置项的标签、图标和样式
            XposedHelpers.setObjectField(newSettingItem, "label", "指纹认证")
            XposedHelpers.setIntField(newSettingItem, "iconRes", iconResId)
            XposedHelpers.callMethod(newSettingItem, "setCornerSide", 1)

            val text = TextView(context).apply {
                this.text = "指纹认证"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = android.view.Gravity.CENTER_VERTICAL
                val marginLeft = (10 * context.resources.displayMetrics.density).toInt()
                setPadding(marginLeft, 0, 0, 0)
            }

            newSettingItem.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            newSettingItem.addView(text)



            // 设置点击事件：弹出功能菜单对话框
            newSettingItem.setOnClickListener { view ->
                showFunctionMenuDialog(view.context)

            }

            // 将新设置项添加到页面设置列表中
            val parentGroup = findParentByChild(settingContainer) as LinearLayout
            parentGroup.addView(newSettingItem)

        } catch (e: NoSuchFieldError) {
            Log.e("Xposed 找不到 rootView 字段: $e")
        } catch (e: Exception) {
            Log.e("Xposed 注入设置页面时发生错误: ${e.message}")
        }
    }

    // ==================== 功能菜单对话框 ====================

    /**
     * 显示功能菜单对话框
     *
     * 包含以下功能项：
     * - 模块版本号：显示当前模块版本
     * - 总开关：启用/禁用指纹认证 Hook
     * - 小米焦点通知开关：是否在通知栏显示支付金额
     * - 测试调用：手动启动指纹认证（不经过支付流程）
     */
    private fun showFunctionMenuDialog(context: Context) {

        val composeView = ComposeView(context)
        val lifecycleOwner = DialogLifecycleOwner()
        val vmOwner = DialogViewModelStoreOwner()
        val savedStateOwner = DialogSavedStateOwner()

        // 关键：补齐 Compose 运行环境， Lifecycle/ViewModelStore/SavedState 所有者
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(vmOwner)
        composeView.setViewTreeSavedStateRegistryOwner(savedStateOwner)

        composeView.setContent {
            MiuixTheme(
                controller = remember { ThemeController(ColorSchemeMode.MonetSystem) },
                content = {
                    HookSettingUI(context)
                }

            )
        }

        android.app.Dialog(context).apply {
            setContentView(composeView)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                // 或者
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0.5f) // 背景遮罩透明度
            }
            show()
        }

    }

}
