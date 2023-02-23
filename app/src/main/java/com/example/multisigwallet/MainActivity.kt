package com.example.multisigwallet

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar;
    var flowManager = FlowManager("access.devnet.nodes.onflow.org", 9000, this@MainActivity)
    var accountManager = AccountManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val nav = findNavController(R.id.nav_host_fragment)
        val appBar = AppBarConfiguration(setOf(R.id.homeFragment, R.id.noticeFragment, R.id.welcomeFragment))
        setupActionBarWithNavController(nav, appBar)
    }

    override fun onSupportNavigateUp()
            = findNavController(R.id.nav_host_fragment).navigateUp()
}