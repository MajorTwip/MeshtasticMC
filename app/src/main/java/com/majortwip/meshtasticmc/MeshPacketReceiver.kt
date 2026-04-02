package com.majortwip.meshtasticmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.meshtastic.core.api.MeshtasticIntent
import org.meshtastic.core.model.DataPacket
import org.meshtastic.proto.Position

/**
 * [BroadcastReceiver] that listens for [MeshtasticIntent.ACTION_RECEIVED_POSITION_APP] broadcasts,
 * extracts [MeshtasticIntent.EXTRA_PAYLOAD] as a [DataPacket], decodes the protobuf payload as
 * [Position], and forwards the raw protobuf bytes through [onPacketReceived].
 */
class MeshPacketReceiver(
    private val onPacketReceived: (bytes: ByteArray) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action != MeshtasticIntent.ACTION_RECEIVED_POSITION_APP) return

        val packet = intent.extractDataPacket()
        if (packet == null) {
            Log.w(TAG, "Missing EXTRA_PAYLOAD DataPacket on position_app intent")
            return
        }

        val payload = packet.bytes?.toByteArray()
        if (payload == null || payload.isEmpty()) {
            Log.w(TAG, "Position DataPacket had empty payload bytes")
            return
        }

        val position = runCatching { Position.ADAPTER.decode(payload) }
            .onFailure { Log.e(TAG, "Failed to decode Position protobuf", it) }
            .getOrNull()

        if (position != null) {
            Log.i(
                TAG,
                "Decoded position: from=${packet.from}, to=${packet.to}, id=${packet.id}, " +
                    "latI=${position.latitudeI}, lonI=${position.longitudeI}, alt=${position.altitude}, " +
                    "time=${position.time}, sats=${position.satsInView}",
            )
        }

        onPacketReceived(payload)
    }

    private fun Intent.extractDataPacket(): DataPacket? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(MeshtasticIntent.EXTRA_PAYLOAD, DataPacket::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(MeshtasticIntent.EXTRA_PAYLOAD)
        }

    companion object {
        private const val TAG = "MeshPacketReceiver"
    }
}
