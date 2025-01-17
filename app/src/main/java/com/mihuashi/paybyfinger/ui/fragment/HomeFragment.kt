package com.mihuashi.paybyfinger.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import cn.xiaowine.xkt.AcTool.restartApp
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.R
import com.mihuashi.paybyfinger.databinding.FragmentHomeBinding
import com.mihuashi.paybyfinger.ui.viewmodel.HomeViewModel
import com.mihuashi.paybyfinger.ui.viewmodel.ShareViewModel
import cn.xiaowine.xkt.Tool.toUpperFirstCaseAndLowerOthers
import com.google.android.material.color.MaterialColors
import com.mihuashi.paybyfinger.hook.Tool.getPhoneName
import com.mihuashi.paybyfinger.tools.ConfigTools.config
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val shareViewModel: ShareViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private var verticalOffset: Int = 0
    private var scrollRange: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            appbarLayout.setExpanded(homeViewModel.expanded)
            appbarLayout.addOnOffsetChangedListener { appbarLayout, verticalOffset ->
                this@HomeFragment.verticalOffset = verticalOffset
                scrollRange = appbarLayout.totalScrollRange
            }

            nestedScrollView.scrollY = homeViewModel.scrollY

            // 状态视图状态
            if (!shareViewModel.activated) {
                statusIcon.setImageResource(R.drawable.ic_round_error_outline)
                statusTitle.text = getString(R.string.unactivated)
                statusSummary.text = getString(R.string.unactivated_summary)
                status.setBackgroundColor(
                    MaterialColors.getColor(requireContext(), android.R.attr.colorError, Color.RED)
                )
                status.setOnClickListener {
                    restartApp()
                }
            } else{
                status.setOnClickListener {
                    Toast.makeText(context,R.string.touchable,Toast.LENGTH_SHORT).show()
                }
            }

            // 信息卡片设置
            deviceValue.text = "$getPhoneName (${Build.DEVICE})"
            if (config.systemversion == ""){
                systemversionValue.text = "Android ${Build.VERSION.RELEASE} SDK ${Build.VERSION.SDK_INT}"
            } else {
                systemversionValue.text = "Android ${Build.VERSION.RELEASE} SDK ${Build.VERSION.SDK_INT} \n${config.systemversion}"
            }
            versionLabelValue.text = BuildConfig.VERSION_NAME
            versionCodeValue.text = BuildConfig.VERSION_CODE.toString()
            versionTypeValue.text = BuildConfig.BUILD_TYPE.toUpperFirstCaseAndLowerOthers()
            buildTimeValue.text = homeViewModel.buildTimeValue ?: SimpleDateFormat(
                "yyyy-MM-dd HH:mm z", Locale.getDefault()
            ).format(BuildConfig.BUILD_TIME)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeViewModel.apply {
            scrollY = binding.nestedScrollView.scrollY
            expanded = verticalOffset == 0 || scrollRange < verticalOffset
        }
        _binding = null
    }
}
