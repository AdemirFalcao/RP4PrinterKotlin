package com.honeywell.rp4printer.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.honeywell.rp4printer.bluetooth.BluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Gerenciador de impressão para RP4 usando arquivo PRN do BarTender
 * 
 * VERSÃO 6.0: Envia arquivo PRN DIRETO (sem processar)!
 * O BarTender já gera os comandos corretos para a RP4!
 */
class PrinterManager(
    private val bluetoothManager: BluetoothManager,
    private val context: Context
) {

    companion object {
        private const val TAG = "PrinterManager"
        private const val MAX_IMAGE_WIDTH = 576
    }

    /**
     * V10.0 - DPL com GW (Graphics Write) - Método CORRETO!
     * Usando comando GW que escreve gráfico diretamente
     */
    suspend fun printSignatureDPL(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Log.d(TAG, "=== DPL V10 GW: Início ===")
            
            // Redimensiona
            val targetWidth = 576
            val ratio = targetWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * ratio).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            
            Log.d(TAG, "Assinatura: ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // Converte para hex DPL
            val widthBytes = (scaledBitmap.width + 7) / 8
            val hexData = convertBitmapToHexDPL(scaledBitmap)
            
            Log.d(TAG, "Dados hex: ${hexData.length} chars")
            
            // Monta comando DPL usando GW (Graphics Write)
            val dplCommand = buildString {
                append("\u0002n\r\n")                    // Reset
                append("\u0002M1500\r\n")                // Altura da etiqueta
                append("\u0002KcLW0405;\r\n")            // Calor
                append("\u0002O0220\r\n")                // Offset
                append("\u0002D11\r\n")                  // Darkness
                append("\u0002A2\r\n")                   // Acceleration
                
                // Texto de exemplo (opcional)
                append("\u0002A100,50,0,4,1,1,N,\"ASSINATURA:\"\r\n")
                
                // GW = Graphics Write (x, y, widthBytes, height, dados)
                append("\u0002GW100,100,$widthBytes,${scaledBitmap.height},$hexData\r\n")
                
                append("Q0001\r\n")                      // Quantidade (sem STX)
                append("E\r\n")                          // Execute (sem STX)
            }
            
            Log.d(TAG, "Comando DPL: ${dplCommand.length} bytes")
            
            // Envia
            bluetoothManager.send(dplCommand.toByteArray(Charsets.ISO_8859_1))
            Thread.sleep(1500)
            
            Log.d(TAG, "=== DPL V10: Enviado ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro DPL GW", e)
            Result.failure(e)
        }
    }

    /**
     * Converte Bitmap para string hexadecimal para comando GW
     */
    private fun convertBitmapToHexDPL(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val hexData = StringBuilder()
        
        var blackPixels = 0
        
        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byte = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        if (gray < 128) {  // Preto
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                hexData.append(String.format("%02X", byte))
            }
        }
        
        Log.d(TAG, "GW: $blackPixels pixels pretos em ${width}x${height}")
        
        return hexData.toString()
    }

    /**
     * Converte Bitmap para formato DPL
     * DPL: [widthBytes_L][widthBytes_H][height_L][height_H][pixels]
     */
    private fun convertBitmapToDPL(bitmap: Bitmap, widthBytes: Int, height: Int): ByteArray {
        val output = ByteArrayOutputStream()
        
        // Header DPL: width e height em little-endian
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        output.write(height and 0xFF)
        output.write((height shr 8) and 0xFF)
        
        // Pixels
        val width = bitmap.width
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var blackPixels = 0
        
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
                        
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                output.write(byte)
            }
        }
        
        Log.d(TAG, "DPL: $blackPixels pixels pretos, ${width}x${height}")
        
        return output.toByteArray()
    }

    /**
     * TESTE: Imprime o arquivo exemplo.prn DIRETO do BarTender
     * Use este método primeiro para validar que o arquivo PRN funciona!
     */
    suspend fun printPrnExample(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Log.d(TAG, "=== TESTE PRN: Lendo exemplo.prn ===")
            
            // Lê o arquivo PRN dos assets
            val prnData = context.assets.open("exemplo.prn").use { inputStream ->
                inputStream.readBytes()
            }
            
            Log.d(TAG, "Arquivo PRN: ${prnData.size} bytes")
            Log.d(TAG, "Primeiros bytes: ${prnData.take(50).joinToString(" ") { "%02X".format(it) }}")
            
            // Envia DIRETO para a impressora (sem processar!)
            bluetoothManager.send(prnData)
            
            Log.d(TAG, "=== TESTE PRN: Enviado ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar PRN", e)
            Result.failure(e)
        }
    }

    /**
     * Imprime assinatura usando o FORMATO DO BARTENDER!
     * Converte a assinatura para Intermec compressed e injeta no PRN
     */
    suspend fun printSignatureFromPrnTemplate(
        bitmap: Bitmap,
        title: String = "ASSINATURA DIGITAL"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Log.d(TAG, "=== PRN TEMPLATE: Início ===")
            Log.d(TAG, "Bitmap: ${bitmap.width}x${bitmap.height}")
            
            // Redimensiona para o mesmo tamanho do exemplo (576x width max)
            val scaledBitmap = if (bitmap.width > MAX_IMAGE_WIDTH) {
                val ratio = MAX_IMAGE_WIDTH.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_WIDTH, newHeight, true)
            } else {
                bitmap
            }

            Log.d(TAG, "Redimensionado: ${scaledBitmap.width}x${scaledBitmap.height}")

            // Converte para formato Intermec comprimido (mesmo do BarTender)
            val imageData = convertBitmapToIntermecCompressed(scaledBitmap)
            
            Log.d(TAG, "Dados comprimidos: ${imageData.size} bytes")

            // Monta o PRN usando o mesmo formato do BarTender
            val command = ByteArrayOutputStream()
            
            // 1. Prefixo STX (0x02) antes de cada comando
            fun writeCommand(cmd: String) {
                command.write(0x02)
                command.write(cmd.toByteArray())
            }
            
            // 2. Reset e configuração (IGUAL ao exemplo.prn)
            writeCommand("n\r\n")
            writeCommand("M1500\r\n")
            writeCommand("KcLW0405;\r\n")
            writeCommand("O0220\r\n")
            writeCommand("d\r\n")
            
            // 3. IMAGE CREATE Raster (mesmo nome: gfx0)
            writeCommand("ICRgfx0\r")
            
            // 4. Dados da imagem compactada
            command.write(imageData)
            
            // 5. Line feed e configurações
            command.write(0x0D)  // CR
            command.write(0x0A)  // LF
            writeCommand("L\r\n")
            writeCommand("D11\r\n")
            writeCommand("A2\r\n")
            
            // 6. Imprimir imagem na mesma posição (X=286, Y=72)
            command.write("1Y1100002860072gfx0\r\n".toByteArray())
            
            // 7. Quantidade e execução
            command.write("Q0001\r\n".toByteArray())
            command.write("E\r\n".toByteArray())
            
            // 8. Limpar gráfico
            writeCommand("xCGgfx0\r\n")
            writeCommand("zC\r\n")

            Log.d(TAG, "Comando total: ${command.size()} bytes")
            
            // Envia
            bluetoothManager.send(command.toByteArray())
            Thread.sleep(1000)
            
            Log.d(TAG, "=== PRN TEMPLATE: Enviado ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro PRN Template", e)
            Result.failure(e)
        }
    }

    /**
     * Converte Bitmap para formato Intermec/Honeywell comprimido
     * Usa run-length encoding como o BarTender faz
     */
    private fun convertBitmapToIntermecCompressed(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        
        val output = ByteArrayOutputStream()
        
        // Header: largura em little-endian (2 bytes)
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        
        // Height em little-endian (2 bytes)
        output.write(height and 0xFF)
        output.write((height shr 8) and 0xFF)
        
        // Converte pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var blackPixels = 0
        val rawData = ByteArrayOutputStream()
        
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
                        
                        // 1 = preto (threshold 128)
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                rawData.write(byte)
            }
        }
        
        // Aplica RLE (Run-Length Encoding) básico
        val compressed = applyRLE(rawData.toByteArray())
        output.write(compressed)
        
        Log.d(TAG, "Intermec: $blackPixels pixels pretos, ${compressed.size} bytes comprimidos")
        
        return output.toByteArray()
    }

    /**
     * Decodifica RLE do formato Intermec/Honeywell
     * Padrão: 0x01 0x00 COUNT BYTE = repete BYTE COUNT vezes
     */
    private fun decodeIntermecRLE(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        var i = 0
        
        while (i < data.size) {
            if (i + 3 < data.size && data[i].toInt() and 0xFF == 0x01 && data[i+1].toInt() and 0xFF == 0x00) {
                // RLE: 01 00 COUNT BYTE
                val count = data[i+2].toInt() and 0xFF
                val byte = data[i+3]
                repeat(count) { output.write(byte.toInt()) }
                i += 4
            } else {
                output.write(data[i].toInt() and 0xFF)
                i++
            }
        }
        
        return output.toByteArray()
    }

    /**
     * Codifica dados em RLE formato Intermec/Honeywell
     * Se 3+ bytes iguais consecutivos: 0x01 0x00 COUNT BYTE
     */
    private fun encodeIntermecRLE(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        var i = 0
        
        while (i < data.size) {
            val currentByte = data[i]
            var count = 1
            
            // Conta bytes iguais consecutivos
            while (i + count < data.size && data[i + count] == currentByte && count < 255) {
                count++
            }
            
            // Se 3 ou mais, usa RLE
            if (count >= 3) {
                output.write(0x01)
                output.write(0x00)
                output.write(count)
                output.write(currentByte.toInt() and 0xFF)
                i += count
            } else {
                // Escreve direto
                output.write(currentByte.toInt() and 0xFF)
                i++
            }
        }
        
        return output.toByteArray()
    }

    /**
     * Converte Bitmap para pixels RAW (sem header, sem compressão)
     * Retorna apenas os bytes dos pixels prontos para RLE
     */
    private fun convertBitmapToRawPixels(bitmap: Bitmap, widthBytes: Int, height: Int): ByteArray {
        val width = bitmap.width
        val output = ByteArrayOutputStream()
        
        // Converte pixels
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
                        
                        // 1 = preto (threshold 128)
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                output.write(byte)
            }
        }
        
        Log.d(TAG, "Pixels convertidos: $blackPixels pretos em ${width}x${height}")
        
        return output.toByteArray()
    }

    /**
     * Converte Bitmap para formato Intermec RAW (SEM compressão)
     * Formato mais compatível e simples
     */
    private fun convertBitmapToIntermecRaw(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        
        val output = ByteArrayOutputStream()
        
        // Header: largura em little-endian (2 bytes)
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        
        // Height em little-endian (2 bytes)
        output.write(height and 0xFF)
        output.write((height shr 8) and 0xFF)
        
        // Converte pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var blackPixels = 0
        
        // Processa linha por linha - RAW (sem compressão)
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
                        
                        // 1 = preto (threshold 128)
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                output.write(byte)
            }
        }
        
        Log.d(TAG, "Intermec RAW: $blackPixels pixels pretos, ${width}x${height}, ${output.size()} bytes")
        
        return output.toByteArray()
    }

    /**
     * Aplica Run-Length Encoding (RLE) ao estilo Intermec
     */
    private fun applyRLE(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        var i = 0
        
        while (i < data.size) {
            val currentByte = data[i]
            var count = 1
            
            // Conta bytes iguais consecutivos
            while (i + count < data.size && 
                   data[i + count] == currentByte && 
                   count < 255) {
                count++
            }
            
            // Se tiver 3 ou mais repetições, comprime
            if (count >= 3) {
                // Formato: 0x01 0x00 COUNT BYTE
                output.write(0x01)
                output.write(0x00)
                output.write(count)
                output.write(currentByte.toInt())
                i += count
            } else {
                // Escreve direto
                output.write(currentByte.toInt())
                i++
            }
        }
        
        return output.toByteArray()
    }

    /**
     * VERSÃO 8.1: Substitui a imagem DIRETO no template exemplo.prn
     * Copia o bloco de dados COMPLETO sem tentar interpretá-lo
     */
    suspend fun printSignatureUsingTemplate(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Log.d(TAG, "=== TEMPLATE V8.1: Início ===")
            
            // 1. Lê o template original
            val templateData = context.assets.open("exemplo.prn").use { it.readBytes() }
            
            Log.d(TAG, "Template original: ${templateData.size} bytes")
            
            // 2. Localiza os marcadores EXATOS
            // "ICRgfx0" marca o início da seção de imagem
            val icrPattern = "ICRgfx0".toByteArray()
            var icrPos = -1
            for (i in 0 until templateData.size - icrPattern.size) {
                var match = true
                for (j in icrPattern.indices) {
                    if (templateData[i + j] != icrPattern[j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    icrPos = i
                    break
                }
            }
            
            if (icrPos == -1) {
                return@withContext Result.failure(Exception("ICRgfx0 não encontrado"))
            }
            
            Log.d(TAG, "ICRgfx0 encontrado na posição: $icrPos")
            
            // Dados da imagem começam após "ICRgfx0\r" (7 bytes + 1 CR)
            val imageDataStart = icrPos + 8  // ICRgfx0 (7) + CR (1)
            
            // Procura o próximo comando: \r\n\x02L
            val footerPattern = byteArrayOf(0x0D, 0x0A, 0x02, 0x4C)  // \r\n\x02L
            var footerPos = -1
            for (i in imageDataStart until templateData.size - footerPattern.size) {
                var match = true
                for (j in footerPattern.indices) {
                    if (templateData[i + j] != footerPattern[j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    footerPos = i
                    break
                }
            }
            
            if (footerPos == -1) {
                return@withContext Result.failure(Exception("Footer não encontrado"))
            }
            
            Log.d(TAG, "Footer encontrado na posição: $footerPos")
            
            // 3. Separa as seções
            val header = templateData.copyOfRange(0, imageDataStart)
            val originalImageData = templateData.copyOfRange(imageDataStart, footerPos)
            val footer = templateData.copyOfRange(footerPos, templateData.size)
            
            Log.d(TAG, "Cabeçalho: ${header.size} bytes")
            Log.d(TAG, "Imagem original: ${originalImageData.size} bytes")
            Log.d(TAG, "Rodapé: ${footer.size} bytes")
            
            // 4. V9.0: DPL PURO - Finalmente identificado corretamente!
            
            Log.d(TAG, ">>> V8.5: Substitui imagem mantendo estrutura exata <<<")
            
            // Decodifica os dados originais
            val decodedOriginal = decodeIntermecRLE(originalImageData.copyOfRange(4, originalImageData.size))
            Log.d(TAG, "Dados originais decodificados: ${decodedOriginal.size} bytes")
            
            // Extrai dimensões do header
            val originalHeader = originalImageData.copyOfRange(0, 4)
            val widthBytes = (originalHeader[0].toInt() and 0xFF) or ((originalHeader[1].toInt() and 0xFF) shl 8)
            val heightValue = (originalHeader[2].toInt() and 0xFF) or ((originalHeader[3].toInt() and 0xFF) shl 8)
            
            Log.d(TAG, "Header: ${originalHeader.joinToString(" ") { "%02X".format(it) }}")
            Log.d(TAG, "WidthBytes: $widthBytes, HeightValue: $heightValue")
            Log.d(TAG, "Tamanho esperado: ${widthBytes * heightValue} bytes")
            
            // IMPORTANTE: Verifica se há um offset/header adicional nos dados decodificados
            // Se decodedOriginal.size > widthBytes * heightValue, há dados extras no início
            val dataOffset = decodedOriginal.size - (widthBytes * heightValue)
            Log.d(TAG, "Offset detectado: $dataOffset bytes")
            
            // Prepara dados da assinatura
            val targetWidth = widthBytes * 8
            val targetHeight = heightValue
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            
            Log.d(TAG, "Assinatura: ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // Converte assinatura para pixels RAW
            val signaturePixels = convertBitmapToRawPixels(scaledBitmap, widthBytes, targetHeight)
            Log.d(TAG, "Pixels assinatura: ${signaturePixels.size} bytes")
            
            // Se há offset, copia o offset original + novos pixels
            val finalPixelData = if (dataOffset > 0) {
                Log.d(TAG, "Copiando offset de $dataOffset bytes do original")
                val combined = ByteArrayOutputStream()
                combined.write(decodedOriginal.copyOfRange(0, dataOffset))  // Offset original
                combined.write(signaturePixels)  // Novos pixels
                combined.toByteArray()
            } else {
                signaturePixels
            }
            
            Log.d(TAG, "Dados finais: ${finalPixelData.size} bytes")
            
            // Aplica RLE
            val compressedFinal = encodeIntermecRLE(finalPixelData)
            Log.d(TAG, "Comprimido: ${compressedFinal.size} bytes (original: ${originalImageData.size - 4})")
            
            // Monta imagem: HEADER ORIGINAL + dados comprimidos
            val newImageData = ByteArrayOutputStream()
            newImageData.write(originalHeader)
            newImageData.write(compressedFinal)
            
            // 5. Monta PRN final
            val finalPrn = ByteArrayOutputStream()
            finalPrn.write(header)
            finalPrn.write(newImageData.toByteArray())
            finalPrn.write(footer)
            
            Log.d(TAG, "PRN final: ${finalPrn.size()} bytes (original: ${templateData.size})")
            
            // Envia
            bluetoothManager.send(finalPrn.toByteArray())
            Thread.sleep(1500)
            
            Log.d(TAG, "=== TEMPLATE V8.5: Enviado com estrutura exata ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na substituição do template", e)
            Result.failure(e)
        }
    }

    /**
     * Imprime assinatura usando formato Intermec/Honeywell EXATO!
     */
    suspend fun printSignature(
        bitmap: Bitmap,
        signatureName: String = "ASSINATURA"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Log.d(TAG, "=== FINGERPRINT: Início ===")
            Log.d(TAG, "Bitmap: ${bitmap.width}x${bitmap.height}")
            
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
            
            Log.d(TAG, "Redimensionado: ${width}x${height}")

            // Converte para formato da impressora
            val imageData = convertBitmapToFingerprint(scaledBitmap)
            
            Log.d(TAG, "Dados: ${imageData.size} bytes")

            // MONTA COMANDO EXATAMENTE COMO O EXEMPLO.PRN
            val command = ByteArrayOutputStream()
            
            // 1. Reset e configuração
            command.write("n\r\n".toByteArray())
            command.write("M1500\r\n".toByteArray())
            command.write("KcLW0405;\r\n".toByteArray())
            command.write("O0220\r\n".toByteArray())
            command.write("d\r\n".toByteArray())
            
            // 2. IMAGE CREATE Raster
            command.write("ICRsig0\r\n".toByteArray())
            
            // 3. Dados da imagem
            command.write(imageData)
            
            // 4. Line feed e configurações
            command.write("L\r\n".toByteArray())
            command.write("D11\r\n".toByteArray())
            command.write("A2\r\n".toByteArray())
            
            // 5. Texto do título
            command.write("H10\r\n".toByteArray())  // Posição X
            command.write("V50\r\n".toByteArray())  // Posição Y
            command.write("FT000\r\n".toByteArray()) // Font
            command.write("D11\r\n".toByteArray())
            command.write("$signatureName\r\n".toByteArray())
            
            // 6. Imprimir imagem
            command.write("1Y1100002860072sig0\r\n".toByteArray())
            
            // 7. Quantidade e execução
            command.write("Q0001\r\n".toByteArray())
            command.write("E\r\n".toByteArray())
            
            // 8. Limpar gráfico
            command.write("xCGsig0\r\n".toByteArray())
            command.write("zC\r\n".toByteArray())

            Log.d(TAG, "Comando: ${command.size()} bytes total")
            
            // Envia
            bluetoothManager.send(command.toByteArray())
            Thread.sleep(1000)
            
            Log.d(TAG, "=== FINGERPRINT: Enviado ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro Fingerprint", e)
            Result.failure(e)
        }
    }

    /**
     * Converte Bitmap para formato Intermec/Honeywell compactado
     */
    private fun convertBitmapToFingerprint(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        
        val output = ByteArrayOutputStream()
        
        // Header: 2 bytes de largura (little endian)
        output.write(widthBytes and 0xFF)
        output.write((widthBytes shr 8) and 0xFF)
        
        // Converte pixels
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
                        
                        // 1 = preto
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                            blackPixels++
                        }
                    }
                }
                output.write(byte)
            }
        }
        
        Log.d(TAG, "Fingerprint: $blackPixels pixels pretos")
        
        return output.toByteArray()
    }

    /**
     * Imprime texto
     */
    suspend fun printText(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val command = buildString {
                append("n\r\n")
                append("M1500\r\n")
                append("H10\r\n")
                append("V100\r\n")
                append("FT000\r\n")
                append("D11\r\n")
                append("$text\r\n")
                append("Q0001\r\n")
                append("E\r\n")
            }

            bluetoothManager.send(command.toByteArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Teste simples
     */
    suspend fun printTest(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!bluetoothManager.isConnected()) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val command = buildString {
                append("n\r\n")
                append("M1500\r\n")
                append("H10\r\n")
                append("V50\r\n")
                append("FT000\r\n")
                append("D11\r\n")
                append("TESTE FINGERPRINT\r\n")
                append("H10\r\n")
                append("V150\r\n")
                append("Honeywell RP4\r\n")
                append("Q0001\r\n")
                append("E\r\n")
            }

            bluetoothManager.send(command.toByteArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imprime recibo com assinatura
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

            val command = ByteArrayOutputStream()
            
            // Configuração inicial
            command.write("n\r\n".toByteArray())
            command.write("M1500\r\n".toByteArray())
            
            var yPos = 50
            
            // Título
            command.write("H10\r\n".toByteArray())
            command.write("V$yPos\r\n".toByteArray())
            command.write("FT000\r\n".toByteArray())
            command.write("D11\r\n".toByteArray())
            command.write("$title\r\n".toByteArray())
            yPos += 100
            
            // Itens
            items.forEach { item ->
                command.write("H10\r\n".toByteArray())
                command.write("V$yPos\r\n".toByteArray())
                command.write("$item\r\n".toByteArray())
                yPos += 70
            }
            
            // Assinatura se existir
            signature?.let {
                val scaled = if (it.width > MAX_IMAGE_WIDTH) {
                    val ratio = MAX_IMAGE_WIDTH.toFloat() / it.width
                    Bitmap.createScaledBitmap(it, MAX_IMAGE_WIDTH, (it.height * ratio).toInt(), true)
                } else {
                    it
                }
                
                val imageData = convertBitmapToFingerprint(scaled)
                
                command.write("d\r\n".toByteArray())
                command.write("ICRsig0\r\n".toByteArray())
                command.write(imageData)
                command.write("L\r\n".toByteArray())
                command.write("1Y110000${yPos + 50}0072sig0\r\n".toByteArray())
                command.write("xCGsig0\r\n".toByteArray())
            }
            
            // Finaliza
            command.write("Q0001\r\n".toByteArray())
            command.write("E\r\n".toByteArray())

            bluetoothManager.send(command.toByteArray())
            Thread.sleep(1000)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
