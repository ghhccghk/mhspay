package com.mihuashi.paybyfinger

import cn.xiaowine.xkt.LogTool
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.mihuashi.paybyfinger.hook.Hook
import com.mihuashi.paybyfinger.hook.Systemui
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage


val PACKAGE_NAME_HOOKED = "com.qixin.mihuas"
private const val TAG = "mihuashihook"

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit /* Optional */ {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == PACKAGE_NAME_HOOKED) {
            // Init EzXHelper
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            LogTool.init("米画师hook", { BuildConfig.DEBUG })
            // Init hooks
            initHooks(Hook)
        }
        if (lpparam.packageName == "com.android.systemui"){
            // Init EzXHelper
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            LogTool.init("米画师hook", { BuildConfig.DEBUG })
            // Init hooks
            initHooks(Systemui)
        }
    }

    // Optional
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    private fun initHooks(vararg hook: BaseHook) {
        hook.forEach {
            runCatching {
                if (it.isInit) return@forEach
                it.init()
                it.isInit = true
                Log.i("Inited hook: ${it.name}")
            }.logexIfThrow("Failed init hook: ${it.name}")
        }
    }
}