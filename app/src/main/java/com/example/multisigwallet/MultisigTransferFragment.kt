package com.example.multisigwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MultisigTransferFragment: Fragment() {
    private lateinit var editTextTo: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var buttonPrepare: Button
    private lateinit var imageViewQr: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_multisig_transfer, container, false)

        editTextTo = view.findViewById(R.id.editTextTo)
        editTextAmount = view.findViewById(R.id.editTextAmount)

        buttonPrepare = view.findViewById(R.id.buttonPrepare)
        buttonPrepare.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                val activity = activity as MainActivity
                val address = activity.accountManager.getAddress()!!
                val scope= CoroutineScope(Dispatchers.IO)
                val to = editTextTo.text.toString()
                val amount = editTextAmount.text.toString()
                scope.launch{
                    val tx = activity.flowManager.createTransferTransaction(
                        address,
                        to,
                        BigDecimal(amount)
                    )

                    val txSigned = activity.flowManager.signTransaction(tx!!, address)
                    val scope= CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        imageViewQr.setImageBitmap(getTransferTransactionQr(txSigned!!.envelopeSignatures[0], txSigned.referenceBlockId, to, amount ))
                    }
                }
            }
        })

        imageViewQr = view.findViewById(R.id.imageViewQr)

        return view
    }
}