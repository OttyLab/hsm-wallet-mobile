package com.example.multisigwallet

import android.os.Bundle
import android.service.voice.VoiceInteractionSession.VisibleActivityCallback
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TransferFragment : Fragment() {
    private lateinit var editTextTo: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var buttonTransfer: Button
    private lateinit var buttonCancel: Button
    private lateinit var progressBarTranferring: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transfer, container, false)

        editTextTo = view.findViewById(R.id.editTextTo)
        editTextAmount = view.findViewById(R.id.editTextAmount)

        buttonTransfer = view.findViewById(R.id.buttonTransfer)
        buttonTransfer.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                editTextTo.isEnabled = false
                editTextAmount.isEnabled = false
                buttonTransfer.isEnabled = false
                buttonCancel.isEnabled = false
                progressBarTranferring.visibility = VISIBLE

                val activity = activity as MainActivity
                val scope= CoroutineScope(Dispatchers.IO)
                scope.launch{
                    val address = activity.accountManager.getAddress()!!
                    val txId = activity.flowManager.transfer(
                        address,
                        editTextTo.text.toString(),
                        BigDecimal(editTextAmount.text.toString()))
                    activity.flowManager.waitForSeal(txId)

                    val scope= CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        findNavController().navigate(R.id.action_transfer_to_home)
                    }
                }
            }
        })

        buttonCancel = view.findViewById(R.id.buttonCancel)
        buttonCancel.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_transfer_to_home)
            }
        })

        progressBarTranferring = view.findViewById(R.id.progressBarTransfering)

        return view
    }
}