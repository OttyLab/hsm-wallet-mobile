package com.example.multisigwallet

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.nftco.flow.sdk.HashAlgorithm
import com.nftco.flow.sdk.Hasher
import com.nftco.flow.sdk.Signer
import kotlinx.coroutines.*
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignerImpl(activity: FragmentActivity) : Signer {
    override val hasher: Hasher = HasherImpl(HashAlgorithm.SHA2_256)
    private val activity = activity

    override fun sign(bytes: ByteArray): ByteArray {
        val scope= CoroutineScope(Dispatchers.Main)
        var signature: ByteArray? = null
        runBlocking {
            scope.launch{
                signature = signInternal(bytes)
            }.join()
        }

        return signature!!
    }

    private suspend fun signInternal(message: ByteArray): ByteArray {
        return suspendCoroutine { continuation ->
            showFingerprint { cryotoObject ->
                run {
                    val signer = cryotoObject!!.signature!!
                    signer.update(message)
                    val signature = signer.sign()
                    continuation.resume(formatSignagure(signature))
                }
            }
        }
    }

    private fun getCryptoObject(): BiometricPrompt.CryptoObject {
        val ks = KeyStore.getInstance(AccountManager.PROVIDER).apply { load(null) }
        val entry = ks.getEntry(AccountManager.ALIAS, null) as KeyStore.PrivateKeyEntry
        val signature = Signature.getInstance(AccountManager.ALGORIGHM)

        val pk = entry.certificate.publicKey.encoded.slice(27..90)

        signature.initSign(entry.privateKey)
        return BiometricPrompt.CryptoObject(signature)
    }

    private fun showFingerprint(cb: (cryptoObject: BiometricPrompt.CryptoObject?) -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("title")
            .setSubtitle("subtitle")
            .setDescription("description")
            .setNegativeButtonText("negative")
            .build()

        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(activity, executor, object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d("SignerImpl", "authentication pass")
                cb(result.cryptoObject)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d("SignerImpl", "authentication error")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("SignerImpl", "authentication failed")
            }
        })

        prompt.authenticate(promptInfo, getCryptoObject())
    }

    //https://bitcoin.stackexchange.com/questions/92680/what-are-the-der-signature-and-sec-format
    private fun formatSignagure(signature: ByteArray): ByteArray {
        var rSize = signature[3].toInt()
        var offset = 4
        if (rSize == 0x21) {
            Log.d("SignerImpl", "rSize==0x21")
            offset = 5
            rSize -= 1
        }

        val r = signature.slice(offset.. offset + rSize - 1)
        var sSize = signature[offset + rSize + 1].toInt()
        if (sSize == 0x21) {
            Log.d("SignerImpl", "sSize==0x21")
            offset += 1
            sSize -= 1
        }
        val s = signature.slice(offset + rSize + 2.. offset + rSize + 2 + sSize -1)
        return (r + s).toByteArray()
    }
}

internal class HasherImpl(
    private val hashAlgo: HashAlgorithm
) : Hasher {

    override fun hash(bytes: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(hashAlgo.algorithm)
        return digest.digest(bytes)
    }
}

