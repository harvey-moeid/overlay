package com.byga.smartoverlay

import com.byga.smartoverlay.accessibility.ScanBudget
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ScanBudgetTest {
    @Test fun expensiveTraversalStopsAtDeadline() {
        var now=0L
        val budget=ScanBudget(Job(),{now})
        now=1999;budget.check();now=2000
        try { budget.check();fail() } catch(e:IllegalStateException) { assertTrue(e.message!!.contains("Pemindaian")) }
    }
    @Test fun cancelledScanCannotContinueToClick() {
        val job=Job();val budget=ScanBudget(job,{0L});job.cancel()
        try { budget.check();fail() } catch(_:CancellationException) { }
    }
}
