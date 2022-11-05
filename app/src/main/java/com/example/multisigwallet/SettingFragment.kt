package com.example.multisigwallet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {
    lateinit var editTextPk: EditText
    lateinit var buttonAddPk: Button
    lateinit var buttonDelete: Button
    lateinit var buttonBack: Button
    lateinit var progressBarAdding: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        editTextPk = view.findViewById(R.id.editTextPk)

        buttonAddPk = view.findViewById(R.id.buttonAddPk)
        buttonAddPk.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                editTextPk.isEnabled = false
                buttonAddPk.isEnabled = false
                buttonBack.isEnabled = false
                progressBarAdding.visibility = VISIBLE

                val activity = activity as MainActivity
                val sender = activity.accountManager.getAddress()!!
                val scope= CoroutineScope(Dispatchers.IO)

                scope.launch{
                    val txId = activity.flowManager.addPk(sender, editTextPk.text.toString())
                    activity.flowManager.waitForSeal(txId)

                    val scope= CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        editTextPk.isEnabled = true
                        buttonAddPk.isEnabled = true
                        buttonBack.isEnabled = true
                        progressBarAdding.visibility = INVISIBLE
                    }
                }
            }
        })

        buttonDelete = view.findViewById(R.id.buttonDelete)
        buttonDelete.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                val activity = activity as MainActivity
                activity.accountManager.deleteAddress()
            }
        })

        buttonBack = view.findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_setting_to_home)
            }
        })

        progressBarAdding = view.findViewById(R.id.progressBarAdding)

        return view
    }
}