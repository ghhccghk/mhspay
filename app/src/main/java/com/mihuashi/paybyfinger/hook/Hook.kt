package com.mihuashi.paybyfinger.hook


import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.mihuashi.paybyfinger.modulePath
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findClass


object Hook : BaseHook() {

    private var isReceiverRegistered: Boolean = false
    override val name: String = "米画师hook"
    var savedDialogObject: Any? = null // 用来保存对象
    var rmb: Int = 0  // 用来保存对象

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun init() {
       loadClassOrNull("com.stub.StubApp").isNotNull {
            it.methodFinder().first { name == "attachBaseContext" }.createHook {
                after { param ->
                    val context = param.args[0] as Context
                    val classLoader = context.classLoader
                    val serviceIntent = Intent()

                    val resultReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val result = intent?.getBooleanExtra("result", false) ?: false
                            val errorMessage = intent?.getStringExtra("error_message")
                            val time = intent?.getLongExtra("timestamp", 0).toString()
                            val fullClassName = context?.javaClass?.name
                            Log.i("FingerprintAuth 认证时间：$time $fullClassName")
                            if (result) {
                                Toast.makeText(
                                    context,
                                    "FingerprintAuth 认证成功，时间：$time",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 获取你想要调用的方法
                                val method = savedDialogObject?.javaClass?.getDeclaredMethod("onPasswordEdited", String::class.java)
                                if (method != null) {
                                    method.isAccessible = true
                                }  // 确保方法是可以访问的

                                // 调用方法并传递参数
                                if (method != null) {
                                    method.invoke(savedDialogObject, "123444")
                                }

                            } else {
                                    Log.i("FingerprintAuth 认证失败，错误信息：$errorMessage，时间：$time")
                                    Toast.makeText(context, "FingerprintAuth 认证失败，错误信息：$errorMessage，时间：$time", Toast.LENGTH_SHORT).show()

                            }
                        }
                    }

                    // 创建 Intent 启动指纹服务
                    fun startFingerprintAuthentication() {
                        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)// 添加此标志
                        serviceIntent.putExtra("open",true)
                        serviceIntent.putExtra("rmb", rmb)
                        serviceIntent.setComponent(
                            ComponentName(
                                "com.mihuashi.paybyfinger",
                                "com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity"
                            )
                        )
                        //启动 BiometricAuthActivity
                        context.startActivity(serviceIntent)

                    }

                    loadClass("com.qixin.mihuas.core.mvvm.v.BaseFragment",classLoader).methodFinder()
                        .first{ name == "onCreateView" }
                        .createHook {
                            after{
                                val fragmentInstance = it.thisObject
                                // 检查 fragmentInstance 是否为 MineSettingEmployerFragment 的实例
                                if (fragmentInstance::class.java.name == "com.qixin.mihuas.module.main.mine.setting.fragment.MineSettingEmployerFragment") {
                                    Log.i("名称 $fragmentInstance")
                                    executeCustomFunction(fragmentInstance,classLoader)

                                }
                            }
                        }
                    loadClass("com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog", classLoader ).methodFinder()
                        .first { name == "setPayingPasswordContent"}
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                // 获取当前的 InputPayingPasswordDialog 实例
                                val dialogInstance = param.thisObject
                                Log.i("对象 ：$dialogInstance")
                                // 保存对象
                                savedDialogObject = dialogInstance
                                if (!isReceiverRegistered) {
                                    // 注册广播接收器
                                    context.registerReceiver(resultReceiver, IntentFilter("com.mihuashi.paybyfinger.AUTH_RESULT"))
                                    isReceiverRegistered = true  // 标记接收器已注册
                                }
                                val amount = param.args[1] as Int
                                rmb = amount
                                Log.i("付的多少钱：$amount")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startFingerprintAuthentication()  // 启动指纹验证
                                }, 10)

                            }
                        }




                    loadClass("com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog", classLoader ).methodFinder()
                        .first { name == "onPasswordEdited"}
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                val password = param.args[0] as String
                                Log.i("password : $password")

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

            if (rootView != null) {
                // rootView 存在，可以在这里进行进一步操作
                Log.i("Xposed 成功获取 rootView")
                Log.i("rootview 为 ${rootView.accessibilityClassName}")
                // 使用反射加载 MineSettingItemView 类
                val mineSettingItemViewClass = findClass("com.qixin.mihuas.module.main.mine.widget.MineSettingItemView", classLoader)
                val itemView = rootView.findViewById<ViewGroup>(0x7f090edd) as FrameLayout


                // 检查 rootView 的类型，如果是 FrameLayout，可以添加新的视图
                if (rootView is FrameLayout) {
                    // 获取当前 Fragment 的 Context
                    if (context != null) {
                        Log.i("context : ${context.javaClass.name}")
                        XposedHelpers.callMethod(context.resources.assets, "addAssetPath", modulePath)
                    }

                    if (context != null) {
                        // 创建 MineSettingItemView 实例，传入 Context
                        val newItemView = XposedHelpers.newInstance(mineSettingItemViewClass, context) as View


                        // 设置 label 和 icon
                        XposedHelpers.setObjectField(newItemView, "label", "指纹认证")
                        XposedHelpers.setIntField(newItemView, "iconRes", 0x7f080993)
                        //XposedHelpers.setIntField(newItemView, "id", 0x7f090edc)
                        XposedHelpers.callMethod(newItemView, "setCornerSide",1)
                        XposedHelpers.callMethod(newItemView, "componentInitialize")

                        // 设置布局参数，避免重叠，使用WRAP_CONTENT
                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        newItemView.id = 0x7f090edf// 使用资源 ID
                        newItemView.layoutParams = layoutParams

                        newItemView.setOnClickListener {
                            val cll = loadClass("com.qixin.mihuas.base.provider.ContainerActivity")
                            val intent = Intent(context, cll)
                            val containerArgsClass = findClass("com.qixin.mihuas.router.params.ContainerArgs", classLoader)
                            val containerArgs = XposedHelpers.newInstance(containerArgsClass,"指纹认证", "com.qixin.mihuas.module.setting.privacy.fragment.PrivacySettingFragment", null, false) as Parcelable?

                            val bundle = Bundle()
                            bundle.putParcelable("args", containerArgs)  // 将 ContainerArgs 放入 Bundle
                            intent.putExtras(bundle)
                            context.startActivity(intent)
                            loadClass("com.qixin.mihuas.base.provider.ContainerActivity").methodFinder().first{ name == "initViews" }.createHook {
                                after{
                                    val Activity = it.thisObject as Activity
                                    val root = (Activity.findViewById<ViewGroup>(R.id.content)!!).getChildAt(0) as View
                                    logAllViews(root)
                                    Log.i("名称 $Activity")
                                }
                            }
                        }


                        val parentGroup = findParentByChild(itemView) as LinearLayout
                        // 添加新视图
                        //linearLayout.addView(newItemView)
                        parentGroup.addView(newItemView)
                        //logAllViews(rootView)
                    }
                }

            } else {
                Log.e("Xposed rootView 字段不存在或为空")
            }
        } catch (e: NoSuchFieldError) {
            Log.e("Xposed 找不到 rootView 字段: ${e}")
        } catch (e: Exception) {
            Log.e("Xposed 获取 rootView 字段时发生错误: ${e.message}")
        }
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
}
