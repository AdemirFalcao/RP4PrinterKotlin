package com.honeywell.rp4printer.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        // UUID padrão para SPP (Serial Port Profile)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão Bluetooth negada", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Fecha conexão anterior se existir
            disconnect()

            Log.d(TAG, "Conectando a ${device.name} (${device.address})")

            // Cria socket RFCOMM
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Conecta
            bluetoothSocket?.connect()
            
            // Obtém o output stream
            outputStream = bluetoothSocket?.outputStream

            Log.d(TAG, "Conectado com sucesso!")
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao conectar", e)
            disconnect()
            Result.failure(e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão negada", e)
            Result.failure(e)
        }
    }

    suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            outputStream?.let {
                it.write(data)
                it.flush()
                Log.d(TAG, "Enviados ${data.size} bytes")
                Result.success(Unit)
            } ?: Result.failure(IOException("Não conectado"))
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao enviar dados", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
            Log.d(TAG, "Desconectado")
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao desconectar", e)
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}



