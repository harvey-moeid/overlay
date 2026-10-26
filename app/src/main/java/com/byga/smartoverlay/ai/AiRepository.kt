package com.byga.smartoverlay.ai

import android.content.Context
import androidx.room.withTransaction
import com.byga.smartoverlay.data.*
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

// Request limits count actual attempts, not tokens or a dollar price estimate.
object RequestBudget {
    fun mayReserve(used: Int, limit: Int) = limit in 1..100 && used in 0 until limit
}
data class Generation(val text: String, val providerId: String, val providerLabel: String)
class AiRepository(private val context: Context) {
    private val db=BygaDatabase.get(context)
    suspend fun reserve(limit: Int) {
        val day=LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
        BudgetLedger.reserve(db,day,limit)
    }

    suspend fun usedToday(): Int = db.records().get("ai_request_count",LocalDate.now(ZoneId.of("Asia/Jakarta")).toString())?.body?.toIntOrNull() ?: 0
    suspend fun generate(prompt: String, providerIds: List<String>, dailyLimit: Int=12): Generation {
        require(prompt.isNotBlank() && prompt.length<=32000) { "Prompt harus 1–32000 karakter" }
        require(providerIds.isNotEmpty()) { "Tambahkan dan pilih provider dahulu" }
        var last="Provider tidak tersedia"
        for(id in providerIds.distinct().take(3)) {
            val record=db.records().get("provider",id) ?: continue
            val config=runCatching { JSONObject(record.body) }.getOrNull() ?: continue
            val key=try { SecretStore(context).read(id) } catch(e:CancellationException){throw e} catch(_: Exception) { null }
            if(key.isNullOrBlank()) { last="Key provider tidak tersedia"; continue }
            reserve(dailyLimit)
            val type=ProviderType.valueOf(config.getString("type"))
            val result=try { HttpProvider(ProviderConfig(id,type,config.getString("url"),config.getString("model")),key).text(prompt) }
                catch(e: CancellationException) { throw e } catch(_: Exception) { AiResult.Failure("PROVIDER_UNAVAILABLE") }
            when(result) {
                is AiResult.Text -> return Generation(result.value,id,ProviderCatalog.get(type).label+" • "+config.getString("model"))
                is AiResult.Failure -> last=result.code
            }
        }
        error("Permintaan gagal: $last. Tidak ada hasil yang ditandai berhasil.")
    }
}
