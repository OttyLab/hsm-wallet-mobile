package com.example.multisigwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

class NoticeFragment : Fragment() {
    private lateinit var textViewPk: TextView
    private lateinit var imageViewQr: ImageView
    private lateinit var buttonDone: Button
    private lateinit var buttonReset: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notice, container, false)


        val activity = activity as MainActivity
        val pk = activity.flowManager.getPk()

        imageViewQr = view.findViewById(R.id.imageViewQr)
        imageViewQr.setImageBitmap(getBackupQr(pk))

        textViewPk = view.findViewById(R.id.textViewPk)
        textViewPk.setText("Public Key: ${pk}")

        buttonDone = view.findViewById(R.id.buttonDone)
        buttonDone.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_notice_to_welcome)
            }
        })

        buttonReset = view.findViewById(R.id.buttonReset)
        buttonReset.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                activity.accountManager.deleteAddress()
                findNavController().navigate(R.id.action_notice_to_welcome)
            }
        })

        return view
    }
}