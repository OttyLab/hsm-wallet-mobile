package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nftco.flow.sdk.FlowAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {
    private lateinit var imageViewQr: ImageView
    private lateinit var textViewAddress: TextView
    private lateinit var buttonAddPk: Button
    private lateinit var switchEnableMultisig: Switch
    private lateinit var buttonDelete: Button
    private lateinit var progressBarAdding: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val launcher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val code = result.contents.toString()
                val pk = readBackup(code)

                if(pk == null) {
                    Toast.makeText(this.context, "Unexpected QR code: ${code}", Toast.LENGTH_SHORT).show()
                } else {
                    addBackup(pk)
                }
            }
        }

        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val address = getAddress()
        imageViewQr = view.findViewById(R.id.imageViewQr)
        imageViewQr.setImageBitmap(getAccountQr(address))

        textViewAddress = view.findViewById(R.id.textViewAddress)
        textViewAddress.setText(address)
        Log.d("TAG", address)

        buttonAddPk = view.findViewById(R.id.buttonAddPk)
        buttonAddPk.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                val options = ScanOptions()
                    .setPrompt("Scan public key on 2st device")
                    .setOrientationLocked(false)
                launcher.launch(options)
            }
        })

        switchEnableMultisig = view.findViewById(R.id.switchEnableMultisig)
        switchEnableMultisig.isChecked = isMultisig()
        switchEnableMultisig.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked == isMultisig()) {
                    return;
                }

                if(isMultisig()) {
                    switchToSinglesig()
                } else {
                    switchToMultisig()
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

        progressBarAdding = view.findViewById(R.id.progressBarAdding)

        return view
    }

    private fun getAddress(): String {
        val activity = activity as MainActivity
        return activity.accountManager.getAddress()!!
    }

    private fun addBackup(pk: String) {
        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()!!

        if (isMultisig()) {
            val scope= CoroutineScope(Dispatchers.IO)
            scope.launch{
                val tx = activity.flowManager.createAddBackupTransaction(address, pk)
                val txSigned = activity.flowManager.signTransaction(tx!!, address)
                val scope= CoroutineScope(Dispatchers.Main)
                scope.launch {
                    imageViewQr.setImageBitmap(getAddBackupTransactionQr(txSigned!!.envelopeSignatures[0], txSigned.referenceBlockId, pk))
                }
            }
        } else {
            enableUIs(false)

            val scope= CoroutineScope(Dispatchers.IO)
            scope.launch{
                try {
                    val tx = activity.flowManager.createAddBackupTransaction(address, pk)
                    val txSigned = activity.flowManager.signTransaction(tx!!, address)
                    val txId = activity.flowManager.sendTransferTransaction(txSigned!!)
                    activity.flowManager.waitForSeal(txId)
                } catch (e: Exception) {
                    Snackbar
                        .make(
                            this@SettingFragment.requireView(),
                            "Transaction error. Maybe balance is too low.",
                            Snackbar.LENGTH_LONG)
                        .show()
                }

                val scope= CoroutineScope(Dispatchers.Main)
                scope.launch {
                    enableUIs(true)
                }
            }
        }
    }

    private fun switchToSinglesig() {
        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()!!

        val scope= CoroutineScope(Dispatchers.IO)
        scope.launch{
            val tx = activity.flowManager.createSwitchToSinglesigTransaction(address)
            val txSigned = activity.flowManager.signTransaction(tx!!, address)
            val scope= CoroutineScope(Dispatchers.Main)
            scope.launch {
                imageViewQr.setImageBitmap(getSwitchToSinglesigTransactionQr(txSigned!!.envelopeSignatures[0], txSigned.referenceBlockId))
            }
        }
    }

    private fun switchToMultisig() {
        enableUIs(false)

        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()!!
        val account = activity.flowManager.getAccount(FlowAddress(address))

        var valids = 0
        for (key in account.keys) {
            if (key.revoked) continue
            if (key.weight < 1000.0) continue
            valids++
        }

        if (valids < 2) {
            Snackbar
                .make(
                    this@SettingFragment.requireView(),
                    "Make at least one backup",
                    Snackbar.LENGTH_LONG)
                .show()
            return;
        }

        val scope= CoroutineScope(Dispatchers.IO)
        scope.launch{
            try {
                val txId = activity.flowManager.switchToMultisig(address);
                activity.flowManager.waitForSeal(txId)
            } catch (e: Exception) {
                Snackbar
                    .make(
                        this@SettingFragment.requireView(),
                        "Transaction error",
                        Snackbar.LENGTH_LONG)
                    .show()
            }

            val scope= CoroutineScope(Dispatchers.Main)
            scope.launch {
                enableUIs(true)
            }
        }
    }

    private fun isMultisig(): Boolean {
        val activity = activity as MainActivity
        val address = activity.accountManager.getAddress()!!
        return activity.flowManager.isMultisig(FlowAddress(address))
    }

    private fun enableUIs(enabled: Boolean) {
        buttonAddPk.isEnabled = enabled
        switchEnableMultisig.isEnabled = enabled
        buttonDelete.isEnabled = enabled
        progressBarAdding.visibility = if (!enabled) VISIBLE else INVISIBLE
    }
}