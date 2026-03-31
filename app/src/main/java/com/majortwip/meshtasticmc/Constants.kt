package com.majortwip.meshtasticmc

/**
 * Application-wide constants for Meshtastic intents and UDP multicast configuration.
 */
object Constants {

    // ── Meshtastic ──────────────────────────────────────────────────────────

    /** Package name of the Meshtastic Android app. */
    const val MESHTASTIC_PACKAGE = "com.geeksville.mesh"

    /**
     * Broadcast action sent by Meshtastic for every packet received from the radio.
     * Extra [EXTRA_FROMRADIO_BYTES] contains the raw serialised `FromRadio` protobuf bytes.
     *
     * This is the primary broadcast used for forwarding: it fires for every packet regardless
     * of port number, and carries the full `FromRadio` wrapper that contains metadata plus the
     * decoded inner `MeshPacket`.
     */
    const val ACTION_RECEIVED_FROMRADIO = "$MESHTASTIC_PACKAGE.RECEIVED_FROMRADIO"

    /**
     * Broadcast action prefix for decoded data packets (per port-number), e.g.:
     *   "com.geeksville.mesh.RECEIVED.1"  → TEXT_MESSAGE_APP
     * The extra for these broadcasts is a `DataPacket` Parcelable, *not* raw bytes, so they
     * are not directly used by this app – [ACTION_RECEIVED_FROMRADIO] is preferred.
     * Kept as a constant for documentation purposes.
     */
    const val ACTION_RECEIVED_DATA_PREFIX = "$MESHTASTIC_PACKAGE.RECEIVED."

    /**
     * Intent extra key carrying raw `FromRadio` protobuf bytes.
     * Used with [ACTION_RECEIVED_FROMRADIO].
     */
    const val EXTRA_FROMRADIO_BYTES = "payload"

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
