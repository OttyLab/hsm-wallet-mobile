package com.example.multisigwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TransferFragment : Fragment() {
    private lateinit var editTextTo: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var buttonTransfer: Button
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
                enableUIs(false)

                val activity = activity as MainActivity
                val scope= CoroutineScope(Dispatchers.IO)
                scope.launch{
                    val address = activity.accountManager.getAddress()!!
                    val txId = activity.flowManager.transfer(
                        address,
                        editTextTo.text.toString(),
                        BigDecimal(editTextAmount.text.toString()))

                    try {
                        activity.flowManager.waitForSeal(txId)
                        val scope= CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            findNavController().navigate(R.id.action_transfer_to_home)
                        }
                    } catch(e: Exception) {
                        Snackbar
                            .make(
                                this@TransferFragment.requireView(),
                                "Transaction failed. txid: ${txId.bytes.toHexString()}",
                                Snackbar.LENGTH_LONG)
                            .show()
                    } finally {
                        val scope= CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            enableUIs(true)
                        }
                    }
                }
            }
        })

        progressBarTranferring = view.findViewById(R.id.progressBarTransfering)

        return view
    }

    private fun enableUIs(enabled: Boolean) {
        editTextTo.isEnabled = enabled
        editTextAmount.isEnabled = enabled
        buttonTransfer.isEnabled = enabled
        progressBarTranferring.visibility = if (!enabled) VISIBLE else View.INVISIBLE
    }
}