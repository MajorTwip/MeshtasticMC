package com.majortwip.meshtasticmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * [BroadcastReceiver] that listens for [Constants.ACTION_RECEIVED_FROMRADIO] broadcasts from the
 * Meshtastic app and invokes [onPacketReceived] with the raw serialised `FromRadio` protobuf bytes.
 *
 * The Meshtastic app also sends per-port broadcasts (`com.geeksville.mesh.RECEIVED.<portNum>`)
 * containing a `DataPacket` Parcelable.  Those cannot be trivially serialised to bytes without
 * depending on the Meshtastic SDK, so only the `RECEIVED_FROMRADIO` broadcast is handled here.
 * That broadcast fires for *every* radio packet, so it covers all port numbers.
 *
 * The receiver is registered dynamically inside [MeshtasticForwardService] so it only lives
 * while the service is running.
 *
 * @param onPacketReceived Called on the main thread with the raw [ByteArray] payload whenever a
 *   packet arrives.  Implementations should dispatch heavy work (e.g. UDP send) off the main thread.
 */
class MeshPacketReceiver(
    private val onPacketReceived: (bytes: ByteArray) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_RECEIVED_FROMRADIO) return

        val bytes = intent.getByteArrayExtra(Constants.EXTRA_FROMRADIO_BYTES)
        if (bytes == null) {
            Log.w(TAG, "RECEIVED_FROMRADIO intent missing '${Constants.EXTRA_FROMRADIO_BYTES}' extra – ignoring")
            return
        }

        Log.d(TAG, "FromRadio packet received (${bytes.size} bytes)")
        onPacketReceived(bytes)
    }

    companion object {
        private const val TAG = "MeshPacketReceiver"
    }
}
