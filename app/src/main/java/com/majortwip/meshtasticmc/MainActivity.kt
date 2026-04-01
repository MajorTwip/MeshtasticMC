package com.majortwip.meshtasticmc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.majortwip.meshtasticmc.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Entry-point activity.
 *
 * Shows a simple toggle button to start/stop [MeshtasticForwardService] and displays the
 * current multicast destination so the user knows where packets are sent.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        observeLog()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    // ── Log observer ──────────────────────────────────────────────────────────

    private fun observeLog() {
        lifecycleScope.launch {
            LogBuffer.entries.collectLatest { lines ->
                val text = if (lines.isEmpty()) {
                    getString(R.string.label_log_empty)
                } else {
                    lines.joinToString("\n")
                }
                binding.textLog.text = text
                // Auto-scroll to the bottom so the latest entry is always visible.
                binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupUi() {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val group = prefs.getString(Constants.PREF_MULTICAST_GROUP, Constants.DEFAULT_MULTICAST_GROUP)
        val port = prefs.getInt(Constants.PREF_MULTICAST_PORT, Constants.DEFAULT_MULTICAST_PORT)

        binding.textMulticastAddress.text = getString(R.string.label_multicast_address, group, port)

        binding.buttonToggleService.setOnClickListener {
            if (MeshtasticForwardService.isRunning) {
                stopForwardingService()
            } else {
                startForwardingService()
            }
        }

        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }

        // Pre-fill settings fields
        binding.editMulticastGroup.setText(group)
        binding.editMulticastPort.setText(port.toString())
    }

    private fun updateServiceStatus() {
        val running = MeshtasticForwardService.isRunning
        binding.buttonToggleService.text = getString(
            if (running) R.string.action_stop_forwarding else R.string.action_start_forwarding,
        )
        binding.statusIndicator.visibility = if (running) View.VISIBLE else View.INVISIBLE
    }

    // ── Service control ───────────────────────────────────────────────────────

    private fun startForwardingService() {
        if (!isMeshtasticInstalled()) {
            Toast.makeText(this, R.string.error_meshtastic_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        val intent = MeshtasticForwardService.startIntent(this)
        ContextCompat.startForegroundService(this, intent)
        updateServiceStatus()
    }

    private fun stopForwardingService() {
        startService(MeshtasticForwardService.stopIntent(this))
        updateServiceStatus()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private fun saveSettings() {
        val groupText = binding.editMulticastGroup.text.toString().trim()
        val portText = binding.editMulticastPort.text.toString().trim()
        val port = portText.toIntOrNull()

        if (groupText.isEmpty()) {
            binding.editMulticastGroup.error = getString(R.string.error_invalid_address)
            return
        }
        if (port == null || port !in 1..65535) {
            binding.editMulticastPort.error = getString(R.string.error_invalid_port)
            return
        }

        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(Constants.PREF_MULTICAST_GROUP, groupText)
            .putInt(Constants.PREF_MULTICAST_PORT, port)
            .apply()

        binding.textMulticastAddress.text = getString(R.string.label_multicast_address, groupText, port)
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isMeshtasticInstalled(): Boolean =
        try {
            packageManager.getPackageInfo(Constants.MESHTASTIC_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
