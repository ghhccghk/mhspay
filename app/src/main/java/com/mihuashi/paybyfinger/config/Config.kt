package com.mihuashi.paybyfinger.config

import android.os.Build
import cn.xiaowine.dsp.delegate.Delegate.serialLazy
import com.mihuashi.paybyfinger.hook.Tool.getSystemProperties

class Config {
    var hidesetting: Boolean by serialLazy(false)
    /**
     * 当前设备是否是 MIUI、HyperOS 定制 Android 系统
     * @return [Boolean] 是否符合条件
     */
    val isMiSystem get() = isMIUI || isMIOS

    /**
     * 当前设备是否是 MIUI 定制 Android 系统
     * @return [Boolean] 是否符合条件
     */
    val isMIUI by lazy { getSystemProperties("ro.miui.ui.version.name") != "" }

    /** 判断当前设备是否为vivo设备*/
    val isVivo by lazy { getSystemProperties("ro.vivo.os.build.display.id") != "" }


    /**
     * 当前设备是否是 HyperOS 定制 Android 系统
     * @return [Boolean] 是否符合条件
     */
    val isMIOS get() = isMIUI && miuiVersion == "816"

    /**
     * 当前设备是否不是 MIUI 定制 Android 系统
     * @return [Boolean] 是否符合条件
     */
    inline val isNotMIUI get() = !isMIUI

    /**
     * 当前设备是否不是 HyperOS 定制 Android 系统
     * @return [Boolean] 是否符合条件
     */
    inline val isNotMIOS get() = !isMIOS

    /**
     * 获取 MIUI、HyperOS 版本
     * @return [String]
     */
    val miSystemVersion
        get() = when {
            isMIOS -> miosVersion
            isMIUI -> miuiVersion
            else -> ""
        }

    /**
     * 获取 MIUI、HyperOS 版本号
     * @return [Float]
     */
    val miSystemVersionCode
        get() = when {
            isMIOS -> miosVersionCode
            isMIUI -> miuiVersionCode
            else -> 0f
        }

    /**
     * 获取 MIUI 版本
     * @return [String]
     */
    val miuiVersion
        get() = if (isMIUI)
            getSystemProperties("ro.miui.ui.version.name").let {
                when (it) {
                    "V11", "V110" -> "11"
                    "V12", "V120" -> "12"
                    "V125" -> "12.5"
                    "V13", "V130" -> "13"
                    "V14", "V140" -> "14"
                    else -> it.replace("V", "")
                }
            }.trim()
        else ""

    /**
     * 获取 HyperOS 版本
     * @return [String]
     */
    val miosVersion
        get() = if (isMIOS)
            getSystemProperties("ro.mi.os.version.name").let {
                if (it.startsWith("OS")) it.replaceFirst("OS", "") else it
            }.trim()
        else ""

    /**
     * 获取 MIUI 版本号
     * @return [Float]
     */
    val miuiVersionCode get() = miuiVersion.toFloatOrNull() ?: 0f

    /**
     * 获取 HyperOS 版本号
     * @return [Float]
     */
    val miosVersionCode get() = getSystemProperties("ro.mi.os.version.code").toFloatOrNull() ?: 0f

    /**
     * 获取 MIUI 次版本号
     * @return [String]
     */
    val miuiIncrementalVersion get() = getSystemProperties("ro.system.build.version.incremental").trim()

    /**
     * 获取 HyperOS 次版本号
     * @return [String]
     */
    val miosIncrementalVersion get() = getSystemProperties("ro.mi.os.version.incremental").trim()

    /**
     * 获取 MIUI、HyperOS 完全版本
     * @return [String]
     */
    val miuisystemFullVersion
        get() = when {
            isMIOS -> "HyperOS " + miosIncrementalVersion.let {
                if (it.lowercase().endsWith(".dev").not() && it.lowercase().any { e -> e.code in 97..122 })
                    "${it.replaceFirst("OS", "")} 稳定版"
                else when {
                    it.lowercase().endsWith(".dev") -> "${it.replaceFirst("OS", "")} 开发版"
                    else -> "$miosVersion $it 开发版"
                }
            }
            isMIUI -> miuiIncrementalVersion.let {
                if (it.lowercase().endsWith(".dev").not() && it.lowercase().any { e -> e.code in 97..122 })
                    "$it 稳定版"
                else when {
                    it.lowercase().endsWith(".dev") -> "$it 开发版"
                    else -> "V$miuiVersion $it 开发版"
                }
            }
            else -> "不是 MIUI 或 HyperOS 系统"
        }

    val vivosystemversion get() = when{
        isVivo -> "${getSystemProperties("ro.vivo.os.build.display.id")}  ${getSystemProperties("ro.build.version.bbk")}"
        else -> "Android ${Build.VERSION.RELEASE} SDK ${Build.VERSION.SDK_INT}"
    }

    val systemversion get() = when{
        isMIOS -> miuisystemFullVersion
        isVivo -> vivosystemversion
        else -> ""
    }
}
        