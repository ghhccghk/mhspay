package com.mihuashi.paybyfinger.hook

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log

object  Tool {
    val getPhoneName by lazy {
        val marketName = getSystemProperties("ro.product.marketname")
        if (marketName.isNotEmpty()) bigtextone(marketName) else bigtextone(Build.BRAND) + " " + Build.MODEL
    }

    fun bigtextone(st:String): String {
        val formattedBrand = st.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        return formattedBrand
    }

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