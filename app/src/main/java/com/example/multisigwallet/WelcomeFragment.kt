package com.example.multisigwallet

import android.opengl.Visibility
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {
    lateinit var progressBarCreating: ProgressBar
    lateinit var buttonCreateAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        progressBarCreating = view.findViewById(R.id.progressBarCreating)
        buttonCreateAccount = view.findViewById<Button>(R.id.buttonCreateAccount)
        buttonCreateAccount.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                createAccount()
            }
        })

        if (isInitialized()) {
            findNavController().navigate(R.id.action_welcome_to_home)
        }

        return view
    }

    private fun isInitialized(): Boolean {
        val activity = activity as MainActivity
        return activity.accountManager.getAddress() != null
    }

    private fun createAccount() {
        buttonCreateAccount.isEnabled = false
        progressBarCreating.visibility = VISIBLE

        val activity = activity as MainActivity
        val pk = activity.accountManager.initKeyPair()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val address = activity.flowManager.createAccount(pk)
            activity.accountManager.setAddress(address)
            Log.d("WelcomeFragment", "address=${address}")

            val scope= CoroutineScope(Dispatchers.Main)
            scope.launch {
                findNavController().navigate(R.id.action_welcome_to_home)
            }
        }
    }
}