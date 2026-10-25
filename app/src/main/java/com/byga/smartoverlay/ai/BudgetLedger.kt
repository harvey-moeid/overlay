package com.byga.smartoverlay.ai
import androidx.room.withTransaction
import com.byga.smartoverlay.data.*
object BudgetLedger {
    suspend fun reserve(db:BygaDatabase,day:String,limit:Int) {
        db.withTransaction{
            val used=db.records().get("ai_request_count",day)?.body?.toIntOrNull()?:0
            check(RequestBudget.mayReserve(used,limit)){"Batas request harian tercapai. Ubah batas di AI Chat bila diperlukan."}
            db.records().save(Record("ai_request_count",day,(used+1).toString()))
        }
    }
}
