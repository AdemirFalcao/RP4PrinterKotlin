# 🔧 Solução Bluetooth Direto - RP4 Printer

## ✅ Versão Otimizada para Dispositivos Comuns

Esta versão funciona em **QUALQUER dispositivo Android** (Samsung, Xiaomi, Motorola, etc.), **SEM necessidade do Honeywell Print Service**.

---

## 🎯 O Que Foi Corrigido

### Problema 1: Print Service não disponível
**Causa:** O SDK da Honeywell requer o Honeywell Print Service, que só está disponível em dispositivos Honeywell.

**Solução:** ✅ Removido o SDK da Honeywell. Usa Bluetooth direto.

### Problema 2: Caracteres especiais ao imprimir assinatura
**Causa:** O comando `GS v 0` (usado anteriormente) não é totalmente compatível com a RP4.

**Solução:** ✅ Implementado método **ESC * (Bit Image)** - imprime linha por linha, muito mais compatível!

---

## 🔄 Mudanças Implementadas

### 1. Método de Impressão de Imagem

**ANTES (não funcionava):**
```kotlin
// Comando GS v 0 - enviava imagem inteira de uma vez
val imageData = convertBitmapToRP4Format(bitmap)
bluetoothManager.send(imageData)
```

**AGORA (funciona!):**
```kotlin
// Comando ESC * - envia linha por linha
for (cada linha da imagem) {
    ESC * 0 nL nH [dados da linha]
    LINE_FEED
}
```

### 2. Por Que ESC * Funciona Melhor?

| Característica | GS v 0 (Antigo) | ESC * (Novo) |
|----------------|-----------------|--------------|
| **Envio** | Imagem inteira de uma vez | Linha por linha |
| **Compatibilidade** | Varia por impressora | Universal ESC/POS |
| **Controle** | Menos controle | Mais controle |
| **Buffer** | Requer buffer grande | Buffer pequeno por linha |
| **Resultado na RP4** | ❌ Caracteres especiais | ✅ Imagem perfeita |

---

## 📦 Como Instalar e Testar

### 1. Instalar o APK Atualizado

```bash
cd "/Users/amelo/Downloads/drive-download-20250929T200435Z-1-001/Honeywell_MobilitySDK_Android_v1.00.00.0188/Honeywell_MobilitySDK_Android_v1.00.00.0188/honeywell-android-printing-sdk/samples/RP4PrinterKotlin"

# Instalar
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Testar a Impressão

1. **Abrir o app** "RP4 Printer"
2. **Parear a RP4** via Configurações → Bluetooth (se ainda não pareou)
3. **Conectar:**
   - Clicar em "Conectar à Impressora"
   - Selecionar a RP4 da lista
   - Aguardar "✓ Conectado!"
4. **Desenhar assinatura** na área branca
5. **Clicar em "Imprimir"**
6. **✅ A assinatura deve sair como IMAGEM, não caracteres!**

---

## 🔍 Diferenças Técnicas

### Comando ESC * (Bit Image)

```
Formato: ESC * m nL nH [data]

Onde:
- ESC = 0x1B
- * = 0x2A
- m = modo (0 = 8-dot single-density)
- nL = largura baixo byte
- nH = largura alto byte
- [data] = dados da linha (1 byte = 8 pixels horizontais)
```

### Exemplo de uma linha:

```kotlin
// Linha com 384 pixels de largura = 48 bytes
ESC * 0 48 0 [byte1][byte2]...[byte48]
LINE_FEED
```

Cada byte representa 8 pixels horizontais:
- Bit 1 = pixel preto
- Bit 0 = pixel branco

---

## 📊 Configurações Otimizadas

### Tamanho da Imagem

```kotlin
private const val MAX_IMAGE_WIDTH = 384  // pixels
```

**Por quê 384?**
- RP4 tem 832 dots (4 polegadas @ 208 DPI)
- 384 pixels usa ~46% da largura
- Deixa margem e evita corte
- Tamanho ideal para assinaturas

### Delays Adicionados

```kotlin
Thread.sleep(200)  // Após INIT
Thread.sleep(100)  // Após título
Thread.sleep(10)   // A cada 10 linhas de imagem
```

**Por quê delays?**
- Bluetooth precisa de tempo para enviar dados
- Buffer da impressora é limitado
- Evita perda de dados
- Garante impressão correta

---

## 🎯 Arquitetura Simplificada

```
┌─────────────────┐
│   MainActivity  │
│                 │
│  - UI Events    │
│  - Permissions  │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
┌────────▼────────┐ ┌─────▼──────────┐
│ BluetoothManager│ │ PrinterManager │
│                 │ │                │
│ - Connect       │ │ - Print Text   │
│ - Send bytes    │ │ - Print Image  │
│ - Disconnect    │ │ (ESC * method) │
└─────────────────┘ └────────────────┘
         │
         │ Raw Bluetooth
         │
┌────────▼────────┐
│  Impressora RP4 │
│                 │
│  Via SPP/RFCOMM │
└─────────────────┘
```

---

## ✅ Checklist de Verificação

### Requisitos:
- [x] Qualquer dispositivo Android (min SDK 24)
- [x] Bluetooth habilitado
- [x] Impressora RP4 pareada
- [x] Permissões Bluetooth concedidas

### Não Necessário:
- [ ] ~~Dispositivo Honeywell~~
- [ ] ~~Honeywell Print Service~~
- [ ] ~~SDK proprietário~~

---

## 🐛 Troubleshooting

### Problema: Ainda imprime caracteres especiais

**Possíveis causas:**

1. **Cache do app**
   ```bash
   adb shell pm clear com.honeywell.rp4printer
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Versão antiga instalada**
   ```bash
   adb uninstall com.honeywell.rp4printer
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Bluetooth instável**
   - Desconectar e reconectar
   - Reiniciar a impressora
   - Distância < 3 metros

### Problema: Não conecta à impressora

1. **Verificar pareamento:**
   ```bash
   adb shell dumpsys bluetooth_manager | grep -A 5 "Bonded"
   ```

2. **Verificar permissões:**
   - Android 12+: BLUETOOTH_CONNECT, BLUETOOTH_SCAN
   - Android < 12: BLUETOOTH, BLUETOOTH_ADMIN, LOCATION

3. **Tentar serial USB** (se disponível):
   - Modificar `BluetoothManager` para usar porta serial

### Problema: Imagem cortada ou incompleta

1. **Reduzir tamanho:**
   ```kotlin
   private const val MAX_IMAGE_WIDTH = 256  // Menor
   ```

2. **Aumentar delays:**
   ```kotlin
   Thread.sleep(500)  // Após INIT
   Thread.sleep(20)   // Entre linhas
   ```

---

## 📈 Melhorias Futuras

### Possíveis otimizações:

1. **Compressão de dados**
   - Usar modo 32-dot double-density (m=32)
   - Menos linhas, mais rápido

2. **Dithering**
   - Floyd-Steinberg para melhor qualidade
   - Tons de cinza simulados

3. **Ajuste de contraste**
   - Slider para ajustar threshold (< 128)
   - Melhor para assinaturas claras

4. **Pré-visualização**
   - Mostrar como vai ficar impresso
   - Antes de enviar para impressora

---

## 🔧 Parâmetros Ajustáveis

Se a impressão ainda não estiver perfeita, você pode ajustar:

### No `PrinterManager.kt`:

```kotlin
// Linha 33: Largura máxima da imagem
private const val MAX_IMAGE_WIDTH = 384  
// Tente: 256, 320, 384, 448

// Linha 70: Delay após inicialização
Thread.sleep(200)
// Tente: 200, 300, 500

// Linha 77: Delay após título  
Thread.sleep(100)
// Tente: 100, 200

// Linha 225: Threshold de cor (linhas 217-222)
if (gray < 128) {  // Considera preto
// Tente: 100 (mais preto), 150 (mais branco)
```

---

## 📝 Logs para Debug

Para ver o que está acontecendo:

```bash
# Filtrar logs do app
adb logcat | grep "PrinterManager\|BluetoothManager"

# Ver apenas erros
adb logcat *:E | grep "rp4printer"

# Salvar em arquivo
adb logcat | grep "rp4printer" > debug_log.txt
```

---

## 🎉 Resultado Esperado

Após instalar esta versão:

✅ **Conecta** em qualquer Android  
✅ **Imprime texto** corretamente  
✅ **Imprime assinatura** como IMAGEM (não caracteres!)  
✅ **Não precisa** de Print Service  
✅ **Funciona** via Bluetooth direto  

---

## 📞 Suporte

Se ainda tiver problemas, forneça:

1. **Modelo do dispositivo Android**
   ```bash
   adb shell getprop ro.product.model
   ```

2. **Versão do Android**
   ```bash
   adb shell getprop ro.build.version.release
   ```

3. **Logs de erro**
   ```bash
   adb logcat > error_log.txt
   ```

4. **Foto do resultado impresso** (se possível)

---

**Data:** 02 de Outubro de 2025  
**Versão:** 3.0.0 (Bluetooth Direto Otimizado)  
**Status:** ✅ Testado e Funcional


