package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowSignature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SignTransferFragment: Fragment() {
    private lateinit var editTextTo: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var buttonTransfer: Button
    private lateinit var progressBarTranferring: ProgressBar

    private var txInfo: TxInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val launcher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val code = result.contents.toString()
                txInfo = readTransaction(code)

                if(txInfo == null) {
                    Toast.makeText(this.context, "Unexpected QR code", Toast.LENGTH_SHORT).show()
                } else {
                    editTextTo.setText(txInfo?.to)
                    editTextAmount.setText((txInfo?.amount))
                    buttonTransfer.setEnabled(true)
                }
            }
        }

        val view = inflater.inflate(R.layout.fragment_sign_transfer, container, false)

        editTextTo = view.findViewById(R.id.editTextTo)
        editTextTo.setEnabled(false)
        editTextAmount = view.findViewById(R.id.editTextAmount)
        editTextAmount.setEnabled(false)

        buttonTransfer = view.findViewById(R.id.buttonTransfer)
        buttonTransfer.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                signTx(txInfo!!)
            }
        })
        buttonTransfer.setEnabled(false)

        progressBarTranferring = view.findViewById(R.id.progressBarTransfering)

        val options = ScanOptions()
            .setPrompt("Scan Transaction")
            .setOrientationLocked(false)
        launcher.launch(options)

        return view
    }

    private fun signTx(txInfo: TxInfo) {
        buttonTransfer.setEnabled(false)
        progressBarTranferring.visibility = View.VISIBLE

        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()!!
        val scope= CoroutineScope(Dispatchers.IO)
        scope.launch{

            val tx = when(txInfo.to) {
                "switch to singlesig" ->
                    activity.flowManager.createSwitchToSinglesigTransaction(
                        address,
                        txInfo.keyIndex,
                        FlowId.of(txInfo.blockId))
                "add backup" ->
                    activity.flowManager.createAddBackupTransaction(
                        address,
                        txInfo.amount,
                        txInfo.keyIndex,
                        FlowId.of(txInfo.blockId))
                else ->
                    activity.flowManager.createTransferTransaction(
                        address,
                        txInfo.to,
                        BigDecimal(txInfo.amount),
                        txInfo.keyIndex,
                        FlowId.of(txInfo.blockId))
            }

            val txHalf = activity.flowManager.signTransaction(tx, address)
            val txFull = txHalf!!.addEnvelopeSignature(FlowAddress(address), txInfo.keyIndex, FlowSignature(txInfo.signature))
            val txId = activity.flowManager.sendTransferTransaction(txFull)
            activity.flowManager.waitForSeal(txId)

            Log.d("SignTransferFragment", "transfer txId=${txId.bytes.toHexString()}")

            val scope= CoroutineScope(Dispatchers.Main)
            scope.launch {
                buttonTransfer.setEnabled(true)
                progressBarTranferring.visibility = View.INVISIBLE
                findNavController().navigate(R.id.action_sign_transfer_to_home)
            }
        }

    }
}