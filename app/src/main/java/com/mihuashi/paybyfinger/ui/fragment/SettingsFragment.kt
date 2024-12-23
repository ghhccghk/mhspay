package com.mihuashi.paybyfinger.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kyuubiran.ezxhelper.Log
import com.mihuashi.paybyfinger.R
import com.mihuashi.paybyfinger.databinding.FragmentSettingsBinding
import com.mihuashi.paybyfinger.tools.ConfigTools.config
import com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.accentButtonPref
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.screen
import de.Maxr1998.modernpreferences.helpers.switch
import de.Maxr1998.modernpreferences.preferences.EditTextPreference

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val screen = screen(context) {
            accentButtonPref("open_settings_button") {
                titleRes = R.string.fingerprint_test
                onClick {
                    // 这里可以触发按钮点击的行为，打开指纹测试
                    val intent = Intent(context, BiometricAuthActivity::class.java)
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, Activity.RESULT_CANCELED)
                    false
                }
            }
            accentButtonPref("open_about_app_button") {
                titleRes = R.string.about_app
                onClick {
                    val intent = Intent(context, AboutFragment::class.java)
                    startActivity(intent)
                    false
                }
            }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            adapter = PreferencesAdapter(screen)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Activity.RESULT_CANCELED || resultCode == Activity.RESULT_OK) {
            val result = data?.getBooleanExtra("resultcode",false)
            val error_message = data?.getStringExtra("error_message")
            if (result == true ){
                Toast.makeText(this.context,R.string.fingerprint_ok,Toast.LENGTH_SHORT).show()
            } else {
                val no_ok = this.context?.resources?.getString(R.string.fingerprint_no_ok)
                if (error_message != null){
                    Toast.makeText(this.context,"$no_ok,错误信息是：$error_message",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}