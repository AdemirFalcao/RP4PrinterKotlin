package com.honeywell.rp4printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.honeywell.rp4printer.printer.HoneywellSDKPrinter
import com.honeywell.rp4printer.signature.SignatureView
import kotlinx.coroutines.launch

/**
 * Activity principal - USA APENAS SDK OFICIAL DA HONEYWELL!
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sdkPrinter: HoneywellSDKPrinter
    private var signatureView: SignatureView? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            showPrinterSelection()
        } else {
            Toast.makeText(this, "Permissões necessárias negadas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // SDK OFICIAL APENAS!
        sdkPrinter = HoneywellSDKPrinter(this)
        
        // Bluetooth adapter nativo (só para listar dispositivos)
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as AndroidBluetoothManager
        bluetoothAdapter = btManager.adapter
        
        signatureView = findViewById(R.id.signatureView)
        
        findViewById<android.widget.Button>(R.id.btnConnect).setOnClickListener {
            checkPermissionsAndConnect()
        }
        
        findViewById<android.widget.Button>(R.id.btnClear).setOnClickListener {
            signatureView?.clear()
        }
        
        findViewById<android.widget.Button>(R.id.btnPrint).setOnClickListener {
            printSignature()
        }
    }

    private fun checkPermissionsAndConnect() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            showPrinterSelection()
        } else {
            requestPermissions.launch(permissions)
        }
    }

    private fun showPrinterSelection() {
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Bluetooth desabilitado", Toast.LENGTH_SHORT).show()
            return
        }

        @Suppress("MissingPermission")
        val devices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        
        if (devices.isEmpty()) {
            Toast.makeText(this, "Nenhuma impressora pareada", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = devices.map { "${it.name} (${it.address})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Selecione a Impressora RP4")
            .setItems(deviceNames) { _, which ->
                connectToPrinter(devices[which])
            }
            .show()
    }

    /**
     * Conecta à impressora usando SDK OFICIAL (ÚNICO)
     */
    private fun connectToPrinter(device: BluetoothDevice) {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Conectando com SDK Oficial...", Toast.LENGTH_SHORT).show()
            
            // Conecta usando SDK OFICIAL!
            val result = sdkPrinter.connect(device.address)
            
            if (result.isSuccess) {
                Toast.makeText(
                    this@MainActivity, 
                    "✓ Conectado com SDK Oficial!", 
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                Toast.makeText(
                    this@MainActivity, 
                    "✗ Erro ao conectar: $errorMsg", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Imprime a assinatura usando SDK OFICIAL da Honeywell!
     */
    private fun printSignature() {
        val signature = signatureView?.getSignatureBitmap()
        
        if (signature == null || signatureView?.isEmpty() == true) {
            Toast.makeText(this, "Assine primeiro na área branca!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!sdkPrinter.isConnected()) {
            Toast.makeText(this, "Conecte à impressora primeiro!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Imprimindo com SDK Oficial...", Toast.LENGTH_SHORT).show()
            
            // USA SDK OFICIAL DA HONEYWELL!
            val result = sdkPrinter.printSignature(signature)
            
            if (result.isSuccess) {
                Toast.makeText(
                    this@MainActivity, 
                    "✓ Assinatura impressa com SDK Oficial!", 
                    Toast.LENGTH_LONG
                ).show()
                signatureView?.clear()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                Toast.makeText(
                    this@MainActivity, 
                    "✗ Erro ao imprimir: $errorMsg", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sdkPrinter.disconnect()
    }
}
