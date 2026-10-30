package com.ecoruta.dispositivo.fakes

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.local.prefs.TokenCipher
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Stands in for the Android Keystore, which does not exist on the JVM. Reversible on purpose. */
class FakeTokenCipher : TokenCipher {
    override fun encrypt(plainText: String) = "enc:$plainText"

    override fun decrypt(encoded: String) = encoded.removePrefix("enc:")
}

/** Real DataStore on a file in [folder], so the tests exercise the actual persistence code. */
fun testPreferences(folder: File, scope: CoroutineScope) = DevicePreferences(
    store = PreferenceDataStoreFactory.create(scope = scope) { File(folder, "device.preferences_pb") },
    cipher = FakeTokenCipher(),
)
