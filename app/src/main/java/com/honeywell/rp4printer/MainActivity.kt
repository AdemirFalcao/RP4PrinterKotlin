package com.honeywell.rp4printer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.honeywell.rp4printer.bluetooth.BluetoothManager
import com.honeywell.rp4printer.printer.PrinterManager
import com.honeywell.rp4printer.signature.SignatureView
import kotlinx.coroutines.launch

/**
 * Activity principal do aplicativo RP4 Printer.
 * 
 * VERSÃO BLUETOOTH DIRETO - Funciona em QUALQUER dispositivo Android!
 * NÃO requer Honeywell Print Service.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var printerManager: PrinterManager
    private var signatureView: SignatureView? = null
    
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
        
        // Inicializa os gerenciadores
        bluetoothManager = BluetoothManager(this)
        printerManager = PrinterManager(bluetoothManager)  // Usa BluetoothManager diretamente
        
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
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, "Bluetooth desabilitado", Toast.LENGTH_SHORT).show()
            return
        }

        val devices = bluetoothManager.getPairedDevices()
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
     * Conecta à impressora via Bluetooth direto
     */
    private fun connectToPrinter(device: BluetoothDevice) {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Conectando...", Toast.LENGTH_SHORT).show()
            
            // Conecta via Bluetooth direto (sem SDK)
            val result = bluetoothManager.connect(device)
            
            if (result.isSuccess) {
                Toast.makeText(
                    this@MainActivity, 
                    "✓ Conectado à ${device.name}!", 
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
     * Imprime a assinatura capturada
     */
    private fun printSignature() {
        val signature = signatureView?.getSignatureBitmap()
        
        if (signature == null || signatureView?.isEmpty() == true) {
            Toast.makeText(this, "Assine primeiro na área branca!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "Conecte à impressora primeiro!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Imprimindo...", Toast.LENGTH_SHORT).show()
            
            val result = printerManager.printSignature(signature, "ASSINATURA DIGITAL")
            
            if (result.isSuccess) {
                Toast.makeText(
                    this@MainActivity, 
                    "✓ Assinatura impressa com sucesso!", 
                    Toast.LENGTH_LONG
                ).show()
                // Opcionalmente, limpar a assinatura após imprimir
                // signatureView?.clear()
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
        // Desconecta do Bluetooth ao sair
        bluetoothManager.disconnect()
    }
}

