package com.honeywell.rp4printer.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.honeywell.rp4printer.bluetooth.BluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Gerenciador de impressão para RP4 usando Ez-Print Language
 * 
 * VERSÃO 4.0: Usa linguagem proprietária Ez-Print da Honeywell
 * Esta é a linguagem NATIVA da RP4!
 */
class PrinterManager(private val bluetoothManager: BluetoothManager) {

    companion object {
        private const val TAG = "PrinterManager"
        
        // Configurações da RP4
        private const val MAX_IMAGE_WIDTH = 576  // RP4: 832 dots, usar 70%
    }

    /**
     * Imprime texto usando Ez-Print
     */
    suspend fun printText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado à impressora"))
            }

            // Ez-Print: TEXT comando
            val command = buildString {
                append("! 0 200 200 0 1\r\n")  // Setup
                append("TEXT 4 0 0 0 $text\r\n")
                append("FORM\r\n")
                append("PRINT\r\n")
            }

            bluetoothManager.send(command.toByteArray(Charsets.UTF_8))
            
            Log.d(TAG, "Texto impresso com Ez-Print")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao imprimir texto", e)
            Result.failure(e)
        }
    }

    /**
     * Imprime assinatura usando Ez-Print IMAGE
     * MÉTODO NATIVO DA HONEYWELL!
     */
    suspend fun printSignature(
        bitmap: Bitmap,
        signatureName: String = "ASSINATURA DIGITAL"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado à impressora"))
            }

            Log.d(TAG, "=== EZ-PRINT: INÍCIO IMPRESSÃO ===")
            Log.d(TAG, "Bitmap original: ${bitmap.width}x${bitmap.height}")
            
            // Redimensiona
            val scaledBitmap = if (bitmap.width > MAX_IMAGE_WIDTH) {
                val ratio = MAX_IMAGE_WIDTH.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_WIDTH, newHeight, true)
            } else {
                bitmap
            }

            val width = scaledBitmap.width
            val height = scaledBitmap.height
            
            Log.d(TAG, "Bitmap redimensionado: ${width}x${height}")

            // Converte para PCX ou HEX format
            val imageData = convertBitmapToEzPrintFormat(scaledBitmap)
            
            Log.d(TAG, "Imagem convertida: ${imageData.size} bytes")

            // Monta comando Ez-Print IMAGE
            val command = buildString {
                // Setup da impressora
                append("! 0 200 200 0 1\r\n")
                
                // Título
                append("TEXT 4 0 50 20 $signatureName\r\n")
                append("LINE 50 100 ${width+50} 100 2\r\n")
                
                // Comando IMAGE
                // Sintaxe: IMAGE x y width height imageData
                append("IMAGE 50 120 $width $height ")
                
                // Dados da imagem em hexadecimal
                imageData.forEach { byte ->
                    append(String.format("%02X", byte))
                }
                append("\r\n")
                
                // Finaliza
                append("FORM\r\n")
                append("PRINT\r\n")
            }

            Log.d(TAG, "Comando Ez-Print: ${command.length} chars")
            
            // Envia comando
            bluetoothManager.send(command.toByteArray(Charsets.UTF_8))
            Thread.sleep(500)  // Aguarda processamento
            
            Log.d(TAG, "=== EZ-PRINT: IMPRESSÃO ENVIADA ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro Ez-Print", e)
            Result.failure(e)
        }
    }

    /**
     * Converte Bitmap para formato Ez-Print (monocromático compactado)
     */
    private fun convertBitmapToEzPrintFormat(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        
        val output = ByteArrayOutputStream()
        
        // Converte para monocromático
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var blackPixels = 0
        
        // Processa linha por linha
        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var byte = 0
                for (bit in 0 until 8) {
                    val pixelX = x * 8 + bit
                    if (pixelX < width) {
                        val pixel = pixels[y * width + pixelX]
                        val gray = (Color.red(pixel) * 0.299 + 
                                   Color.green(pixel) * 0.587 + 
                                   Color.blue(pixel) * 0.114).toInt()
                        
                        // 1 = preto, 0 = branco (normal)
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                output.write(byte)
            }
        }
        
        Log.d(TAG, "Ez-Print: $blackPixels pixels pretos de ${width*height}")
        
        return output.toByteArray()
    }

    /**
     * Imprime recibo com assinatura usando Ez-Print
     */
    suspend fun printReceipt(
        title: String,
        items: List<String>,
        signature: Bitmap? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val command = buildString {
                // Setup
                append("! 0 200 200 0 1\r\n")
                
                var yPos = 20
                
                // Título
                append("SETBOLD 1\r\n")
                append("TEXT 7 0 100 $yPos $title\r\n")
                append("SETBOLD 0\r\n")
                yPos += 80
                
                // Linha
                append("LINE 50 $yPos 750 $yPos 2\r\n")
                yPos += 20
                
                // Itens
                items.forEach { item ->
                    append("TEXT 4 0 50 $yPos $item\r\n")
                    yPos += 50
                }
                
                // Linha
                yPos += 10
                append("LINE 50 $yPos 750 $yPos 2\r\n")
                yPos += 30
                
                // Assinatura
                signature?.let {
                    val width = if (it.width > MAX_IMAGE_WIDTH) MAX_IMAGE_WIDTH else it.width
                    val ratio = width.toFloat() / it.width
                    val height = (it.height * ratio).toInt()
                    
                    val scaled = Bitmap.createScaledBitmap(it, width, height, true)
                    val imageData = convertBitmapToEzPrintFormat(scaled)
                    
                    append("TEXT 4 0 50 $yPos ASSINATURA\r\n")
                    yPos += 50
                    
                    append("IMAGE 50 $yPos $width $height ")
                    imageData.forEach { byte ->
                        append(String.format("%02X", byte))
                    }
                    append("\r\n")
                }
                
                // Finaliza
                append("FORM\r\n")
                append("PRINT\r\n")
            }

            bluetoothManager.send(command.toByteArray(Charsets.UTF_8))
            Thread.sleep(500)
            
            Log.d(TAG, "Recibo Ez-Print enviado")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no recibo Ez-Print", e)
            Result.failure(e)
        }
    }

    /**
     * Teste de impressão Ez-Print
     */
    suspend fun printTest(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val command = buildString {
                append("! 0 200 200 0 1\r\n")
                append("SETBOLD 1\r\n")
                append("TEXT 7 0 100 50 TESTE EZ-PRINT\r\n")
                append("SETBOLD 0\r\n")
                append("TEXT 4 0 100 150 Honeywell RP4\r\n")
                append("TEXT 4 0 100 200 Linguagem Nativa\r\n")
                append("LINE 100 250 700 250 2\r\n")
                append("FORM\r\n")
                append("PRINT\r\n")
            }

            bluetoothManager.send(command.toByteArray(Charsets.UTF_8))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
