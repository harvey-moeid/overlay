package com.byga.smartoverlay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment

internal const val ITEMS_PER_PAGE = 8
internal fun lastPage(count: Int) = ((count - 1).coerceAtLeast(0)) / ITEMS_PER_PAGE

/** Bound composition inside the workspace's scroll container; no nested scroll lists. */
@Composable fun <T> PagedItems(items: List<T>, resetKey: String = "", content: @Composable (T) -> Unit) {
    var requestedPage by rememberSaveable(resetKey) { mutableIntStateOf(0) }
    val page = requestedPage.coerceIn(0, lastPage(items.size))
    items.subList(page * ITEMS_PER_PAGE, minOf(items.size,(page+1)*ITEMS_PER_PAGE)).forEach { content(it) }
    if (items.size > ITEMS_PER_PAGE) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = page > 0, onClick = { requestedPage = page - 1 }) { Text("Sebelumnya") }
            Text("${page + 1} / ${lastPage(items.size) + 1}")
            TextButton(enabled = page < lastPage(items.size), onClick = { requestedPage = page + 1 }) { Text("Berikutnya") }
        }
    }
}
