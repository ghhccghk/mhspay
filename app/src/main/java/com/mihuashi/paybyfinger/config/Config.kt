package com.mihuashi.paybyfinger.config

import android.os.Build
import cn.xiaowine.dsp.delegate.Delegate.serialLazy
import com.mihuashi.paybyfinger.hook.Tool.getSystemProperties

class Config {
    var hidesetting: Boolean by serialLazy(false)
}
        