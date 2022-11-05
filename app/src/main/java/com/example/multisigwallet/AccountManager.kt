package com.example.multisigwallet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.fragment.app.FragmentActivity
import java.security.KeyPairGenerator

class AccountManager (activity: FragmentActivity){
    private val activity = activity

    fun getAddress(): String? {
        val sharedPreference = activity.getSharedPreferences("account", Context.MODE_PRIVATE)
        return sharedPreference.getString(ADDRESS_KEY, null)
    }

    fun setAddress(address: String) {
        val sharedPreference = activity.getSharedPreferences("account", Context.MODE_PRIVATE)
        with(sharedPreference.edit()) {
            putString(ADDRESS_KEY, address)
            apply()
        }
    }

    fun deleteAddress() {
        val sharedPreference = activity.getSharedPreferences("account", Context.MODE_PRIVATE)
        sharedPreference.edit().remove(ADDRESS_KEY).apply()
    }

    fun initKeyPair(): String {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            PROVIDER
        )
        val parameterSpecBuilder = KeyGenParameterSpec
            .Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        kpg.initialize(parameterSpecBuilder.build())
        val kp = kpg.generateKeyPair()
        return kp.public.encoded.slice(27..90).toByteArray().toHexString()
    }

    companion object {
        const val ADDRESS_KEY = "address"
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "MAIN_EC_KEY"
        const val ALGORIGHM = "SHA256withECDSA"
    }
}