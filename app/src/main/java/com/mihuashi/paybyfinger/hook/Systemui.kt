package com.mihuashi.paybyfinger.hook

import com.github.islamkhsh.BuildConfig
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.mihuashi.paybyfinger.BaseHook
import com.mihuashi.paybyfinger.PACKAGE_NAME_HOOKED
import com.mihuashi.paybyfinger.tools.ConfigTools.config
import de.robv.android.xposed.XposedHelpers

object Systemui : BaseHook() {

    override val name: String = "米画师hook"

    @Suppress("UNREACHABLE_CODE")
    override fun init() {
        // 拿到插件的classloader

        if (config.isMIUI) {
        loadClass("com.android.systemui.shared.plugins.PluginInstance").methodFinder()
            .first { name == "loadPlugin" }.createHook {
                after { p0 ->
                    val mPlugin = XposedHelpers.getObjectField(p0.thisObject, "mPlugin")
                    val pluginClassLoader = mPlugin::class.java.classLoader
                    try {
                        val cl = loadClass(
                            "miui.systemui.notification.FocusNotificationPluginImpl",
                            pluginClassLoader
                        )
                        // 过滤 系统界面组件
                        if (cl.isInstance(mPlugin)) {
                            loadClass(
                                "miui.systemui.notification.NotificationSettingsManager",
                                pluginClassLoader
                            ).methodFinder().first { name == "canShowFocus" }.createHook {
                                before { param ->
                                    val aa = param.args[1] as String
                                    if (BuildConfig.DEBUG) {
                                        Log.d("软件名称 ${aa}")
                                    }
                                    if (aa == PACKAGE_NAME_HOOKED) {
                                        param.result = true
                                    }
                                }
                            }
                        }

                    } catch (e: Exception) {
                        return@after
                        if (BuildConfig.DEBUG){
                            e.message?.let { Log.i(it) }
                        }
                    }
                    // 启用debug日志
//                    setStaticObject(
//                        loadClass(
//                            "miui.systemui.notification.NotificationUtil",
//                            pluginClassLoader
//                        ), "DEBUG", true
//                    )

                }
            }
        }
    }
}