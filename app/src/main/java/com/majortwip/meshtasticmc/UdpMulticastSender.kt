package com.majortwip.meshtasticmc

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/**
 * Manages a UDP multicast socket and sends raw byte payloads to the configured group/port.
 *
 * Call [open] once before sending and [close] when done.
 * Designed to be extensible: the inverse direction (receiving UDP multicast and forwarding
 * to Meshtastic) can be added here later using [MulticastSocket.receive].
 */
class UdpMulticastSender(
    private val groupAddress: String = Constants.DEFAULT_MULTICAST_GROUP,
    private val port: Int = Constants.DEFAULT_MULTICAST_PORT,
    private val ttl: Int = Constants.DEFAULT_MULTICAST_TTL,
) {
    private var socket: MulticastSocket? = null
    private var group: InetAddress? = null

    /** Human-readable destination string for display in logs. */
    val destination: String get() = "$groupAddress:$port"

    /**
     * Opens the underlying [MulticastSocket].
     * Must be called from a background thread (performs I/O).
     */
    @Throws(Exception::class)
    fun open() {
        socket = MulticastSocket().also { it.timeToLive = ttl }
        group = InetAddress.getByName(groupAddress)
        Log.i(TAG, "UDP multicast socket opened → $groupAddress:$port (TTL=$ttl)")
    }

    /**
     * Sends [data] as a UDP multicast datagram.
     * Must be called from a background thread (performs I/O).
     */
    @Throws(Exception::class)
    fun send(data: ByteArray) {
        val sock = socket ?: throw IllegalStateException("Socket is not open")
        val grp = group ?: throw IllegalStateException("Group address not resolved")
        val datagram = DatagramPacket(data, data.size, grp, port)
        sock.send(datagram)
        Log.d(TAG, "Sent ${data.size} bytes to $groupAddress:$port")
    }

    /**
     * Convenience suspend extension that dispatches [send] on [Dispatchers.IO].
     */
    suspend fun sendAsync(data: ByteArray) = withContext(Dispatchers.IO) { send(data) }

    /**
     * Closes the underlying socket. Safe to call even if [open] was never called.
     */
    fun close() {
        socket?.close()
        socket = null
        Log.i(TAG, "UDP multicast socket closed")
    }

    companion object {
        private const val TAG = "UdpMulticastSender"
    }
}
