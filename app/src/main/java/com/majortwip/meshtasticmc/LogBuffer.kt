package com.majortwip.meshtasticmc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory log buffer shared between [MeshtasticForwardService] and [MainActivity].
 *
 * Keeps the last [MAX_LINES] timestamped log lines and exposes them as a [StateFlow] so the
 * Activity can observe changes without polling.  Thread-safe via [MutableStateFlow]'s atomic
 * value swap.
 */
object LogBuffer {

    private const val MAX_LINES = 200
    private val _entries = MutableStateFlow<List<String>>(emptyList())

    /** Snapshot of all retained log lines, newest last. */
    val entries: StateFlow<List<String>> = _entries

    /** Append [line] prefixed with the current wall-clock time. */
    fun append(line: String) {
        // Create a new instance per call: SimpleDateFormat is not thread-safe.
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val stamped = "[$ts] $line"
        val current = _entries.value
        _entries.value = if (current.size >= MAX_LINES) {
            current.drop(1) + stamped
        } else {
            current + stamped
        }
    }
}
