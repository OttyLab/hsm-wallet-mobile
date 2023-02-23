package com.example.multisigwallet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {
    private lateinit var imageViewQr: ImageView
    private lateinit var textViewAddress: TextView
    private lateinit var buttonAddPk: Button
    private lateinit var buttonDelete: Button
    private lateinit var buttonBack: Button
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

    private fun getAddress(): String {
        val activity = activity as MainActivity
        return activity.accountManager.getAddress()!!
    }

    private fun addBackup(pk: String) {
        buttonAddPk.isEnabled = false
        buttonDelete.isEnabled = false
        buttonBack.isEnabled = false
        progressBarAdding.visibility = VISIBLE

        val activity = activity as MainActivity
        val sender = activity.accountManager.getAddress()!!
        val scope= CoroutineScope(Dispatchers.IO)

        scope.launch{
            try {
                val txId = activity.flowManager.addPk(sender, pk)
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
                buttonAddPk.isEnabled = true
                buttonDelete.isEnabled = true
                buttonBack.isEnabled = true
                progressBarAdding.visibility = INVISIBLE
            }
        }

    }
}