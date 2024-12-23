package com.mihuashi.paybyfinger

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE
import cn.xiaowine.xkt.AcTool
import com.google.android.material.navigation.NavigationBarView
import com.mihuashi.paybyfinger.databinding.ActivityMainBinding
import com.mihuashi.paybyfinger.tools.Tools.xpActivation
import com.mihuashi.paybyfinger.ui.viewmodel.ShareViewModel

class MainActivity : AppCompatActivity() {

    private val shareViewModel: ShareViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AcTool.init(this)
        enableEdgeToEdge()
        xpActivation = DSP.init(this, BuildConfig.APPLICATION_ID, MODE.HOOK, false)
        shareViewModel.activated = xpActivation
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


    override fun onStart() {
        super.onStart()
        (binding.nav as NavigationBarView).setupWithNavController(findNavController(R.id.nav_host_fragment))
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
