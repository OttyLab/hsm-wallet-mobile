package com.example.multisigwallet

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.functions.ktx.functions
import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.UFix64NumberField
import java.io.InputStreamReader
import java.math.BigDecimal
import com.google.firebase.ktx.Firebase
import io.grpc.StatusRuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FlowManager(host: String, port: Int, activity: FragmentActivity) {
    private val accessApi = Flow.newAccessApi(host, port)
    private val activity = activity
    private var functions = Firebase.functions

    private val latestBlockId: FlowId get() = accessApi.getLatestBlockHeader().id

    fun getAccount(address: FlowAddress) = accessApi.getAccountAtLatestBlock(address)!!

    fun getAccountBalance(address: FlowAddress): BigDecimal {
        val account = getAccount(address)
        return account.balance
    }

    suspend fun createAccount(pk: String): String {
        return suspendCoroutine { continuation ->
            functions
                .getHttpsCallable("account")
                .call(hashMapOf("pk" to pk))
                .addOnSuccessListener { result ->
                    Log.d("FlowManager", "create account txId=${result.data}")
                    val result = waitForSeal(FlowId(result.data.toString()))
                    val events = result.getEventsOfType("flow.AccountCreated", true)
                    val address = events[0].value!!.fields[0].value.value.toString()
                    continuation.resume(address)
                }
        }
    }

    fun transfer(from: String, to: String, amount: BigDecimal): FlowId {
        val sender = FlowAddress(from)
        val key = getAccountKey(sender, 0)
        val stream = activity.assets.open("transfer.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }
        var tx = FlowTransaction(
            script = FlowScript(script),
            arguments = listOf(
                FlowArgument(UFix64NumberField(amount.toDouble().toString())),
                FlowArgument(AddressField(to))),
            referenceBlockId = latestBlockId,
            gasLimit = 100,
            proposalKey = FlowTransactionProposalKey(
                address = sender,
                keyIndex = 0,
                sequenceNumber = key.sequenceNumber.toLong()
            ),
            payerAddress = sender,
            authorizers = listOf(sender)
        )

        val signer = SignerImpl(activity)
        tx = tx.addEnvelopeSignature(sender, 0, signer)
        val txId = accessApi.sendTransaction(tx)
        Log.d("FlowManager", "transfer txId=${txId.bytes.toHexString()}")
        return txId
    }

    fun waitForSeal(txID: FlowId): FlowTransactionResult {
        var txResult: FlowTransactionResult
        while (true) {
            try {
                txResult = getTransactionResult(txID)
                if (txResult.status == FlowTransactionStatus.SEALED) {
                    return txResult
                }
            } catch (e: StatusRuntimeException){}
            Thread.sleep(1000)
        }
    }

    private fun getAccountKey(address: FlowAddress, keyIndex: Int): FlowAccountKey {
        val account = getAccount(address)
        return account.keys[keyIndex]
    }

    private fun getTransactionResult(txID: FlowId): FlowTransactionResult {
        val txResult = accessApi.getTransactionResultById(txID)!!
        if (txResult.errorMessage.isNotEmpty()) {
            throw Exception(txResult.errorMessage)
        }
        return txResult
    }
}