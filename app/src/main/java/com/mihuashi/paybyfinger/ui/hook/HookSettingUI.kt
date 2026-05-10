package com.mihuashi.paybyfinger.ui.hook

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.hook.Hook.AUTH_RESULT_ACTION
import com.mihuashi.paybyfinger.hook.Hook.createBiometricIntent
import com.mihuashi.paybyfinger.hook.Hook.paymentTimestamp
import com.mihuashi.paybyfinger.hook.Hook.resultReceiver
import com.mihuashi.paybyfinger.hook.HookTool.Companion.PasswordSaveCallback
import com.mihuashi.paybyfinger.hook.HookTool.Companion.savePasswordWithCallback
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog


@Composable
fun HookSettingUI(context: Context){
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "指纹认证设置",
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        var showDialog by remember { mutableStateOf(false) }
        var passwordValue by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var passwordCheckValue by remember { mutableStateOf("") }
        var passwordCheckVisible by remember { mutableStateOf(false) }
        var isError by remember { mutableStateOf(false) }
        var isErrorCheck by remember { mutableStateOf(false) }
        val errorColor = androidx.compose.ui.graphics.Color.Red.copy(0.3f)



        // 内容区域需要考虑 padding
        LazyColumn(
            modifier = Modifier
                .padding(start = 8.dp, end= 8.dp)
                .fillMaxSize()
                // 如需添加越界回弹效果，则应在绑定滚动行为之前添加
                .overScrollVertical()
                // 绑定 TopAppBar 滚动事件
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
        ) {
            item {
                Card {
                    BasicComponent(
                        title = "测试调用",
                        onClick = {
                            try {
                                paymentTimestamp = System.currentTimeMillis().toString()
                                val intent = createBiometricIntent()
                                context.startActivity(intent)

                                // 注册广播接收器监听认证结果
                                val filter = IntentFilter(AUTH_RESULT_ACTION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED)
                                } else {
                                    ContextCompat.registerReceiver(
                                        context, resultReceiver, filter, ContextCompat.RECEIVER_EXPORTED
                                    )
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        }
                    )
                    BasicComponent(
                        title = "修改密码",
                        summary = "点击此处修改密码",
                        onClick = { showDialog = true }
                    )
                    BasicComponent(
                        title = "版本号",
                        summary = BuildConfig.VERSION_NAME,

                        )
                }
            }

        }


        WindowDialog(
            title = "请输入密码",
            show = showDialog,
            onDismissRequest = {
                passwordValue = ""
                passwordCheckValue = ""
                showDialog = false
            } // 关闭对话框
        ) {
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                label = "请输入密码,只允许6个数字",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                value = passwordValue,
                maxLines = 1,
                labelColor = if (isError) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Rename,
                            tint = if (passwordVisible) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSecondaryContainer,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                onValueChange = {
                    passwordValue = it
                    isError = passwordValue.isNotEmpty() && !passwordValue.matches(Regex("^\\d{6}$")) }
            )
            if (isError) {
                Text(
                    text = "请输入有效的密码",
                    color = errorColor,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                visualTransformation = if (passwordCheckVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                label = "请再次输入密码,只允许6个数字",
                value = passwordCheckValue,
                maxLines = 1,
                trailingIcon = {
                    IconButton(
                        onClick = { passwordCheckVisible = !passwordCheckVisible },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Rename,
                            tint = if (passwordCheckVisible) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSecondaryContainer,
                            contentDescription = if (passwordCheckVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                labelColor = if (isErrorCheck) errorColor else MiuixTheme.colorScheme.onSecondaryContainer,
                onValueChange = {
                    passwordCheckValue = it
                    isErrorCheck = passwordCheckValue.isNotEmpty() && !passwordCheckValue.matches(Regex("^\\d{6}$"))
                }
            )
            if (isErrorCheck) {
                Text(
                    text = "请输入有效的密码",
                    color = errorColor,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }


            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        passwordValue = ""
                        passwordCheckValue = ""
                        showDialog = false
                    }, // 关闭对话框
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "确认",
                    onClick = {
                        val password = passwordValue
                        val confirmPassword = passwordCheckValue
                        if (password.length == 6 && password == confirmPassword) {
                            val callback = object : PasswordSaveCallback {
                                override fun onSuccess() {
                                    Toast.makeText(context, "密码设置成功!", Toast.LENGTH_SHORT).show()
                                    passwordValue = ""
                                    passwordCheckValue = ""
                                }

                                override fun onFailure(error: String) {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    passwordValue = ""
                                    passwordCheckValue = ""
                                }
                            }
                            savePasswordWithCallback(context, confirmPassword, callback)
                        } else if (password.length != 6) {
                            Toast.makeText(context, "密码需要6位数字", Toast.LENGTH_SHORT).show()
                            passwordValue = ""
                            passwordCheckValue = ""
                        } else {
                            Toast.makeText(context, "密码设置失败！", Toast.LENGTH_SHORT).show()
                            passwordValue = ""
                            passwordCheckValue = ""
                        }
                        showDialog = false
                    }, // 关闭对话框
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary() // 使用主题颜色
                )
            }
        }
    }
}