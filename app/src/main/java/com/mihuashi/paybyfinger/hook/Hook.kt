package com.mihuashi.paybyfinger.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findClass
import java.lang.ref.WeakReference
import java.lang.reflect.Method


object Hook : BaseHook() {

    private var isReceiverRegistered: Boolean = false
    override val name: String = "米画师hook"

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun init() {
       loadClassOrNull("com.stub.StubApp").isNotNull {
            it.methodFinder().first { name == "attachBaseContext" }.createHook {
                after { param ->
                    val context = param.args[0] as Context
                    val classLoader = context.classLoader
                    val serviceIntent = Intent()

                    val resultReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val result = intent?.getBooleanExtra("result", false) ?: false
                            val errorMessage = intent?.getStringExtra("error_message")
                            val time = intent?.getLongExtra("timestamp", 0).toString()
                            val fullClassName = context?.javaClass?.name
                            Log.i("FingerprintAuth 认证时间：$time $fullClassName")
                            if (result) {
                                Toast.makeText(
                                    context,
                                    "FingerprintAuth 认证成功，时间：$time",
                                    Toast.LENGTH_SHORT
                                ).show()
                                XposedHelpers.findAndHookConstructor(
                                    "com.qixin.mihuas.widgets.modal.base.BaseDialog\$SafetyDialog",
                                    classLoader,
                                    Context::class.java,
                                    Int::class.javaPrimitiveType,  // 使用基本类型的 `int`
                                    object : XC_MethodHook() {
                                        override fun afterHookedMethod(param: MethodHookParam) {
                                            val safetyDialog = param.thisObject
                                            // 获取 SafetyDialog 的所有字段并打印
                                            val fields = safetyDialog.javaClass.declaredFields
                                            fields.forEach { field ->
                                                field.isAccessible = true // 确保可以访问私有字段
                                                val fieldType = field.type
                                                val fieldName = field.name
                                                if (fieldType == EditText::class.java) {
                                                    val editTextInstance = field.get(safetyDialog) as? EditText
                                                    Log.i("Found EditText field: $editTextInstance")
                                                }
                                                Log.i("Field name: $fieldName, type: $fieldType")
                                            }

                                            // 获取到特定的 EditText 实例
                                            val editTextField = XposedHelpers.findFieldIfExists(
                                                safetyDialog.javaClass,
                                                "passwordEditltem"  // 替换为实际字段名
                                            ) ?: return
                                            val editTextInstance = editTextField.get(safetyDialog) as? EditText
                                            editTextInstance?.addTextChangedListener(object : TextWatcher {
                                                override fun afterTextChanged(s: Editable?) {
                                                    Log.i("Text changed: ${s.toString()}")
                                                }
                                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                            })
                                        }
                                    }
                                )

                            } else {
                                    Log.i("FingerprintAuth 认证失败，错误信息：$errorMessage，时间：$time")
                                    Toast.makeText(context, "FingerprintAuth 认证失败，错误信息：$errorMessage，时间：$time", Toast.LENGTH_SHORT).show()

                            }
                        }
                    }

                    // 创建 Intent 启动指纹服务
                    fun startFingerprintAuthentication() {
                        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)// 添加此标志
                        serviceIntent.setComponent(
                            ComponentName(
                                "com.mihuashi.paybyfinger",
                                "com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity"
                            )
                        )
                        //启动 BiometricAuthActivity
                        context.startActivity(serviceIntent)
                    }

                    loadClass("com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog", classLoader ).methodFinder()
                        .first { name == "setPayingPasswordContent"}
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                if (!isReceiverRegistered) {
                                    // 注册广播接收器
                                    context.registerReceiver(resultReceiver, IntentFilter("com.mihuashi.paybyfinger.AUTH_RESULT"))
                                    isReceiverRegistered = true  // 标记接收器已注册
                                }
                                val amount = param.args[1] as Int
                                Log.i("付的多少钱：$amount")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startFingerprintAuthentication()  // 启动指纹验证
                                }, 10)

                            }
                        }
                    loadClass("com.qixin.mihuas.modules.account.dialog.InputPayingPasswordDialog", classLoader ).methodFinder()
                        .first { name == "onPasswordEdited"}
                        .createHook {
                            after { param ->
                                // 检查接收器是否已经注册
                                val password = param.args[0] as String
                                Log.i("password : $password")

                            }
                        }
                    val inputDialogClass = findClass("com.qixin.mihuas.widgets.modal.base.BaseDialog", classLoader)
                    //val toastModalClass = findClass("com.qixin.mihuas.widgets.modal.ToastModalDialog", classLoader)

                    fun logClassFields(cls: Class<*>, className: String) {
                        cls.declaredFields.forEach { field ->
                          //Log.i("$className Field: ${field.name}")
                        }
                    }

                    logClassFields(inputDialogClass, "BaseDialog")

                    //logClassFields(toastModalClass, "ToastModalDialog")
                    //val safetyDialogClass = Class.forName("com.qixin.mihuas.widgets.modal.base.BaseDialog\$SafetyDialog")
                    XposedHelpers.findAndHookMethod("com.qixin.mihuas.widgets.modal.base.BaseDialog\$SafetyDialog", classLoader, "setHostFragment", // 替换为实际存在的方法名
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                //val contextField = param.javaClass.getDeclaredField("mContext")
                                //contextField.isAccessible = true
                                //val context = contextField.get(param.javaClass) as Context
                                //Log.i("Context via reflection: $context")
                                val baseDialog = param.args[0] as Dialog
                                val contexta = baseDialog.context // 或者其他方法来获取 context
                                Log.i("Context from BaseDialog: $contexta")
                            }
                        }
                    )

                }
            }
        }
    }

}