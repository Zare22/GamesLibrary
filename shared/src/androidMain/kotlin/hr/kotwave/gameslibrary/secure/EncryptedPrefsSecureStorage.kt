package hr.kotwave.gameslibrary.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android [SecureStorage] backed by EncryptedSharedPreferences (AES via an Android Keystore master key). */
internal class EncryptedPrefsSecureStorage(context: Context) : SecureStorage {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "gameslibrary_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) { prefs.getString(key, null) }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(key).apply()
    }
}
