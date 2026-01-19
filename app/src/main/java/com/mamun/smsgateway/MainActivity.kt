package com.mamun.smsgateway

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mamun.smsgateway.adapter.ConnectedDevicesAdapter
import com.mamun.smsgateway.adapter.ConsoleLogAdapter
import com.mamun.smsgateway.databinding.ActivityMainBinding
import com.mamun.smsgateway.model.ConnectedDevice
import com.mamun.smsgateway.model.LogEntry
import com.mamun.smsgateway.server.SmsHttpServer
import com.mamun.smsgateway.server.SmsWebSocketServer
import com.mamun.smsgateway.util.NetworkUtils
import com.mamun.smsgateway.util.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceManager

    private lateinit var devicesAdapter: ConnectedDevicesAdapter
    private lateinit var logsAdapter: ConsoleLogAdapter

    private val logEntries = mutableListOf<LogEntry>()
    private val connectedDevices = mutableListOf<ConnectedDevice>()

    private var gatewayService: SmsGatewayService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SmsGatewayService.LocalBinder
            gatewayService = binder.getService()
            serviceBound = true
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            gatewayService = null
            serviceBound = false
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SmsWebSocketServer.ACTION_DEVICES_UPDATE -> {
                    updateConnectedDevices()
                }
                SmsHttpServer.ACTION_LOG_ENTRY -> {
                    val logEntry = LogEntry(
                        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis()),
                        method = intent.getStringExtra("method") ?: "",
                        path = intent.getStringExtra("path") ?: "",
                        clientIp = intent.getStringExtra("clientIp") ?: "",
                        statusCode = intent.getIntExtra("statusCode", 200)
                    )
                    addLogEntry(logEntry)
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            checkBatteryOptimization()
        } else {
            Toast.makeText(this, "Permissions required for SMS Gateway to work", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        setupRecyclerViews()
        setupListeners()
        requestPermissions()

        // Restore state
        binding.etWebSocketPort.setText(
            if (prefs.webSocketPort != PreferenceManager.DEFAULT_WS_PORT)
                prefs.webSocketPort.toString()
            else ""
        )
        binding.etHttpPort.setText(
            if (prefs.httpPort != PreferenceManager.DEFAULT_HTTP_PORT)
                prefs.httpPort.toString()
            else ""
        )
        binding.switchServer.isChecked = prefs.serverEnabled
    }

    override fun onStart() {
        super.onStart()
        bindService()
        registerReceivers()
        updateIpAddresses()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
        unregisterReceivers()
    }

    private fun setupRecyclerViews() {
        devicesAdapter = ConnectedDevicesAdapter()
        binding.rvConnectedDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = devicesAdapter
        }

        logsAdapter = ConsoleLogAdapter()
        binding.rvConsoleLogs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = logsAdapter
        }
    }

    private fun setupListeners() {
        binding.switchServer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startServer()
            } else {
                stopServer()
            }
        }

        binding.btnClearLogs.setOnClickListener {
            logEntries.clear()
            logsAdapter.submitList(emptyList())
        }
    }

    private fun startServer() {
        val wsPort = binding.etWebSocketPort.text.toString().toIntOrNull()
            ?: PreferenceManager.DEFAULT_WS_PORT
        val httpPort = binding.etHttpPort.text.toString().toIntOrNull()
            ?: PreferenceManager.DEFAULT_HTTP_PORT

        // Validate ports
        if (wsPort == httpPort) {
            Toast.makeText(this, "WebSocket and HTTP ports must be different", Toast.LENGTH_SHORT).show()
            binding.switchServer.isChecked = false
            return
        }

        if (wsPort !in 1024..65535 || httpPort !in 1024..65535) {
            Toast.makeText(this, "Ports must be between 1024 and 65535", Toast.LENGTH_SHORT).show()
            binding.switchServer.isChecked = false
            return
        }

        prefs.webSocketPort = wsPort
        prefs.httpPort = httpPort
        prefs.serverEnabled = true

        SmsGatewayService.start(this, wsPort, httpPort)

        binding.tvServerStatus.text = getString(R.string.server_running)
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.success))

        // Disable port fields when server is running
        binding.etWebSocketPort.isEnabled = false
        binding.etHttpPort.isEnabled = false

        updateIpAddresses()
    }

    private fun stopServer() {
        prefs.serverEnabled = false
        SmsGatewayService.stop(this)

        binding.tvServerStatus.text = getString(R.string.server_stopped)
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.error))

        // Enable port fields
        binding.etWebSocketPort.isEnabled = true
        binding.etHttpPort.isEnabled = true

        connectedDevices.clear()
        devicesAdapter.submitList(emptyList())
        updateDeviceCount()
    }

    private fun bindService() {
        val intent = Intent(this, SmsGatewayService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(SmsWebSocketServer.ACTION_DEVICES_UPDATE)
            addAction(SmsHttpServer.ACTION_LOG_ENTRY)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun updateUI() {
        gatewayService?.let { service ->
            if (service.isRunning) {
                binding.switchServer.isChecked = true
                binding.tvServerStatus.text = getString(R.string.server_running)
                binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
                binding.etWebSocketPort.isEnabled = false
                binding.etHttpPort.isEnabled = false

                updateConnectedDevices()
            }
        }
    }

    private fun updateConnectedDevices() {
        gatewayService?.getWebSocketServer()?.let { server ->
            val devices = server.getConnectedDevices()
            connectedDevices.clear()
            connectedDevices.addAll(devices)
            devicesAdapter.submitList(devices.toList())
            updateDeviceCount()

            binding.tvNoDevices.visibility = if (devices.isEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
            binding.rvConnectedDevices.visibility = if (devices.isNotEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun updateDeviceCount() {
        binding.tvDeviceCount.text = connectedDevices.size.toString()
    }

    private fun addLogEntry(logEntry: LogEntry) {
        logEntries.add(logEntry)
        if (logEntries.size > 500) {
            logEntries.removeAt(0)
        }
        logsAdapter.submitList(logEntries.toList())
        binding.rvConsoleLogs.scrollToPosition(logEntries.size - 1)
    }

    private fun updateIpAddresses() {
        val wsPort = binding.etWebSocketPort.text.toString().toIntOrNull()
            ?: PreferenceManager.DEFAULT_WS_PORT
        val httpPort = binding.etHttpPort.text.toString().toIntOrNull()
            ?: PreferenceManager.DEFAULT_HTTP_PORT

        binding.tvIpAddresses.text = NetworkUtils.formatIpAddressesDisplay(wsPort, httpPort)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkBatteryOptimization()
        }
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Battery Optimization")
                .setMessage("For the server to run reliably in the background, please disable battery optimization for this app.")
                .setPositiveButton("Settings") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }
}