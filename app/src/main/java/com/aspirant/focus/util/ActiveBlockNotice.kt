package com.aspirant.focus.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockNotice(
    val title: String,
    val message: String,
    val reason: String, // "CURFEW", "ADULT_CONTENT", "INCOGNITO", "STRICT_KIOSK", "APP_BLOCKED"
    val timestamp: Long = System.currentTimeMillis()
)

object ActiveBlockNotice {
    private val _notice = MutableStateFlow<BlockNotice?>(null)
    val notice: StateFlow<BlockNotice?> = _notice.asStateFlow()

    fun show(title: String, message: String, reason: String) {
        _notice.value = BlockNotice(title, message, reason)
    }

    fun dismiss() {
        _notice.value = null
    }
}
