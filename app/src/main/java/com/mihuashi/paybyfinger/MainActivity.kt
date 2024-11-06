package com.mihuashi.paybyfinger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.mihuashi.paybyfinger.databinding.ActivityMainBinding
import com.mihuashi.paybyfinger.service.FingerprintService
import com.mihuashi.paybyfinger.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var fingerprintService: FingerprintService? = null
    private var isBound = false
    private val TAG = "mihuashihook"

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as FingerprintService.FingerprintBinder
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            fingerprintService = binder.service
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            fingerprintService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 绑定服务
        val intent = Intent(this, FingerprintService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        setContent {
            MyApplicationTheme { // 使用你自定义的主题
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun startAuthentication(context: Context) {
        if (isBound && fingerprintService != null) {
            fingerprintService?.startBiometricAuthentication(object : FingerprintService.AuthenticationCallback {
                override fun onAuthenticationSuccess() {
                    Log.i("认证成功")
                    Toast.makeText(context, "认证成功了", Toast.LENGTH_SHORT).show()

                }

                override fun onAuthenticationFailure(error: String) {
                    Log.i("认证失败")
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    @Composable
    fun MainScreen() {
        // 创建按钮
        val context = LocalContext.current
        Button(
            onClick = { startAuthentication(context) },
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center) // 居中显示按钮
        ) {
            Text(text = "开始认证")
        }
    }
}
