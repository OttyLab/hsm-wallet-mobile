package com.example.multisigwallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    var flowManager = FlowManager("access.devnet.nodes.onflow.org", 9000, this@MainActivity)
    var accountManager = AccountManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}