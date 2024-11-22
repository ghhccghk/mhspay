package com.mihuashi.paybyfinger.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
                    startActivity(intent)
                    false
                }
            }
            accentButtonPref("open_about_app_button") {
                titleRes = R.string.about_app
                onClick {
                    Toast.makeText(context,"界面还没写",Toast.LENGTH_SHORT).show()
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
}