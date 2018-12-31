package com.odys.smartoffice

import android.content.Context
import android.preference.PreferenceManager

abstract class PreferencesManager {
    companion object {
        private const val FIRST_LAUNCHED = "first_launch"
        private const val FIRST_LAUNCHED_DEF = true
        private const val SERVER_PUBLIC_KEY = "serv_pub_key"
        private const val SERVER_PUBLIC_KEY_DEF = ""
        private const val ANDROID_PUB_KEY = "and_pubkey"
        private const val ANDROID_PRIV_KEY = "and_privkeys"
        private const val ANDROID_KEYS_DEF = ""
        private const val USERNAME = "usernamee"
        private const val USERNAME_DEF = "DefaultUser0000"

        fun isFirstLaunched(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FIRST_LAUNCHED, FIRST_LAUNCHED_DEF)
        }

        fun setFirstLaunched(context: Context, launched: Boolean) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(FIRST_LAUNCHED, launched).apply()
        }

        fun isServerPublicKeySaved(context: Context): Boolean {
            val nana = PreferenceManager.getDefaultSharedPreferences(context).getString(SERVER_PUBLIC_KEY, SERVER_PUBLIC_KEY_DEF)
            return nana != SERVER_PUBLIC_KEY_DEF
        }

        fun getServerPublicKeySaved(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(SERVER_PUBLIC_KEY, SERVER_PUBLIC_KEY_DEF)
        }

        fun saveServerPublicKey(context: Context, serverPublicKey: String) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(SERVER_PUBLIC_KEY, serverPublicKey).apply()
        }

        fun areAndroidKeysSaved(context: Context): Boolean {
            val nana = PreferenceManager.getDefaultSharedPreferences(context).getString(ANDROID_PRIV_KEY, ANDROID_KEYS_DEF)
            val nana2 = PreferenceManager.getDefaultSharedPreferences(context).getString(ANDROID_PUB_KEY, ANDROID_KEYS_DEF)
            return (nana != ANDROID_KEYS_DEF && nana2 != ANDROID_KEYS_DEF)
        }

        fun getAndroidPublicKey(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(ANDROID_PUB_KEY, ANDROID_KEYS_DEF)
        }

        fun getAndroidPrivateKey(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(ANDROID_PRIV_KEY, ANDROID_KEYS_DEF)
        }

        fun saveAndroidKeys(context: Context, pubKey: String, privKey: String) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ANDROID_PUB_KEY, pubKey).apply()
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ANDROID_PRIV_KEY, privKey).apply()
        }

        fun getUsername(context: Context): String {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(USERNAME, USERNAME_DEF)
        }

        fun setUsername(context: Context, username: String) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(USERNAME, username).apply()
        }
    }
}