package com.example.learningremote

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learningremote.adapter.SignalAdapter
import com.example.learningremote.databinding.ActivityMainBinding
import com.example.learningremote.model.RemoteSignal
import com.example.learningremote.network.TcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SignalAdapter
    private var allSignals: MutableList<RemoteSignal> = mutableListOf()
    private var currentTab: TabType = TabType.IR

    enum class TabType { IR, RF, DECODE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabs()
        setupConnection()
        setupTcpCallbacks()
    }

    private fun setupRecyclerView() {
        adapter = SignalAdapter(
            signals = mutableListOf(),
            onTransmit = { signal -> transmitSignal(signal) },
            onDelete = { signal -> deleteSignal(signal) }
        )
        binding.recyclerViewSignals.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSignals.adapter = adapter
    }

    private fun setupTabs() {
        updateTabUI(TabType.IR)

        binding.tabIR.setOnClickListener { switchTab(TabType.IR) }
        binding.tabRF.setOnClickListener { switchTab(TabType.RF) }
        binding.tabDecode.setOnClickListener { switchTab(TabType.DECODE) }
    }

    private fun switchTab(tab: TabType) {
        currentTab = tab
        updateTabUI(tab)

        when (tab) {
            TabType.IR, TabType.RF -> {
                binding.recyclerViewSignals.visibility = android.view.View.VISIBLE
                binding.textViewDecodeResult.visibility = android.view.View.GONE
                filterSignalsByType(tab == TabType.IR ? "ir" : "rf")
            }
            TabType.DECODE -> {
                binding.recyclerViewSignals.visibility = android.view.View.GONE
                binding.textViewDecodeResult.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun updateTabUI(tab: TabType) {
        val irActive = tab == TabType.IR
        val rfActive = tab == TabType.RF
        val decodeActive = tab == TabType.DECODE

        binding.tabIR.isSelected = irActive
        binding.tabRF.isSelected = rfActive
        binding.tabDecode.isSelected = decodeActive

        binding.tabIR.setBackgroundColor(if (irActive) 0xFF2196F3.toInt() else android.graphics.Color.TRANSPARENT)
        binding.tabRF.setBackgroundColor(if (rfActive) 0xFF2196F3.toInt() else android.graphics.Color.TRANSPARENT)
        binding.tabDecode.setBackgroundColor(if (decodeActive) 0xFF2196F3.toInt() else android.graphics.Color.TRANSPARENT)
    }

    private fun filterSignalsByType(type: String) {
        val filtered = allSignals.filter { it.type == type }
        adapter.updateData(filtered.toMutableList())
    }

    private fun setupConnection() {
        binding.buttonConnect.setOnClickListener {
            val ip = binding.editTextIp.text.toString().trim()
            if (ip.isNotEmpty()) {
                connectToESP32(ip)
            } else {
                Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToESP32(ip: String) {
        binding.buttonConnect.isEnabled = false
        binding.textViewStatus.text = "Status: Connecting..."

        lifecycleScope.launch(Dispatchers.IO) {
            val success = TcpClient.connect(ip)

            withContext(Dispatchers.Main) {
                if (success) {
                    binding.textViewStatus.text = "Status: Connected to $ip"
                    binding.textViewStatus.setBackgroundColor(0xFFC8E6C9.toInt())
                    loadSignals()
                } else {
                    binding.textViewStatus.text = "Status: Connection failed"
                    binding.textViewStatus.setBackgroundColor(0xFFFFCDD2.toInt())
                    Toast.makeText(this@MainActivity, "Failed to connect to $ip", Toast.LENGTH_LONG).show()
                }
                binding.buttonConnect.isEnabled = true
            }
        }
    }

    private fun setupTcpCallbacks() {
        TcpClient.onMessageReceived = { message ->
            runOnUiThread {
                handleServerMessage(message)
            }
        }

        TcpClient.onConnectionChanged = { connected ->
            runOnUiThread {
                if (!connected) {
                    binding.textViewStatus.text = "Status: Disconnected"
                    binding.textViewStatus.setBackgroundColor(0xFFFFCDD2.toInt())
                }
            }
        }
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            val cmd = json.optString("cmd", "")

            when (cmd) {
                "connected" -> {
                    appendLog("✓ Connected to server")
                }
                "decode_result" -> {
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        val type = data.optString("type", "unknown")
                        val protocol = data.optString("protocol", "RAW")
                        val dataArray = data.optJSONArray("data")
                        val pulseCount = dataArray?.length() ?: 0

                        val result = """📡 Signal Decoded!
                            |Type: ${type.uppercase()}
                            |Protocol: $protocol
                            |Pulses: $pulseCount
                            |
                            |Raw Data:
                            |${data.toString(2)}""".trimMargin()

                        binding.textViewDecodeResult.text = result
                        appendLog("📥 Decoded $type signal ($pulseCount pulses)")

                        showSaveDialog(data.toString())
                    }
                }
                else -> {
                    appendLog("📨 Server: $message")
                }
            }
        } catch (e: Exception) {
            appendLog("⚠ Parse error: ${e.message}")
        }
    }

    private fun showSaveDialog(signalData: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_signal, null)
        val editRemoteId = dialogView.findViewById<EditText>(R.id.editTextRemoteId)
        val editButtonName = dialogView.findViewById<EditText>(R.id.editTextButtonName)

        AlertDialog.Builder(this)
            .setTitle("Save Signal")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val remoteId = editRemoteId.text.toString().trim()
                val buttonName = editButtonName.text.toString().trim()

                if (remoteId.isNotEmpty() && buttonName.isNotEmpty()) {
                    saveSignal(currentTab == TabType.IR ? 0 : 1, remoteId, buttonName)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSignal(type: Int, remoteId: String, buttonName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val command = "SAVE_SIGNAL $type \"$remoteId\" \"$buttonName\""
            TcpClient.send(command)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "✓ Signal saved", Toast.LENGTH_SHORT).show()
                loadSignals()
            }
        }
    }

    private fun loadSignals() {
        lifecycleScope.launch(Dispatchers.IO) {
            TcpClient.send("GET_SIGNALS")
        }
    }

    private fun parseSignalsResponse(response: String) {
        try {
            val json = JSONObject(response)
            val signalsArray = json.optJSONArray("signals")

            allSignals.clear()
            if (signalsArray != null) {
                for (i in 0 until signalsArray.length()) {
                    val sigObj = signalsArray.getJSONObject(i)
                    val signal = RemoteSignal(
                        id = sigObj.getInt("id"),
                        type = sigObj.getString("type"),
                        remoteId = sigObj.getString("remote_id"),
                        buttonName = sigObj.getString("button"),
                        dataLen = sigObj.getInt("data_len")
                    )
                    allSignals.add(signal)
                }
            }

            filterSignalsByType(if (currentTab == TabType.IR) "ir" else "rf")
            appendLog("📋 Loaded ${allSignals.size} signals")
        } catch (e: Exception) {
            appendLog("⚠ Error parsing signals: ${e.message}")
        }
    }

    private fun transmitSignal(signal: RemoteSignal) {
        if (!TcpClient.isConnected()) {
            Toast.makeText(this, "Not connected to ESP32", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            TcpClient.send("TRANSMIT ${signal.id}")

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "▶ Transmitting: ${signal.buttonName}", Toast.LENGTH_SHORT).show()
                appendLog("▶ Sent: ${signal.remoteId} - ${signal.buttonName}")
            }
        }
    }

    private fun deleteSignal(signal: RemoteSignal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Signal")
            .setMessage("Delete \"${signal.buttonName}\" from \"${signal.remoteId}\"?")
            .setPositiveButton("Delete") { _, _ ->
                if (TcpClient.isConnected()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        TcpClient.send("DELETE ${signal.id}")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "🗑 Signal deleted", Toast.LENGTH_SHORT).show()
                            loadSignals()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val currentLog = binding.textViewLog.text.toString()
        val newLog = "[$timestamp] $message\n$currentLog"
        binding.textViewLog.text = newLog
    }

    override fun onDestroy() {
        super.onDestroy()
        TcpClient.disconnect()
    }
}