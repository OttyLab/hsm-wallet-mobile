package com.example.multisigwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

class ReceiveFragment : Fragment() {
    private lateinit var imageViewQr: ImageView
    private lateinit var textAddress: TextView
    private lateinit var buttonCopy: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_receive, container, false)

        imageViewQr = view.findViewById(R.id.imageViewQr)
        textAddress = view.findViewById(R.id.textAddress)
        buttonCopy = view.findViewById(R.id.buttonCopy)
        buttonCopy.setOnClickListener(object: OnClickListener{
            override fun onClick(v: View?) {
                val address = getAddress()
                val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Flow address", address))
                Snackbar.make(view, "Copied", Snackbar.LENGTH_SHORT).show()
            }
        })

        val address = getAddress()
        showQr(address)
        textAddress.setText(address)

        return view
    }

    private fun getAddress(): String {
        val activity = activity as MainActivity
        return activity.accountManager.getAddress()!!
    }

    private fun showQr(address: String) {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(address, BarcodeFormat.QR_CODE, 400, 400)
        val bitmap = BarcodeEncoder().createBitmap(bitMatrix)
        imageViewQr.setImageBitmap(bitmap)
    }
}