package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nftco.flow.sdk.FlowAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {
    lateinit var progressBarCreating: ProgressBar
    lateinit var buttonCreateAccount: Button
    lateinit var buttonBackup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val launcher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val code = result.contents.toString()
                val address = readAccount(code)

                if(address == null) {
                    Snackbar
                        .make(
                            this.requireView(),
                            "Unexpected QR code: ${code}",
                            Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    try {
                        val activity = activity as MainActivity
                        activity.accountManager.setAddress(address)
                        activity.accountManager.initKeyPair()
                        findNavController().navigate(R.id.action_welcome_to_notice)
                    } catch (e: Exception) {
                        Snackbar
                            .make(
                                this@WelcomeFragment.requireView(),
                                "Enable biometrics in system setting",
                                Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        progressBarCreating = view.findViewById(R.id.progressBarCreating)
        buttonCreateAccount = view.findViewById<Button>(R.id.buttonCreateAccount)
        buttonCreateAccount.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                createAccount()
            }
        })

        buttonBackup = view.findViewById<Button>(R.id.buttonBackup)
        buttonBackup.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                val options = ScanOptions()
                    .setPrompt("Scan account information on 1st device setting screen")
                    .setOrientationLocked(false)
                launcher.launch(options)
            }
        })

        if (isInitialized()) {
            if (isRegistered()) {
                findNavController().navigate(R.id.action_welcome_to_home)
            } else {
                findNavController().navigate(R.id.action_welcome_to_notice)
            }
        }

        return view
    }

    private fun isInitialized(): Boolean {
        val activity = activity as MainActivity
        return activity.accountManager.getAddress() != null
    }

    private fun isRegistered(): Boolean {
        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()
        try {
            val pk = activity.flowManager.getPk()
            return activity.flowManager.getKeyIndex(FlowAddress(address!!), pk) != -1
        } catch (exception: java.lang.NullPointerException) {
            activity.accountManager.initKeyPair()
            return false
        }
    }

    private fun createAccount() {
        enableUIs(false)

        try {
            val activity = activity as MainActivity
            val pk = activity.accountManager.initKeyPair()

            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val address = activity.flowManager.createAccount(pk)
                activity.accountManager.setAddress(address)
                Log.d("WelcomeFragment", "address=${address}")

                val scope= CoroutineScope(Dispatchers.Main)
                scope.launch {
                    enableUIs(true)
                    findNavController().navigate(R.id.action_welcome_to_home)
                }
            }
        } catch (e: java.lang.Exception) {
            enableUIs(true)
            Snackbar
                .make(
                    this@WelcomeFragment.requireView(),
                    "Enable biometrics in system setting",
                    Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun enableUIs(enabled: Boolean) {
        buttonCreateAccount.isEnabled = enabled
        buttonBackup.isEnabled = enabled
        progressBarCreating.visibility = if (!enabled) VISIBLE else View.INVISIBLE
    }
}