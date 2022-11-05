package com.example.multisigwallet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.findNavController

class BackupFragment : Fragment() {
    private lateinit var editTextAddress: EditText
    private lateinit var buttonSetup: Button
    private lateinit var buttonBack: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_backup, container, false)

        editTextAddress = view.findViewById(R.id.editTextAddress)

        buttonSetup = view.findViewById(R.id.buttonSetup)
        buttonSetup.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                val activity = activity as MainActivity
                activity.accountManager.setAddress(editTextAddress.text.toString())
                activity.accountManager.initKeyPair()
                findNavController().navigate(R.id.action_backup_to_notice)
            }
        })

        buttonBack = view.findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_backup_to_welcome)
            }
        })

        return view
    }
}