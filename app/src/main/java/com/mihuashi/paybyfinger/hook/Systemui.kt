package com.mihuashi.paybyfinger.hook

import com.github.islamkhsh.BuildConfig
import com.mihuashi.paybyfinger.BaseHook
import com.mihuashi.paybyfinger.PACKAGE_NAME_HOOKED
import com.mihuashi.paybyfinger.tools.SystemConfig
import de.robv.android.xposed.XposedHelpers
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook

object Systemui : BaseHook() {

    override val name: String = "米画师hook"

    @Suppress("UNREACHABLE_CODE")
    override fun init() {
        if (SystemConfig.isMIUI) {
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
                                        Logger.d("软件名称 ${aa}")
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
                            e.message?.let { Logger.i(it) }
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