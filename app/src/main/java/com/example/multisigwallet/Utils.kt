package com.example.multisigwallet

import android.graphics.Bitmap

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

fun ByteArray.toHexString() =
    asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

val SCHEME = "lhsmw:"

fun readAccount(uri: String): String? {
    val regex = Regex("${SCHEME}//account/(0x[0-9a-z]+)")
    val match = regex.find(uri)
    if (match == null) {return null}
    return match.groups[1]?.value.toString()
}

fun readBackup(uri: String): String? {
    val regex = Regex("${SCHEME}//backup/([0-9a-z]+)")
    val match = regex.find(uri)
    if (match == null) {return null}
    return match.groups[1]?.value.toString()
}

fun getAccountQr(address: String): Bitmap {
    return generateQrBitmap("${SCHEME}//account/${address}")
}

fun getBackupQr(address: String): Bitmap {
    return generateQrBitmap("${SCHEME}//backup/${address}")
}

private fun generateQrBitmap(address: String): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix = writer.encode(address, BarcodeFormat.QR_CODE, 400, 400)
    return BarcodeEncoder().createBitmap(bitMatrix)
}
