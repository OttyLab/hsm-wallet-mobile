package com.example.multisigwallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.nftco.flow.sdk.FlowAddress
import kotlinx.coroutines.*
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    private lateinit var editTextAmount: EditText
    private lateinit var buttonSendTx: Button

    private lateinit var accountManager: AccountManager
    private lateinit var flowManager: FlowManager

    private lateinit var address: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flowManager = FlowManager("access.devnet.nodes.onflow.org", 9000, this@MainActivity)
        accountManager = AccountManager(this)
        if (accountManager.getAddress() == null) {
            val pk = accountManager.initKeyPair()
            val scope= CoroutineScope(Dispatchers.IO)
            scope.launch{
                address = flowManager.createAccount(pk)
                accountManager.setAddress(address)
                Log.d("TAG", "address=${address}")
            }
        } else {
            address = accountManager.getAddress()!!
            Log.d("TAG", "address=${address}")
        }

        editTextAmount = findViewById(R.id.editTextAmount)

        buttonSendTx = findViewById(R.id.buttonSendTx)
        buttonSendTx.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                val balance = flowManager.getAccountBalance(FlowAddress(address))
                Log.d("TAG", "Balance=${balance}")

                val scope= CoroutineScope(Dispatchers.IO)
                scope.launch{
                    flowManager.transfer(address, "0xbf54f5b00e5329c4", BigDecimal(1.2))
                }
            }
        })

        //flowManager = FlowManager("192.168.127.249", 3569, this@MainActivity)
        //flowManager = FlowManager("rest-testnet.onflow.org", 443, this@MainActivity)
    }
}