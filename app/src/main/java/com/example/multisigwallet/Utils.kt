package com.example.multisigwallet

import android.graphics.Bitmap

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.nftco.flow.sdk.FlowId
import com.nftco.flow.sdk.FlowTransactionSignature
import java.util.*

fun ByteArray.toHexString() =
    asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

val SCHEME = "lhsmw:"

fun readAccount(uri: String): String? {
    val regex = Regex("${SCHEME}//account/(0x[0-9a-z]+)")
    val match = regex.find(uri) ?: return null
    return match.groups[1]?.value.toString()
}

fun readBackup(uri: String): String? {
    val regex = Regex("${SCHEME}//backup/([0-9a-z]+)")
    val match = regex.find(uri) ?: return null
    return match.groups[1]?.value.toString()
}

data class TxInfo (
    val signature: ByteArray,
    val blockId: ByteArray,
    val keyIndex: Int,
    val to: String,
    val amount: String,
)

fun readTransaction(uri: String): TxInfo? {
    val regex = Regex("${SCHEME}//(transfer|switch-to-singlesig|add-backup)/.*$")
    val match = regex.find(uri) ?: return null

    return when(match.groups[1]?.value!!) {
        "transfer" -> readTransferTransaction(uri)
        "switch-to-singlesig" -> readSwitchToSinglesigTransaction(uri)
        "add-backup" -> readAddBackupTransaction(uri)
        else -> null
    }
}

fun readTransferTransaction(uri: String): TxInfo? {
    val regex = Regex("${SCHEME}//transfer/(.+)-(.+)-([0-9]+)-(0x[0-9a-z]+)-([0-9.]+)")
    val match = regex.find(uri) ?: return null

    val signature = Base64.getDecoder().decode(match.groups[1]?.value.toString())
    val blockId = Base64.getDecoder().decode(match.groups[2]?.value.toString())
    val keyIndex = match.groups[3]?.value!!.toInt()
    val to = match.groups[4]?.value!!
    val amount = match.groups[5]?.value!!

    return TxInfo(signature, blockId, keyIndex, to, amount)
}

fun readAddBackupTransaction(uri: String): TxInfo? {
    val regex = Regex("${SCHEME}//add-backup/(.+)-(.+)-([0-9]+)-([0-9a-z]+)")
    val match = regex.find(uri) ?: return null

    val signature = Base64.getDecoder().decode(match.groups[1]?.value.toString())
    val blockId = Base64.getDecoder().decode(match.groups[2]?.value.toString())
    val keyIndex = match.groups[3]?.value!!.toInt()
    val pk = match.groups[4]?.value!!.toString()

    return TxInfo(signature, blockId, keyIndex, "add backup", pk)
}

fun readSwitchToSinglesigTransaction(uri: String): TxInfo? {
    val regex = Regex("${SCHEME}//switch-to-singlesig/(.+)-(.+)-([0-9]+)")
    val match = regex.find(uri) ?: return null

    val signature = Base64.getDecoder().decode(match.groups[1]?.value.toString())
    val blockId = Base64.getDecoder().decode(match.groups[2]?.value.toString())
    val keyIndex = match.groups[3]?.value!!.toInt()

    return TxInfo(signature, blockId, keyIndex, "switch to singlesig", "0")
}

fun getAccountQr(address: String): Bitmap {
    return generateQrBitmap("${SCHEME}//account/${address}")
}

fun getBackupQr(address: String): Bitmap {
    return generateQrBitmap("${SCHEME}//backup/${address}")
}

fun getTransferTransactionQr(
    flowTransactionSignature: FlowTransactionSignature,
    blockId: FlowId,
    to: String,
    amount: String): Bitmap {
    val signature = Base64.getEncoder().encodeToString(flowTransactionSignature.signature.bytes)
    val block = Base64.getEncoder().encodeToString(blockId.bytes)
    return generateQrBitmap("${SCHEME}//transfer/${signature}-${block}-${flowTransactionSignature.keyIndex}-${to}-${amount}" )
}

fun getAddBackupTransactionQr(
    flowTransactionSignature: FlowTransactionSignature,
    blockId: FlowId,
    pk: String): Bitmap {
    val signature = Base64.getEncoder().encodeToString(flowTransactionSignature.signature.bytes)
    val block = Base64.getEncoder().encodeToString(blockId.bytes)
    return generateQrBitmap("${SCHEME}//add-backup/${signature}-${block}-${flowTransactionSignature.keyIndex}-${pk}" )
}

fun getSwitchToSinglesigTransactionQr(
    flowTransactionSignature: FlowTransactionSignature, blockId: FlowId): Bitmap {
    val signature = Base64.getEncoder().encodeToString(flowTransactionSignature.signature.bytes)
    val block = Base64.getEncoder().encodeToString(blockId.bytes)
    return generateQrBitmap("${SCHEME}//switch-to-singlesig/${signature}-${block}-${flowTransactionSignature.keyIndex}" )
}

private fun generateQrBitmap(address: String, size: Int = 400): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix = writer.encode(address, BarcodeFormat.QR_CODE, size, size)
    return BarcodeEncoder().createBitmap(bitMatrix)
}
