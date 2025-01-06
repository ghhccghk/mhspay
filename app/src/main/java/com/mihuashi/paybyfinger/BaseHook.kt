package com.mihuashi.paybyfinger

import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE


abstract class BaseHook {
    abstract val name: String
    var isInit: Boolean = false
    open fun init() {
        DSP.init(null, BuildConfig.APPLICATION_ID, MODE.HOOK, true)
    }
}


