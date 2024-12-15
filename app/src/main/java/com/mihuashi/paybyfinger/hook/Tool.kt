package com.mihuashi.paybyfinger.hook

import android.annotation.SuppressLint
import android.os.Build

object  Tool {
    val getPhoneName by lazy {
        val marketName = getSystemProperties("ro.product.marketname")
        if (marketName.isNotEmpty()) marketName else Build.BRAND + Build.MODEL}


    @SuppressLint("PrivateApi")

    fun getSystemProperties(key: String): String {
        val ret: String = try {
            Class.forName("android.os.SystemProperties").getDeclaredMethod("get", String::class.java).invoke(null, key) as String
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ""
        }
            return ret
        }
    }