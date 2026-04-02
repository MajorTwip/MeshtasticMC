package com.majortwip.meshtasticmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.meshtastic.core.model.DataPacket

/**
 * Receives POSITION_APP broadcasts from the Meshtastic app and decodes the Position protobuf.
 *
 * The [Constants.EXTRA_PAYLOAD] extra is a [DataPacket] Parcelable sent by the
 * Meshtastic app. Uses the real [org.meshtastic.core.model.DataPacket] from the
 * official library (v2.7.13), decoded via Wire protobuf.
 */
class MeshPacketReceiver(
    private val onPacketReceived: (bytes: ByteArray) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_RECEIVED_POSITION_APP) return

        val packet = intent.getParcelableExtra("com.geeksville.mesh.Payload", DataPacket::class.java) ?: return
        val protoBytes = packet.bytes ?: return

        decodePosition(protoBytes, packet.from)
        onPacketReceived(protoBytes)
    }

    // ── Protobuf decoder (Position message) ─────────────────────────────────

    private fun decodePosition(bytes: ByteArray, from: String?) {
        val pos = org.meshtastic.proto.Position.parseFrom(bytes)
        val lat = if (pos.hasLatitudeI())  "%.7f".format(pos.latitudeI  / 1e7) else "?"
        val lon = if (pos.hasLongitudeI()) "%.7f".format(pos.longitudeI / 1e7) else "?"
        val alt = if (pos.hasAltitude())   "${pos.altitude}m"                   else "?"
        Log.i(TAG, "Position from=$from  lat=$lat  lon=$lon  alt=$alt  time=${pos.time}  sats=${pos.satsInView}")
    }

    companion object {
        private const val TAG = "MeshPacketReceiver"
    }
}
