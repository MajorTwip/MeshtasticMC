package com.majortwip.meshtasticmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Log
import org.meshtastic.core.api.MeshtasticIntent

/**
 * [BroadcastReceiver] that listens for [MeshtasticIntent.ACTION_RECEIVED_POSITION_APP] broadcasts
 * and logs the raw payload bytes for diagnostic purposes.
 */
class MeshPacketReceiver(
    private val onPacketReceived: (bytes: ByteArray) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action != MeshtasticIntent.ACTION_RECEIVED_POSITION_APP) return

        // Dump raw intent parcel BEFORE any extras access (avoids deserialization NPE).
        val parcel = Parcel.obtain()
        try {
            intent.writeToParcel(parcel, 0)
            val raw = parcel.marshall()
            Log.d(TAG, "=== Raw intent parcel (${raw.size} bytes) ===")
            raw.toList().chunked(16).forEachIndexed { i, chunk ->
                Log.d(TAG, "%04x  %-48s  |%s|".format(
                    i * 16,
                    chunk.joinToString(" ") { "%02x".format(it) },
                    chunk.joinToString("") { b -> if (b.toInt() in 32..126) b.toInt().toChar().toString() else "." },
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Raw parcel dump failed: $e")
        } finally {
            parcel.recycle()
        }

        Log.d(TAG, "Bundle keys: ${intent.extras?.keySet()}")
    }

    companion object {
        private const val TAG = "MeshPacketReceiver"
    }
}
