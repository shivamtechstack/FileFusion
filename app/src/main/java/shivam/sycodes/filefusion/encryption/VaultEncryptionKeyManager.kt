package shivam.sycodes.filefusion.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object VaultEncryptionKeyManager {
    private const val KEY_ALIAS = "VaultEncryptionKey"

    fun getKey() : SecretKey{
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return if(keyStore.containsAlias(KEY_ALIAS)){
            (keyStore.getEntry(KEY_ALIAS,null) as KeyStore.SecretKeyEntry).secretKey
        }else{
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }
}