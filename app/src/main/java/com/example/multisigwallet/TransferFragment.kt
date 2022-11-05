package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
                val activity = activity as MainActivity
                val scope= CoroutineScope(Dispatchers.IO)
                scope.launch{
                    var address = activity.accountManager.getAddress()!!
                    activity.flowManager.transfer(
                        address,
                        editTextTo.text.toString(),
                        BigDecimal(editTextAmount.text.toString()))
                }
            }
        })

        buttonCancel = view.findViewById(R.id.buttonCancel)
        buttonCancel.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_transfer_to_home)
            }
        })
        return view
    }
}