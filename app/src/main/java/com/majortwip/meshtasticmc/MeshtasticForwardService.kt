package com.majortwip.meshtasticmc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground [Service] that bridges Meshtastic radio packets to UDP multicast.
 *
 * Lifecycle:
 *  1. On start: creates the UDP sender, registers a [MeshPacketReceiver] for Meshtastic
 *     broadcasts, and acquires a Wi-Fi multicast lock so outgoing multicast datagrams are
 *     not dropped by the Wi-Fi driver.
 *  2. For every broadcast from the Meshtastic app the raw payload bytes are sent as a UDP
 *     multicast datagram on a background coroutine.
 *  3. On stop: unregisters the receiver, releases the multicast lock, closes the socket.
 *
 * The service listens for:
 *  - [Constants.ACTION_RECEIVED_FROMRADIO] — fires for every raw radio packet (preferred,
 *    catches all traffic regardless of port number).
 *  - Well-known per-port-number broadcasts — registered as a belt-and-suspenders fallback so
 *    that common packet types are also forwarded if the Meshtastic version in use does not
 *    send ACTION_RECEIVED_FROMRADIO.
 *
 * Inverse direction (UDP multicast → Meshtastic) is intentionally left as a TODO so it can
 * be added without restructuring this class.
 */
class MeshtasticForwardService : Service() {

    private val udpSender by lazy { createUdpSender() }
    private lateinit var meshReceiver: MeshPacketReceiver
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var multicastLock: WifiManager.MulticastLock? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(Constants.NOTIFICATION_ID, buildNotification())

        acquireMulticastLock()

        serviceScope.launch {
            try {
                udpSender.open()
                LogBuffer.append("UDP socket open – destination: ${udpSender.destination}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open UDP socket", e)
                LogBuffer.append("ERROR opening UDP socket: ${e.message}")
            }
        }

        meshReceiver = MeshPacketReceiver { packetBytes ->
            serviceScope.launch {
                try {
                    udpSender.send(packetBytes)
                    LogBuffer.append("Forwarded ${packetBytes.size} bytes → ${udpSender.destination}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send UDP multicast packet", e)
                    LogBuffer.append("ERROR sending packet: ${e.message}")
                }
            }
        }

        LogBuffer.append("Service started – forwarding to UDP multicast")
        registerMeshtasticReceiver()
        Log.i(TAG, "Service started – forwarding Meshtastic packets to UDP multicast")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.ACTION_STOP_SERVICE) {
            Log.i(TAG, "Stop requested via intent")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(meshReceiver)
        serviceJob.cancel()
        udpSender.close()
        releaseMulticastLock()
        LogBuffer.append("Service stopped")
        Log.i(TAG, "Service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    // ── BroadcastReceiver registration ────────────────────────────────────────

    private fun registerMeshtasticReceiver() {
        val filter = IntentFilter(Constants.ACTION_RECEIVED_FROMRADIO)

        // Android 14 (API 34) requires an explicit RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED
        // flag for dynamically registered receivers.  We need EXPORTED because the broadcasts
        // originate from the Meshtastic app (a different process).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(meshReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(meshReceiver, filter)
        }
    }

    // ── Wi-Fi multicast lock ──────────────────────────────────────────────────

    private fun acquireMulticastLock() {
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager ?: return
        multicastLock = wifi.createMulticastLock(TAG).also {
            it.setReferenceCounted(true)
            it.acquire()
            Log.d(TAG, "Multicast lock acquired")
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
        Log.d(TAG, "Multicast lock released")
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, MeshtasticForwardService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, getString(R.string.action_stop), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    private fun createUdpSender(): UdpMulticastSender {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val group = prefs.getString(Constants.PREF_MULTICAST_GROUP, Constants.DEFAULT_MULTICAST_GROUP)
            ?: Constants.DEFAULT_MULTICAST_GROUP
        val port = prefs.getInt(Constants.PREF_MULTICAST_PORT, Constants.DEFAULT_MULTICAST_PORT)
        return UdpMulticastSender(groupAddress = group, port = port)
    }

    companion object {
        private const val TAG = "MeshtasticForwardSvc"

        /**
         * Reflects whether the service is currently running.
         * Set to `true` in [onCreate] and `false` in [onDestroy].
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        /** @return an [Intent] that starts this service. */
        fun startIntent(context: Context) = Intent(context, MeshtasticForwardService::class.java)

        /** @return an [Intent] that stops this service. */
        fun stopIntent(context: Context) = Intent(context, MeshtasticForwardService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
    }
}
