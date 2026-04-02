package com.majortwip.meshtasticmc

/**
 * Application-wide constants for Meshtastic intents and UDP multicast configuration.
 */
object Constants {

    // ── Meshtastic ──────────────────────────────────────────────────────────

    /** Package name of the Meshtastic Android app. */
    const val MESHTASTIC_PACKAGE = "com.geeksville.mesh"

    /** Broadcast action sent by Meshtastic when a position packet is received. */
    const val ACTION_RECEIVED_POSITION_APP = "com.geeksville.mesh.RECEIVED.POSITION_APP"

    /** Intent extra key carrying the raw packet payload (a Parcelable DataPacket). */
    const val EXTRA_PAYLOAD = "com.geeksville.mesh.Payload"

    // ── UDP Multicast ────────────────────────────────────────────────────────

    /** Default multicast group address. */
    const val DEFAULT_MULTICAST_GROUP = "224.0.0.251"

    /** Default UDP multicast port. */
    const val DEFAULT_MULTICAST_PORT = 5005

    /** Time-to-live for outgoing multicast packets (LAN scope). */
    const val DEFAULT_MULTICAST_TTL = 5

    // ── Notification ─────────────────────────────────────────────────────────

    const val NOTIFICATION_CHANNEL_ID = "meshtasticmc_forward"
    const val NOTIFICATION_ID = 1

    // ── Service actions ───────────────────────────────────────────────────────

    /** Intent action sent to [MeshtasticForwardService] to stop it. */
    const val ACTION_STOP_SERVICE = "com.majortwip.meshtasticmc.STOP"

    // ── SharedPreferences ─────────────────────────────────────────────────────

    const val PREFS_NAME = "MeshtasticMCPrefs"
    const val PREF_MULTICAST_GROUP = "multicast_group"
    const val PREF_MULTICAST_PORT = "multicast_port"
}
