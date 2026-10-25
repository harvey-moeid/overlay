package com.byga.smartoverlay.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

val Context.settings by preferencesDataStore("byga_settings")
@Entity(tableName="records", primaryKeys=["kind","id"])
data class Record(val kind: String, val id: String, val body: String, val updatedAt: Long = System.currentTimeMillis())
@Dao interface Records {
    @Query("SELECT * FROM records WHERE kind = :kind ORDER BY updatedAt DESC") fun observe(kind: String): Flow<List<Record>>
    @Query("SELECT * FROM records WHERE kind = :kind AND id = :id") suspend fun get(kind: String, id: String): Record?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(value: Record)
    @Query("DELETE FROM records WHERE kind = :kind AND id = :id") suspend fun delete(kind: String, id: String)
    @Query("DELETE FROM records WHERE kind IN ('diagnostic', 'execution', 'prompt')") suspend fun clearHistory()
}
@Database(entities=[Record::class], version=1, exportSchema=true)
abstract class BygaDatabase : RoomDatabase() {
    abstract fun records(): Records
    companion object {
        @Volatile private var instance: BygaDatabase? = null
        fun get(context: Context): BygaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BygaDatabase::class.java, "byga.db").build().also { instance = it }
        }
    }
}
class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("encrypted_provider_keys", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("byga.providers.v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("byga.providers.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(id: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()); updateAAD(id.toByteArray()) }
        val payload = cipher.iv + cipher.doFinal(value.toByteArray())
        check(prefs.edit().putString(id, Base64.encodeToString(payload, Base64.NO_WRAP)).commit())
    }
    fun read(id: String): String? = prefs.getString(id, null)?.let {
        val bytes = Base64.decode(it, Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12))); updateAAD(id.toByteArray()) }
        String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)))
    }
    fun delete(id: String) { prefs.edit().remove(id).apply() }
}
