/*
 * StatusBarLyric
 * Copyright (C) 2021-2022 fkj@fkj233.cn
 * https://github.com/577fkj/StatusBarLyric
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by 577fkj.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/577fkj/StatusBarLyric/blob/main/LICENSE>.
 */

package com.mihuashi.paybyfinger.tools

import android.annotation.SuppressLint
import  com.mihuashi.paybyfinger.config.Config

@SuppressLint("StaticFieldLeak")
object ConfigTools {
    val config: Config by lazy { Config() }
    val xConfig: Config by lazy { Config() }

    // ==================== 常量定义 ====================

    /** SharedPreferences 文件名 */
    val PREF_NAME = "mhshooksetting"

    /** 总开关：启用/禁用指纹认证 Hook */
   val KEY_ALL_SWITCH = "allswitch"

    /** 小米焦点通知开关：是否在通知栏显示支付金额 */
    val KEY_MI_SWITCH = "miswitch"

    /** 指纹认证结果广播的 Action */
     val AUTH_RESULT_ACTION = "com.mihuashi.paybyfinger.AUTH_RESULT"

    /** 指纹验证 Activity 的类名 */
   val BIOMETRIC_ACTIVITY_CLASS =
        "com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity"

    /** 本模块的包名 */
    val MODULE_PACKAGE = "com.mihuashi.paybyfinger"

    /** 米画师密码输入对话框类名 */
     val INPUT_PASSWORD_DIALOG_CLASS =
        "com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog"

    /** 设置页面 Fragment 类名 */
     val SETTING_FRAGMENT_CLASS =
        "com.qixin.mihuas.module.main.mine.setting.fragment.MineSettingEmployerFragment"

    /** 设置项自定义 View 类名 */
    val MINE_SETTING_ITEM_VIEW_CLASS =
        "com.qixin.mihuas.module.main.mine.widget.MineSettingItemView"

    /** 基础 Fragment 类名 */
    val BASE_FRAGMENT_CLASS =
        "com.qixin.mihuas.core.mvvm.v.BaseFragment"
}
