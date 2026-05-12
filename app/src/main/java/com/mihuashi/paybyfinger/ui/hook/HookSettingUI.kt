package com.mihuashi.paybyfinger.ui.hook

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.hook.Hook.createBiometricIntent
import com.mihuashi.paybyfinger.hook.Hook.paymentTimestamp
import com.mihuashi.paybyfinger.hook.Hook.resultReceiver
import com.mihuashi.paybyfinger.hook.HookTool.Companion.PasswordSaveCallback
import com.mihuashi.paybyfinger.hook.HookTool.Companion.convertTimestampToTime
import com.mihuashi.paybyfinger.hook.HookTool.Companion.savePasswordWithCallback
import com.mihuashi.paybyfinger.tools.ConfigTools.AUTH_RESULT_ACTION
import com.mihuashi.paybyfinger.tools.ConfigTools.DEFAULT_DELAY_MAX
import com.mihuashi.paybyfinger.tools.ConfigTools.DEFAULT_DELAY_MIN
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_ALL_SWITCH
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_AVATAR_URL
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_CREATE_AT
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_DELAY_MAX
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_DELAY_MIN
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_PHONE
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_PHONE_PREFIX
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_USER_ID
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_USERNAME
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.core.content.edit
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mihuashi.paybyfinger.tools.ConfigTools.KEY_MI_SWITCH
import com.mihuashi.paybyfinger.tools.ConfigTools.PREF_NAME
import androidx.compose.foundation.shape.CircleShape
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
fun HookSettingUI(context: Context) {
    val scrollBehavior = MiuixScrollBehavior()
    val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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

        var allSwitch by remember {
            mutableStateOf(
                sharedPreferences.getBoolean(KEY_ALL_SWITCH, false)
            )
        }
        var miSwitch by remember {
            mutableStateOf(
                sharedPreferences.getBoolean(KEY_MI_SWITCH, false)
            )
        }
        var delayMin by remember {
            mutableStateOf(
                sharedPreferences.getFloat(KEY_DELAY_MIN, DEFAULT_DELAY_MIN)
            )
        }
        var delayMax by remember {
            mutableStateOf(
                sharedPreferences.getFloat(KEY_DELAY_MAX, DEFAULT_DELAY_MAX)
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
        ) {


            if (BuildConfig.DEBUG) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        BasicComponent(
                            title = "当前为调试版本",
                            titleColor = BasicComponentColors(
                                color = MiuixTheme.colorScheme.error,
                                disabledColor = MiuixTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp))
                {
                    BasicComponent(
                        title = "总开关",
                        endActions = {
                            Switch(
                                checked = allSwitch,
                                onCheckedChange = {
                                    allSwitch = it
                                    sharedPreferences.edit {
                                        putBoolean(KEY_ALL_SWITCH, it)
                                    }
                                }
                            )
                        }
                    )
                }
            }
            item {
                SmallTitle(
                    text = "当前用户信息"
                )
                Card(modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp))
                {
                    val username = sharedPreferences.getString(KEY_USERNAME, "未设置用户名")
                    val avatarUrl = sharedPreferences.getString(KEY_AVATAR_URL, null)
                    val createAt = sharedPreferences.getString(KEY_CREATE_AT, "")
                    val id = sharedPreferences.getString(KEY_USER_ID, "")
                    val phone = sharedPreferences.getString(KEY_PHONE, "")
                    val phonePrefix = sharedPreferences.getString(KEY_PHONE_PREFIX, "")

                    val formatted = remember {
                        runCatching {
                            OffsetDateTime.parse(createAt)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        }.getOrNull() ?: "时间解析失败"
                    }
                    BasicComponent {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .build(),
                                    contentDescription = "头像",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.Rename,
                                    contentDescription = "默认头像",
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(18.dp))
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = username ?: "未设置用户名",style = MiuixTheme.textStyles.main)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = id.toString(),style = MiuixTheme.textStyles.footnote1, modifier = Modifier.padding(bottom = 2.dp))
                                }
                                Text(text = "手机号: $phonePrefix $phone",style = MiuixTheme.textStyles.main)
                                Text(text = "注册时间: $formatted",style = MiuixTheme.textStyles.main)
                            }
                        }
                    }
                }

            }
            item {
                Card(modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp))
                {
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
                                    context.registerReceiver(
                                        resultReceiver,
                                        filter,
                                        Context.RECEIVER_EXPORTED
                                    )
                                } else {
                                    ContextCompat.registerReceiver(
                                        context,
                                        resultReceiver,
                                        filter,
                                        ContextCompat.RECEIVER_EXPORTED
                                    )
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "启动失败: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                    )
                }
            }
            if (allSwitch) {
                item {
                    SmallTitle(
                        text = "配置"
                    )
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        BasicComponent(
                            title = "修改密码",
                            summary = "点击此处修改密码",
                            onClick = { showDialog = true }
                        )
                        BasicComponent(
                            title = "焦点通知金额开关 (小米专用)",
                            endActions = {
                                Switch(
                                    checked = miSwitch,
                                    onCheckedChange = {
                                        miSwitch = it
                                        sharedPreferences.edit {
                                            putBoolean(KEY_MI_SWITCH, it)
                                        }
                                    }
                                )
                            }
                        )
                    }
                    SmallTitle(
                        text = "随机延迟配置"
                    )
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "最小延迟: ${delayMin.toInt()} ms",
                                style = MiuixTheme.textStyles.body2
                            )
                            Spacer(modifier = Modifier.width(4.dp),)
                            Slider(
                                value = delayMin,
                                onValueChange = { newValue ->
                                    delayMin = if (newValue <= delayMax) newValue else delayMax
                                },
                                onValueChangeFinished = {
                                    sharedPreferences.edit {
                                        putFloat(KEY_DELAY_MIN, delayMin)
                                    }
                                },
                                valueRange = 50f..2000f,
                                steps = 38,
                                modifier = Modifier.fillMaxSize()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "最大延迟: ${delayMax.toInt()} ms",
                                style = MiuixTheme.textStyles.body2
                            )
                            Spacer(modifier = Modifier.width(4.dp),)
                            Slider(
                                value = delayMax,
                                onValueChange = { newValue ->
                                    delayMax = if (newValue >= delayMin) newValue else delayMin
                                },
                                onValueChangeFinished = {
                                    sharedPreferences.edit {
                                        putFloat(KEY_DELAY_MAX, delayMax)
                                    }
                                },
                                valueRange = 50f..2000f,
                                steps = 38,
                                modifier = Modifier.fillMaxSize()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "实际随机范围: ${delayMin.toInt()}ms ~ ${delayMax.toInt()}ms",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    SmallTitle(
                        text = "关于"
                    )
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        BasicComponent(
                            title = "构建类型",
                            summary = BuildConfig.BUILD_TYPE,
                        )
                        BasicComponent(
                            title = "构建时间",
                            summary = convertTimestampToTime(BuildConfig.BUILD_TIME),
                        )
                        BasicComponent(
                            title = "版本号",
                            summary = BuildConfig.VERSION_NAME,
                        )
                    }
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
                    isError =
                        passwordValue.isNotEmpty() && !passwordValue.matches(Regex("^\\d{6}$"))
                }
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
                    isErrorCheck =
                        passwordCheckValue.isNotEmpty() && !passwordCheckValue.matches(Regex("^\\d{6}$"))
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
                                    Toast.makeText(context, "密码设置成功!", Toast.LENGTH_SHORT)
                                        .show()
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