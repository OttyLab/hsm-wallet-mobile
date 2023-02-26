package com.example.multisigwallet

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.nftco.flow.sdk.FlowAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.util.Timer;
import java.util.TimerTask

class HomeFragment : Fragment() {
    private lateinit var address: String
    private lateinit var textBalance: TextView
    private lateinit var buttonTransfer: Button
    private lateinit var buttonMultisigTransfer: Button
    private lateinit var buttonSignTransfer: Button
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.actionSettings -> {
            findNavController().navigate(R.id.action_home_to_setting)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        textBalance = view.findViewById<TextView>(R.id.textBalance)
        textBalance.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_home_to_receive)
            }
        })

        buttonTransfer = view.findViewById<Button>(R.id.buttonTransfer)
        buttonTransfer.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_home_to_transfer)
            }
        })

        buttonMultisigTransfer = view.findViewById<Button>(R.id.buttonMultisigTransfer)
        buttonMultisigTransfer.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_home_to_multisig_transfer)
            }
        })

        buttonSignTransfer = view.findViewById<Button>(R.id.buttonSignTransfer)
        buttonSignTransfer.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                findNavController().navigate(R.id.action_home_to_sign_transfer)
            }
        })

        initAddress()

        if(isMultisig()) {
            buttonTransfer.visibility = View.GONE
        } else {
            buttonMultisigTransfer.visibility = View.GONE
            buttonSignTransfer.visibility = View.GONE
        }

        timer = Timer()
        timer.scheduleAtFixedRate(object: TimerTask(){
            override fun run() {
                val scope= CoroutineScope(Dispatchers.Main)
                scope.launch {
                    updateBalance()
                }
            }
        }, 0, 10000)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
    }

    private fun initAddress() {
        val activity = activity as MainActivity
        address = activity.accountManager.getAddress()!!
    }

    private fun updateBalance() {
        val activity = activity as MainActivity
        val balance = activity.flowManager.getAccountBalance(FlowAddress(address))
        textBalance.setText("${balance.setScale(5, RoundingMode.UP).toDouble()} FLOW")
    }

    private fun isMultisig(): Boolean {
        val activity = activity as MainActivity
        return activity.flowManager.isMultisig(FlowAddress(address))
    }
}