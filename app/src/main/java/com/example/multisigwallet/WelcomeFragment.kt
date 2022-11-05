package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        val button = view.findViewById<Button>(R.id.buttonCreateAccount)
        button.setOnClickListener(object : View.OnClickListener{
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
        val activity = activity as MainActivity
        val pk = activity.accountManager.initKeyPair()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val address = activity.flowManager.createAccount(pk)
            activity.accountManager.setAddress(address)
            Log.d("WelcomeFragment", "address=${address}")
            findNavController().navigate(R.id.action_welcome_to_home)
        }
    }
}