package com.mihuashi.paybyfinger.hook


//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.hook.HookTool.Companion.convertTimestampToTime
import com.mihuashi.paybyfinger.hook.HookTool.Companion.decryptData
import com.mihuashi.paybyfinger.hook.HookTool.Companion.findParentByChild
import com.mihuashi.paybyfinger.hook.HookTool.Companion.getresId
import com.mihuashi.paybyfinger.hook.HookTool.Companion.showMaterialPasswordDialog
import com.mihuashi.paybyfinger.modulePath
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findClass


object Hook : BaseHook() {

    private var isReceiverRegistered: Boolean = false
    override val name: String = "米画师hook"
    var savedDialogObject: Any? = null // 用来保存对象
    private var rmb: Int = 0  // 用来保存对象
    private var uitext: Boolean = false //ui hook 确认
    lateinit var sharedPreferences: SharedPreferences
    var paytime: String = ""

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun init() {
        loadClassOrNull("com.stub.StubApp").isNotNull {
            it.methodFinder().first { name == "attachBaseContext" }.createHook {
                after { param ->
                    val context = param.args[0] as Context
                    val classLoader = context.classLoader
                    sharedPreferences = context.getSharedPreferences(
                        "mhshooksetting",
                        Context.MODE_PRIVATE
                    ) // 用来保存设置

                    val serviceIntent = Intent()
                    val resultReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val result = intent?.getBooleanExtra("result", false) ?: false
                            val errorMessage = intent?.getStringExtra("error_message")
                            val time = intent?.getLongExtra("timestamp", 0) as Long
                            val pay = intent.getStringExtra("paytime") as String
                            val fullClassName = context?.javaClass?.name
                            val miswitch = sharedPreferences.getBoolean("miswitch", false)
                            if (BuildConfig.DEBUG) {
                                Log.i("FingerprintAuth 认证时间：${convertTimestampToTime(time)} $fullClassName")
                            }
                            if (result) {
                                Toast.makeText(
                                    context,
                                    "FingerprintAuth 认证成功，时间：${convertTimestampToTime(time)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 获取你想要调用的方法
                                val method = savedDialogObject?.javaClass?.getDeclaredMethod(
                                    "onPasswordEdited",
                                    String::class.java
                                )
                                if (method != null) {
                                    method.isAccessible = true
                                }  // 确保方法是可以访问的
                                if (context?.let { it1 -> decryptData(alias, it1,true) } == "ok" ) {
                                    val pass = decryptData(alias, context)
                                    if (BuildConfig.DEBUG) {
                                        Log.i("pay: $pay paytime : $paytime")
                                    }
                                    // 调用方法并传递参数
                                    if (paytime == pay && pass != null) {
                                        method?.invoke(savedDialogObject, pass)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "校验失败，无法认证",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "解密失败，无法认证",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                if (miswitch) {
                                    context?.let { it1 -> HookTool.cancelNotification(it1) }
                                }
                            } else {
                                Log.i(
                                    "FingerprintAuth 认证失败，错误信息：$errorMessage，时间：${
                                        convertTimestampToTime(
                                            time
                                        )
                                    }"
                                )
                                Toast.makeText(
                                    context,
                                    "FingerprintAuth 认证失败，错误信息：$errorMessage，时间：${
                                        convertTimestampToTime(time)
                                    }",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                            if (miswitch) {
                                context?.let { it1 -> HookTool.cancelNotification(it1) }
                            }
                        }
                    }

                    // 创建 Intent 启动指纹服务
                    fun startFingerprintAuthentication() {
                        paytime = System.currentTimeMillis().toString()
                        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)// 添加此标志
                        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        serviceIntent.putExtra("open", true)
                        serviceIntent.putExtra("rmb", rmb)
                        serviceIntent.putExtra("paytime", paytime)
                        serviceIntent.setComponent(
                            ComponentName(
                                "com.mihuashi.paybyfinger",
                                "com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity"
                            )
                        )
                        //启动 BiometricAuthActivity
                        context.startActivity(serviceIntent)

                    }

                    loadClass(
                        "com.qixin.mihuas.core.mvvm.v.BaseFragment",
                        classLoader
                    ).methodFinder()
                        .first { name == "onCreateView" }
                        .createHook {
                            after { it ->
                                val fragmentInstance = it.thisObject
                                // 检查 fragmentInstance 是否为 MineSettingEmployerFragment 的实例
                                if (fragmentInstance::class.java.name == "com.qixin.mihuas.module.main.mine.setting.fragment.MineSettingEmployerFragment") {
                                    Log.i("名称 $fragmentInstance")
                                    executeCustomFunction(fragmentInstance, classLoader)

                                }
                            }
                        }
                    loadClass(
                        "com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog",
                        classLoader
                    ).methodFinder()
                        .first { name == "setPayingPasswordContent" }
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                // 获取当前的 InputPayingPasswordDialog 实例
                                val allswitch = sharedPreferences.getBoolean("allswitch", false)
                                val miswitch = sharedPreferences.getBoolean("miswitch", false)
                                val amount = param.args[1] as Int
                                val dialogInstance = param.thisObject
                                if (allswitch) {
                                    if (BuildConfig.DEBUG) {
                                        Log.i("对象 ：$dialogInstance")
                                    }
                                    // 保存对象
                                    savedDialogObject = dialogInstance
                                    if (!isReceiverRegistered) {
                                        // 注册广播接收器
                                        context.registerReceiver(
                                            resultReceiver,
                                            IntentFilter("com.mihuashi.paybyfinger.AUTH_RESULT")
                                        )
                                        isReceiverRegistered = true  // 标记接收器已注册
                                    }
                                    rmb = amount
                                    if (BuildConfig.DEBUG) {
                                        Log.i("付的多少钱：$amount")
                                    }
                                    if (miswitch) {
                                        HookTool.sendNotification("支付:$amount 元", context)
                                    }
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        startFingerprintAuthentication()  // 启动指纹验证
                                    }, 5)
                                }
                            }
                        }

                    loadClass(
                        "com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog",
                        classLoader
                    ).methodFinder()
                        .first { name == "onPasswordEdited" }
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                val allswitch = sharedPreferences.getBoolean("allswitch", false)
                                if (allswitch) {
                                    val password = param.args[0] as String
                                    if (BuildConfig.DEBUG) {
                                        Log.i("password : $password")
                                    }
                                }

                            }
                        }

                }
            }
        }
    }

    @SuppressLint("ResourceType")
    fun executeCustomFunction(fragmentInstance: Any, classLoader: ClassLoader) {
        try {
            // 获取 rootView 字段的值
            val rootView = XposedHelpers.getObjectField(fragmentInstance, "rootView") as? View
            val context = XposedHelpers.callMethod(fragmentInstance, "requireContext") as? Context
            val resources = context!!.resources
            val mineSettingEmployerRss = getresId(resources, "mineSettingEmployerRss", "id")

            if (rootView != null) {
                // rootView 存在，可以在这里进行进一步操作
                if (BuildConfig.DEBUG) {
                    Log.i("Xposed 成功获取 rootView")
                    Log.i("rootview 为 ${rootView.accessibilityClassName}")
                }
                // 使用反射加载 MineSettingItemView 类
                val mineSettingItemViewClass = findClass(
                    "com.qixin.mihuas.module.main.mine.widget.MineSettingItemView",
                    classLoader
                )
                val itemView =
                    rootView.findViewById<ViewGroup>(mineSettingEmployerRss) as FrameLayout


                // 检查 rootView 的类型，如果是 FrameLayout，可以添加新的视图
                if (rootView is FrameLayout) {
                    // 获取当前 Fragment 的 Context
                    if (context != null && BuildConfig.DEBUG) {
                        Log.i("context : ${context.javaClass.name}")
                        XposedHelpers.callMethod(
                            context.resources.assets,
                            "addAssetPath",
                            modulePath
                        )
                    }

                    if (context != null) {
                        // 创建 MineSettingItemView 实例，传入 Context
                        val newItemView =
                            XposedHelpers.newInstance(mineSettingItemViewClass, context) as View


                        // 设置 label 和 icon
                        XposedHelpers.setObjectField(newItemView, "label", "指纹认证")
                        XposedHelpers.setIntField(newItemView, "iconRes", 0x7f080993)
                        //XposedHelpers.setIntField(newItemView, "id", 0x7f090edc)
                        XposedHelpers.callMethod(newItemView, "setCornerSide", 1)
                        XposedHelpers.callMethod(newItemView, "componentInitialize")

                        // 设置布局参数，避免重叠，使用WRAP_CONTENT
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        //newItemView.id = 0x7f090edf// 使用资源 ID
                        newItemView.layoutParams = layoutParams

                        newItemView.setOnClickListener {
                            val cll = loadClass("com.qixin.mihuas.base.provider.ContainerActivity")
                            val intent = Intent(context, cll)
                            val containerArgsClass = findClass(
                                "com.qixin.mihuas.router.params.ContainerArgs",
                                classLoader
                            )
                            //val classcontainerExtras = findClass("com.qixin.mihuas.router.params.ContainerExtras",classLoader)
                            //val containerExtras = XposedHelpers.newInstance(classcontainerExtras,null,null,null,null,2131099708,2131099709,null,null,2131099708) as Parcelable?
                            val containerArgs = XposedHelpers.newInstance(
                                containerArgsClass,
                                "指纹认证",
                                "com.qixin.mihuas.module.setting.privacy.fragment.PrivacySettingFragment",
                                null,
                                false
                            ) as Parcelable?
                            val bundle = Bundle()
                            bundle.putParcelable("extraArgs", null)
                            bundle.putParcelable(
                                "args",
                                containerArgs
                            )  // 将 ContainerArgs 放入 Bundle
                            intent.putExtras(bundle)
                            context.startActivity(intent)
                            uitext = true
                            addui(classLoader, context)
                        }


                        val parentGroup = findParentByChild(itemView) as LinearLayout
                        // 添加新视图
                        parentGroup.addView(newItemView)
                        //logAllViews(rootView)
                    }
                }

            } else {
                Log.e("Xposed rootView 字段不存在或为空")
            }
        } catch (e: NoSuchFieldError) {
            Log.e("Xposed 找不到 rootView 字段: $e")
        } catch (e: Exception) {
            Log.e("Xposed 获取 rootView 字段时发生错误: ${e.message}")
        }
    }

    @SuppressLint("RestrictedApi", "ResourceType")
    fun addui(classLoader: ClassLoader, context: Context) {
        loadClass("com.qixin.mihuas.base.provider.ContainerActivity").methodFinder()
            .first { name == "initViews" }.createHook {
            after {
                val bctivity = it.thisObject as Activity
                val root = (bctivity.findViewById<ViewGroup>(R.id.content)!!).getChildAt(0) as View
                ///解决函数被执行时影响其他界面,已经解决了，该代码为获取标题参考
                //loadClass("com.qixin.mihuas.base.provider.ContainerActivity").methodFinder().first{ name == "initToolbar" }.createHook {
                //    after{
                //        // 获取传入的ContainerArgs对象
                //        val containerArgs: Any = it.args[0]
                //        try {
                //            val title = XposedHelpers.callMethod(containerArgs, "getTitle") as String
                //        } catch (e: NullPointerException) {
                //            Log.e("Xposed 获取 title 字段时发生错误: ${e.message}")
                //        }
                //    }
                //}
                // 处理获取到的标题内容
                //Log.i("标题栏内容: $uitext")
                if (BuildConfig.DEBUG) {
                    Log.i("名称 $bctivity")
                }
                //logAllViews(childView)
                if (uitext) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val compatLinearLayout = findParentByChild(root) as LinearLayout
                        val resources = context.resources
                        val container = getresId(resources, "container", "id")
                        val childView = compatLinearLayout.findViewById<LinearLayout>(container)
                        val subViewLinearLayout = childView.getChildAt(0) as LinearLayout
                        if (BuildConfig.DEBUG) {
                            Log.i("界面 ${subViewLinearLayout.javaClass.simpleName}")
                        }
                        subViewLinearLayout.removeAllViews()
                        setui(classLoader, subViewLinearLayout, context, bctivity)
                    }, 20)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("UseCompatLoadingForColorStateLists", "ResourceType")
    fun setui(
        classLoader: ClassLoader,
        subViewLinearLayout: LinearLayout,
        context: Context,
        bctivity: Activity
    ) {
        val resources = context.resources
        val allswitch = sharedPreferences.getBoolean("allswitch", false)
        val miswitch = sharedPreferences.getBoolean("miswitch", false)
        val nameclass =
            findClass("com.qixin.mihuas.resource.compat.text.CompatTextView", classLoader)
        val roundedLinearLayoutclass =
            findClass("com.qixin.mihuas.resource.widget.RoundedLinearLayout", classLoader)
        val roundedRelativeLayoutclass =
            findClass("com.qixin.mihuas.resource.widget.RoundedRelativeLayout", classLoader)
        val viewa =
            findClass("com.qixin.mihuas.module.main.mine.widget.MineSettingItemView", classLoader)
        val switchclass = findClass(
            "com.qixin.mihuas.resource.widget.text.switchbutton.SwitchButtonView",
            classLoader
        )

        val name = XposedHelpers.newInstance(nameclass, context) as TextView
        val nameone = XposedHelpers.newInstance(nameclass, context) as TextView
        val nametwo = XposedHelpers.newInstance(nameclass, context) as TextView
        val roundedRelativeLayout =
            XposedHelpers.newInstance(roundedRelativeLayoutclass, context) as RelativeLayout
        val roundedRelativeLayouta =
            XposedHelpers.newInstance(roundedRelativeLayoutclass, context) as RelativeLayout
        val roundedLinearLayout =
            XposedHelpers.newInstance(roundedLinearLayoutclass, context) as LinearLayout

        val newItemView = XposedHelpers.newInstance(viewa, context) as ViewGroup
        val setpassword = XposedHelpers.newInstance(viewa, context) as ViewGroup

        // 创建带有指定 style 的上下文，米画师默认开关样式
        val switchNormal = getresId(resources, "SwitchNormal", "style")
        val themedContext = ContextThemeWrapper(context, switchNormal)
        val switch = XposedHelpers.newInstance(switchclass, themedContext) as CompoundButton
        val switcha = XposedHelpers.newInstance(switchclass, themedContext) as CompoundButton

        // 获取资源 ID
        val colorInfo60 = getresId(resources, "colorInfo60", "color")
        val colorInfo80 = getresId(resources, "colorInfo80", "color")
        val txt32 = getresId(resources, "txt_32", "dimen")
        val dp15 = getresId(resources, "dp_15", "dimen")
        val interval_normal = getresId(resources, "interval_normal", "dimen")
        val interval_small = getresId(resources, "interval_small", "dimen")
        val svg_icon_install_manage = getresId(resources, "svg_icon_install_manage", "drawable")


        name.text = "基础设置"
        name.textSize = 12.0F
        //name.setTextColor(resources.getColor(colorInfo60))
        val namecolorStateList = ContextCompat.getColorStateList(context, colorInfo60)
        name.setTextColor(namecolorStateList)
        name.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = resources.getDimensionPixelSize(interval_small)
            leftMargin = resources.getDimensionPixelSize(interval_normal)
        }

        roundedLinearLayout.orientation = LinearLayout.VERTICAL
        roundedLinearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = resources.getDimensionPixelSize(interval_normal)  // 设置左侧外边距
            topMargin = resources.getDimensionPixelSize(interval_small) // 设置顶部外边距
            rightMargin = resources.getDimensionPixelSize(interval_normal)  // 设置右侧外边距
            bottomMargin = resources.getDimensionPixelSize(interval_small)  // 设置底部外边距
        }


        /** 设置 label 和 icon
         * newItemView 为 MineSettingItemView */
        XposedHelpers.setObjectField(newItemView, "label", "模块版本号")
        XposedHelpers.setIntField(newItemView, "iconRes", svg_icon_install_manage)
        XposedHelpers.callMethod(newItemView, "setCornerSide", 0)
        XposedHelpers.callMethod(newItemView, "componentInitialize")

        /** 设置 label 和 icon 设置 密码输入选项 */
        XposedHelpers.setObjectField(setpassword, "label", "设置密码")
        XposedHelpers.setIntField(setpassword, "iconRes", svg_icon_install_manage)
        XposedHelpers.callMethod(setpassword, "componentInitialize")

        val frameLayout = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val frameLayouta = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 设置布局参数，避免重叠，使用WRAP_CONTENT
        val layoutParamsaa = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        //newItemView.id = 0x7f090edf// 使用资源 ID
        newItemView.layoutParams = layoutParamsaa
        setpassword.layoutParams = layoutParamsaa
        newItemView.setOnClickListener {
            Toast.makeText(context, "模块版本号为 ${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT)
                .show()
        }
        setpassword.setOnClickListener {
            showMaterialPasswordDialog(bctivity)
        }

        //////// 开关构建
        roundedRelativeLayout.setPadding(
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt()
        )

        //////// 开关构建
        roundedRelativeLayouta.setPadding(
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt(),
            resources.getDimension(dp15).toInt()
        )

        // 创建文本视图 (CompatTextView)
        // 设置文本大小
        nameone.textSize =
            resources.getDimension(txt32) / context.resources.displayMetrics.scaledDensity
        nameone.text = "总开关"
        // 设置文本样式为粗体
        nameone.setTypeface(null, Typeface.BOLD)
        // 设置文本颜色
        //nameone.setTextColor(resources.getColorStateList(colorInfo80))
        val colorStateList = ContextCompat.getColorStateList(context, colorInfo80)
        nameone.setTextColor(colorStateList)


        // 设置文本大小
        nametwo.textSize =
            resources.getDimension(txt32) / context.resources.displayMetrics.scaledDensity
        nametwo.text = "焦点通知金额开关（仅小米可用）"
        // 设置文本样式为粗体
        nametwo.setTypeface(null, Typeface.BOLD)
        // 设置文本颜色
        nametwo.setTextColor(colorStateList)


        // 设置布局参数：垂直居中
        val paramsa = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        paramsa.addRule(RelativeLayout.CENTER_VERTICAL)
        nameone.layoutParams = paramsa
        nametwo.layoutParams = paramsa


        val paramsb = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        paramsb.addRule(RelativeLayout.CENTER_VERTICAL)
        paramsb.addRule(RelativeLayout.ALIGN_PARENT_END)
        switch.layoutParams = paramsb
        switch.isChecked = allswitch
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putBoolean("allswitch", true)
                    apply()
                }
                roundedLinearLayout.addView(setpassword)
            } else {
                with(sharedPreferences.edit()) {
                    putBoolean("allswitch", false)
                    apply()
                }
            }
        }

        switcha.layoutParams = paramsb
        switcha.isChecked = miswitch
        switcha.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putBoolean("miswitch", true)
                    apply()
                }
            } else {
                with(sharedPreferences.edit()) {
                    putBoolean("miswitch", false)
                    apply()
                }
            }
        }
        // 将 TextView 和 SwitchButton 添加到容器中
        roundedRelativeLayout.addView(nameone)
        roundedRelativeLayout.addView(switch)

        // 将 TextView 和 SwitchButton 添加到容器中
        roundedRelativeLayouta.addView(nametwo)
        roundedRelativeLayouta.addView(switcha)

        ///////开关构建完成
        frameLayout.addView(roundedRelativeLayout)
        frameLayouta.addView(roundedRelativeLayouta)

        roundedLinearLayout.addView(name)  // 添加标题文本
        roundedLinearLayout.addView(newItemView) //添加模块版本号
        roundedLinearLayout.addView(frameLayout)// 添加开关
        roundedLinearLayout.addView(frameLayouta)
        if (allswitch) {
            roundedLinearLayout.addView(setpassword) //添加设置密码模块
        }

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0, // 左
                resources.getDimensionPixelSize(interval_normal), // 上
                0, // 右
                resources.getDimensionPixelSize(interval_normal) // 下
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
            }
        }

        linearLayout.addView(roundedLinearLayout)
        subViewLinearLayout.addView(linearLayout)
        uitext = false

    }
}
