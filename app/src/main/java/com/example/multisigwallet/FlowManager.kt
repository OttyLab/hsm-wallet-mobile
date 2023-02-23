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
import com.nftco.flow.sdk.cadence.StringField
import io.grpc.StatusRuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FlowManager(host: String, port: Int, activity: FragmentActivity) {
    private val accessApi = Flow.newAccessApi(host, port)
    private val activity = activity
    private val signer = SignerImpl(activity)
    private var functions = Firebase.functions

    private val latestBlockId: FlowId get() = accessApi.getLatestBlockHeader().id

    fun getAccount(address: FlowAddress) = accessApi.getAccountAtLatestBlock(address)!!

    fun getAccountBalance(address: FlowAddress): BigDecimal {
        val account = getAccount(address)
        return account.balance
    }

    fun getKeyIndex(address: FlowAddress, pk: String): Int{
        val account = getAccount(address)
        return account.getKeyIndex(pk)
    }

    fun isMultisig(address: FlowAddress): Boolean {
        val account = getAccount(address)
        for(key in account.keys) {
            if(key.revoked) continue;
            if(key.publicKey != FlowPublicKey(signer.getPublicKey())) continue;
            return key.weight < 1000.0
        }

        return false;
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

    fun getPk(): String {
        return signer.getPublicKey()
    }

    fun switchToMultisig(sender: String): FlowId {
        val stream = activity.assets.open("switch_to_multisig.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }

        val txId = sendTransaction(sender, FlowScript(script), emptyList())
        Log.d("FlowManager", "switch to multisig txId=${txId.bytes.toHexString()}")

        return txId
    }

    fun transfer(from: String, to: String, amount: BigDecimal): FlowId {
        val stream = activity.assets.open("transfer.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }
        val args = listOf(
            FlowArgument(UFix64NumberField(amount.toDouble().toString())),
            FlowArgument(AddressField(to)),
        )

        val txId = sendTransaction(from, FlowScript(script), args)
        Log.d("FlowManager", "transfer txId=${txId.bytes.toHexString()}")

        return txId
    }

    fun createTransferTransaction(from: String, to: String, amount: BigDecimal): FlowTransaction?{
        val sender = FlowAddress(from)

        val keyIndex = getKeyIndex(sender, signer.getPublicKey())
        if (keyIndex == -1) {
            Log.e("FlowManager", "PK does not exiest")
            return null
        }

        return createTransferTransaction(from, to, amount, keyIndex)
    }

    fun createTransferTransaction(
        from: String,
        to: String,
        amount: BigDecimal,
        keyIndex:Int,
        blockId: FlowId = latestBlockId): FlowTransaction {
        val stream = activity.assets.open("transfer.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }
        val args = listOf(
            FlowArgument(UFix64NumberField(amount.toDouble().toString())),
            FlowArgument(AddressField(to)),
        )

        val sender = FlowAddress(from)
        return createTransaction(sender, FlowScript(script), args, keyIndex, blockId)
    }

    fun createAddBackupTransaction(from: String, pk: String): FlowTransaction? {
        val sender = FlowAddress(from)

        val keyIndex = getKeyIndex(sender, signer.getPublicKey())
        if (keyIndex == -1) {
            Log.e("FlowManager", "PK does not exiest")
            return null
        }

        return createAddBackupTransaction(from, pk, keyIndex, latestBlockId)
    }

    fun createAddBackupTransaction(from: String, pk: String, keyIndex:Int, blockId: FlowId = latestBlockId): FlowTransaction {
        val sender = FlowAddress(from)
        val stream = activity.assets.open("add_pk.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }
        val weight = if (isMultisig(sender)) "500.0" else "1000.0"
        val args = listOf(FlowArgument(StringField(pk)), FlowArgument(UFix64NumberField(weight)))

        return createTransaction(sender, FlowScript(script), args, keyIndex, blockId)
    }

    fun createSwitchToSinglesigTransaction(from: String): FlowTransaction? {
        val sender = FlowAddress(from)

        val keyIndex = getKeyIndex(sender, signer.getPublicKey())
        if (keyIndex == -1) {
            Log.e("FlowManager", "PK does not exiest")
            return null
        }

       return createSwitchToSinglesigTransaction(from, keyIndex, latestBlockId)
    }

    fun createSwitchToSinglesigTransaction(from: String, keyIndex:Int, blockId: FlowId = latestBlockId): FlowTransaction {
        val sender = FlowAddress(from)
        val stream = activity.assets.open("switch_to_singlesig.cdc")
        val script = InputStreamReader(stream).buffered().use { it.readText() }

        return createTransaction(sender, FlowScript(script), emptyList(), keyIndex, blockId)
    }

    fun signTransaction(tx: FlowTransaction, from: String): FlowTransaction? {
        val sender = FlowAddress(from)
        val keyIndex = getKeyIndex(sender, signer.getPublicKey())
        if (keyIndex == -1) {
            Log.e("FlowManager", "PK does not exiest")
            return null
        }

        return tx.addEnvelopeSignature(sender, keyIndex, signer)
    }

    fun sendTransferTransaction(tx: FlowTransaction): FlowId {
        return accessApi.sendTransaction(tx)
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

    private fun sendTransaction(sender: String, script:FlowScript, args: List<FlowArgument>): FlowId {
        val sender = FlowAddress(sender)

        val keyIndex = getKeyIndex(sender, signer.getPublicKey())
        if (keyIndex == -1) {
            Log.e("FlowManager", "PK does not exiest")
            return FlowId("0x0")
        }

        val tx = createTransaction(sender, script, args, keyIndex, latestBlockId)
        val signed = tx.addEnvelopeSignature(sender, keyIndex, signer)

        return accessApi.sendTransaction(signed)
    }

    private fun createTransaction(
        sender: FlowAddress,
        script:FlowScript,
        args: List<FlowArgument>,
        keyIndex: Int,
        blockId: FlowId,
    ): FlowTransaction {
        val key = getAccountKey(sender, keyIndex)
        return FlowTransaction(
            script = script,
            arguments = args,
            referenceBlockId = blockId,
            gasLimit = 100,
            proposalKey = FlowTransactionProposalKey(
                address = sender,
                keyIndex = keyIndex,
                sequenceNumber = key.sequenceNumber.toLong()
            ),
            payerAddress = sender,
            authorizers = listOf(sender)
        )
    }
}