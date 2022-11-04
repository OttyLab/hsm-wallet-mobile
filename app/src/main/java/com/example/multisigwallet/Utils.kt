package com.example.multisigwallet

fun ByteArray.toHexString() =
    asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }