package com.honeywell.rp4printer.printer

import android.content.Context
import android.graphics.*
import android.util.Log
import honeywell.connection.ConnectionBase
import honeywell.connection.Connection_Bluetooth
import honeywell.printer.DocumentDPL
import honeywell.printer.ParametersDPL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SDK Oficial Honeywell com LOGS DETALHADOS
 */
class HoneywellSDKPrinter(private val context: Context) {
    
    companion object {
        private const val TAG = "HoneywellSDK"
        private const val RP4_USABLE_WIDTH = 743
    }
    
    private var connection: ConnectionBase? = null
    
    suspend fun connect(macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Conectando ===")
            
            connection = Connection_Bluetooth.createClient(macAddress, false)
            if (!connection!!.isOpen) {
                connection!!.open()
            }
            
            Log.d(TAG, "✅ Conectado!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao conectar", e)
            Result.failure(e)
        }
    }
    
    private fun trimBitmap(bitmap: Bitmap): Bitmap {
        var top = bitmap.height
        var left = bitmap.width
        var right = 0
        var bottom = 0
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel != -1 && pixel != 0xFFFFFFFF.toInt()) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        
        val margin = 5
        left = maxOf(0, left - margin)
        top = maxOf(0, top - margin)
        right = minOf(bitmap.width - 1, right + margin)
        bottom = minOf(bitmap.height - 1, bottom + margin)
        
        val width = right - left + 1
        val height = bottom - top + 1
        
        if (width <= 0 || height <= 0) return bitmap
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
    
    private fun darkenBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (r + g + b) / 3
                
                output.setPixel(x, y, if (luminance < 200) Color.BLACK else Color.WHITE)
            }
        }
        
        return output
    }
    
    private fun createRotatedLabel(signatureBitmap: Bitmap): Bitmap {
        val darkenedBitmap = darkenBitmap(signatureBitmap)
        val trimmedBitmap = trimBitmap(darkenedBitmap)
        
        val scaledWidth = trimmedBitmap.width / 3
        val scaledHeight = trimmedBitmap.height / 3
        val scaledSignature = Bitmap.createScaledBitmap(trimmedBitmap, scaledWidth, scaledHeight, true)
        
        val finalSignature = if (scaledSignature.width > RP4_USABLE_WIDTH - 20) {
            val ratio = (RP4_USABLE_WIDTH - 20).toFloat() / scaledSignature.width
            val newHeight = (scaledSignature.height * ratio).toInt()
            Bitmap.createScaledBitmap(scaledSignature, RP4_USABLE_WIDTH - 20, newHeight, true)
        } else {
            scaledSignature
        }
        
        val textHeight = 30
        val spacing = 10
        val lineHeight = 3
        val bottomMargin = 10
        
        val labelWidth = RP4_USABLE_WIDTH
        val labelHeight = textHeight + spacing + finalSignature.height + spacing + lineHeight + bottomMargin
        
        val labelBitmap = Bitmap.createBitmap(labelWidth, labelHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(labelBitmap)
        canvas.drawColor(Color.WHITE)
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("ASSINATURA:", 10f, 22f, textPaint)
        
        val xPos = (labelWidth - finalSignature.width) / 2
        val yPos = textHeight + spacing
        canvas.drawBitmap(finalSignature, xPos.toFloat(), yPos.toFloat(), null)
        
        val lineY = textHeight + spacing + finalSignature.height + spacing
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(10f, lineY.toFloat(), (labelWidth - 10).toFloat(), lineY.toFloat(), linePaint)
        
        val matrix = Matrix()
        matrix.postRotate(180f)
        val rotatedLabel = Bitmap.createBitmap(labelBitmap, 0, 0, labelBitmap.width, labelBitmap.height, matrix, true)
        
        Log.d(TAG, "📐 Label: ${rotatedLabel.width}x${rotatedLabel.height}px")
        
        return rotatedLabel
    }
    
    /**
     * Helper para logar dados em HEX
     */
    private fun logData(label: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        val ascii = data.map { b ->
            val c = b.toInt() and 0xFF
            when {
                c == 0x02 -> "<STX>"
                c == 0x0D -> "<CR>"
                c == 0x0A -> "<LF>"
                c in 32..126 -> c.toChar().toString()
                else -> String.format("\\x%02X", c)
            }
        }.joinToString("")
        
        Log.d(TAG, "────────────────────────────────────────")
        Log.d(TAG, "$label:")
        Log.d(TAG, "Tamanho: ${data.size} bytes")
        Log.d(TAG, "HEX: $hex")
        Log.d(TAG, "ASCII: $ascii")
        Log.d(TAG, "────────────────────────────────────────")
    }
    
    suspend fun printSignature(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (connection == null) {
                return@withContext Result.failure(Exception("Não conectado"))
            }
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "=== 🖨️ IMPRIMINDO ===")
            Log.d(TAG, "========================================")
            
            val rotatedLabel = createRotatedLabel(bitmap)
            
            // 1. COMANDO o0 (altura automática)
            val o0Command = "\u0002o0\r\n".toByteArray()
            logData("📤 COMANDO 1: o0 (altura automática)", o0Command)
            connection!!.write(o0Command)
            Thread.sleep(100)
            
            // 2. SDK
            val docDPL = DocumentDPL()
            val paramDPL = ParametersDPL()
            docDPL.writeImage(rotatedLabel, 0, 0, paramDPL)
            val sdkData = docDPL.getDocumentData()
            
            // Loga os primeiros 200 bytes do SDK (onde ficam os comandos)
            val sdkHeader = sdkData.take(200).toByteArray()
            logData("📤 COMANDO 2: SDK (primeiros 200 bytes)", sdkHeader)
            
            Log.d(TAG, "📤 Enviando dados completos do SDK (${sdkData.size} bytes)...")
            connection!!.write(sdkData)
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ ENVIADO!")
            Log.d(TAG, "========================================")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro", e)
            Result.failure(e)
        }
    }
    
    fun disconnect() {
        try {
            connection?.close()
            connection = null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desconectar", e)
        }
    }
    
    fun isConnected(): Boolean = connection != null
}
