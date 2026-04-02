package org.meshtastic.core.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Stub Parcelable whose package + class name matches the real DataPacket sent by the
 * Meshtastic app. Android resolves Parcelables by class name, so [Bundle.getParcelable]
 * invokes our CREATOR — no Meshtastic SDK needed.
 *
 * Read order mirrors DataPacket.readFromParcel() in the official Meshtastic source:
 * https://github.com/meshtastic/Meshtastic-Android/blob/main/core/model/src/commonMain/kotlin/org/meshtastic/core/model/DataPacket.kt
 */
class DataPacket(val bytes: ByteArray?, val from: String?) : Parcelable {
    override fun writeToParcel(dest: Parcel, flags: Int) = Unit
    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<DataPacket> {
        override fun createFromParcel(p: Parcel): DataPacket {
            p.readString()                          // to
            val bytes = p.createByteArray()         // bytes  (via ByteStringParceler)
            p.readInt()                             // dataType
            val from = p.readString()               // from
            p.readLong()                            // time
            p.readInt()                             // id
            if (p.readInt() != 0) p.readString()   // status: presence flag + optional name string
            p.readInt()                             // hopLimit
            p.readInt()                             // channel
            p.readInt()                             // wantAck
            p.readInt()                             // hopStart
            p.readFloat()                           // snr
            p.readInt()                             // rssi
            if (p.readInt() != 0) p.readInt()      // replyId: presence flag + optional value
            if (p.readInt() != 0) p.readInt()      // relayNode: presence flag + optional value
            p.readInt()                             // relays
            p.readInt()                             // viaMqtt
            p.readInt()                             // emoji
            p.createByteArray()                     // sfppHash (via ByteStringParceler)
            p.readInt()                             // transportMechanism
            return DataPacket(bytes, from)
        }
        override fun newArray(size: Int) = arrayOfNulls<DataPacket>(size)
    }
}
