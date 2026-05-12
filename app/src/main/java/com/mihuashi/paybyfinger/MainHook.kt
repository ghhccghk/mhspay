package com.mihuashi.paybyfinger

import cn.xiaowine.xkt.LogTool
import com.mihuashi.paybyfinger.hook.Hook
import com.mihuashi.paybyfinger.hook.Systemui
import com.mihuashi.paybyfinger.tools.utils.ResInjectTool
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kyuubiran.ezxhelper.android.logging.AndroidLogger
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import io.github.kyuubiran.ezxhelper.xposed.EzXposed


val PACKAGE_NAME_HOOKED = "com.qixin.mihuas"
private const val TAG = "mihuashihook"

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit /* Optional */ {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == PACKAGE_NAME_HOOKED) {
            // Init EzXHelper
            EzXposed.initHandleLoadPackage(lpparam)
            Logger.tag = TAG
            LogTool.init("米画师hook", { BuildConfig.DEBUG })
            // Init hooks
            initHooks(Hook)
        }
        if (lpparam.packageName == "com.android.systemui"){
            // Init EzXHelper
            EzXposed.initHandleLoadPackage(lpparam)
            Logger.tag = TAG
            LogTool.init("米画师hook", { BuildConfig.DEBUG })
            // Init hooks
            initHooks(Systemui)
        }
    }

    // Optional
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXposed.initZygote(startupParam)
        ResInjectTool.init(startupParam.modulePath)
    }
    private fun initHooks(vararg hook: BaseHook) {
        hook.forEach {
            runCatching {
                if (it.isInit) return@forEach
                it.init()
                it.isInit = true
                Logger.i("Inited hook: ${it.name}")
            }
        }
    }
}