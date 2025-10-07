package com.honeywell.rp4printer.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import honeywell.connection.ConnectionBase
import honeywell.connection.Connection_Bluetooth
import honeywell.printer.DocumentLP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Impressão com DocumentLP (Line Mode)
 * FUNCIONA COM CORTE DE PAPEL! ✅
 */
class HoneywellSDKPrinter(private val context: Context) {
    
    companion object {
        private const val TAG = "HoneywellSDK"
        private const val PAPER_WIDTH_DOTS_MM = 203 // 203 dpi
        private const val PAPER_WIDTH_MM = 103 // Largura do papel em mm
        private const val BORDER_MM = 5 // 5mm de borda
        private const val USABLE_WIDTH_MM = PAPER_WIDTH_MM - (2 * BORDER_MM) // 93mm
        private const val USABLE_WIDTH_DOTS = (USABLE_WIDTH_MM * PAPER_WIDTH_DOTS_MM / 25.4).toInt() // 743 dots
    }
    
    private var connection: ConnectionBase? = null
    
    suspend fun connect(macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Conectando (DocumentLP) ===")
            Log.d(TAG, "MAC: $macAddress")
            
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
    
    private fun trimBitmap(bitmap: Bitmap, margin: Int = 5): Bitmap {
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
        
        left = maxOf(0, left - margin)
        top = maxOf(0, top - margin)
        right = minOf(bitmap.width - 1, right + margin)
        bottom = minOf(bitmap.height - 1, bottom + margin)
        
        val width = right - left + 1
        val height = bottom - top + 1
        
        if (width <= 0 || height <= 0) return bitmap
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
    
    private fun darkenBitmap(bitmap: Bitmap, threshold: Int = 200): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (r + g + b) / 3
                
                output.setPixel(x, y, if (luminance < threshold) Color.BLACK else Color.WHITE)
            }
        }
        
        return output
    }
    
    /**
     * Imprime assinatura usando DocumentLP (Line Mode)
     * IGUAL ao exemplo DO-Print que FUNCIONA com corte de papel!
     */
    suspend fun printSignature(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (connection == null || !connection!!.isOpen) {
                return@withContext Result.failure(Exception("Não conectado"))
            }
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "=== 🖨️ DocumentLP (Line Mode) ===")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Bitmap original: ${bitmap.width}x${bitmap.height}")
            
            // 1. Escurece a imagem
            val darkenedBitmap = darkenBitmap(bitmap, 200)
            Log.d(TAG, "✅ Imagem escurecida (threshold 200)")
            
            // 2. Recorta espaços vazios
            val trimmedBitmap = trimBitmap(darkenedBitmap, 5)
            Log.d(TAG, "✅ Bitmap recortado: ${trimmedBitmap.width}x${trimmedBitmap.height}")
            
            // 3. Reduz para 1/3 do tamanho
            val scaledBitmap = Bitmap.createScaledBitmap(
                trimmedBitmap,
                trimmedBitmap.width / 3,
                trimmedBitmap.height / 3,
                true
            )
            Log.d(TAG, "✅ Bitmap reduzido (1/3): ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // 4. Ajusta largura se exceder a largura útil
            val finalSignatureBitmap = if (scaledBitmap.width > USABLE_WIDTH_DOTS) {
                val ratio = USABLE_WIDTH_DOTS.toFloat() / scaledBitmap.width
                Bitmap.createScaledBitmap(
                    scaledBitmap,
                    USABLE_WIDTH_DOTS,
                    (scaledBitmap.height * ratio).toInt(),
                    true
                )
            } else {
                scaledBitmap
            }
            Log.d(TAG, "✅ Assinatura final: ${finalSignatureBitmap.width}x${finalSignatureBitmap.height}")
            
            // 5. Cria label completo (texto + assinatura + linha)
            val textHeight = 30
            val lineThickness = 3
            val verticalSpacing = 10
            val finalMargin = 10
            
            val totalContentHeight = textHeight + verticalSpacing + finalSignatureBitmap.height + verticalSpacing + lineThickness + finalMargin
            val labelBitmap = Bitmap.createBitmap(USABLE_WIDTH_DOTS, totalContentHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(labelBitmap)
            canvas.drawColor(Color.WHITE)
            
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            
            // Desenha texto "ASSINATURA:"
            val text = "ASSINATURA:"
            val textX = (USABLE_WIDTH_DOTS - paint.measureText(text)) / 2
            canvas.drawText(text, textX, textHeight.toFloat() - 5, paint)
            
            // Desenha assinatura (centralizada)
            val signatureX = (USABLE_WIDTH_DOTS - finalSignatureBitmap.width) / 2
            val signatureY = textHeight + verticalSpacing
            canvas.drawBitmap(finalSignatureBitmap, signatureX.toFloat(), signatureY.toFloat(), null)
            
            // Desenha linha divisória
            val lineY = signatureY + finalSignatureBitmap.height + verticalSpacing
            canvas.drawRect(0f, lineY.toFloat(), USABLE_WIDTH_DOTS.toFloat(), (lineY + lineThickness).toFloat(), paint)
            
            Log.d(TAG, "📐 Label completo: ${labelBitmap.width}x${labelBitmap.height}px")
            
            // 6. Rotaciona 180° (RP4 imprime de cabeça para baixo)
            val matrix = Matrix()
            matrix.postRotate(180f)
            val rotatedLabel = Bitmap.createBitmap(labelBitmap, 0, 0,
                labelBitmap.width, labelBitmap.height, matrix, true)
            Log.d(TAG, "✅ Label rotacionado 180°: ${rotatedLabel.width}x${rotatedLabel.height}px")
            
            // =================================================================================
            // SOLUÇÃO FINAL: DocumentLP (Line Mode) - IGUAL AO EXEMPLO QUE FUNCIONA!
            // =================================================================================
            
            val docLP = DocumentLP("!")
            
            Log.d(TAG, "────────────────────────────────────────")
            Log.d(TAG, "📤 Usando DocumentLP (Line Mode)")
            Log.d(TAG, "   Largura da impressora: $USABLE_WIDTH_DOTS dots")
            
            // Método EXATO do exemplo DO-Print (linha 938)
            docLP.writeImage(rotatedLabel, USABLE_WIDTH_DOTS)
            
            val printData = docLP.getDocumentData()
            
            Log.d(TAG, "   Dados gerados: ${printData.size} bytes")
            Log.d(TAG, "   Primeiros 100 bytes HEX:")
            Log.d(TAG, "   ${printData.take(100).joinToString(" ") { "%02X".format(it) }}")
            Log.d(TAG, "────────────────────────────────────────")
            
            connection!!.write(printData)
            Thread.sleep(2000)
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ ENVIADO COM DocumentLP!")
            Log.d(TAG, "========================================")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao imprimir", e)
            Result.failure(e)
        }
    }
    
    fun disconnect() {
        try {
            connection?.close()
            connection = null
            Log.d(TAG, "Desconectado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desconectar", e)
        }
    }
    
    fun isConnected(): Boolean = connection?.isOpen == true
}