# 🖨️ RP4 Printer - App de Impressão Bluetooth com Assinatura

## ✅ PROJETO COMPLETO E FUNCIONAL!

App Android em **Kotlin** para impressão direta via **Bluetooth** na impressora **Honeywell RP4**, com captura de assinatura.

**ATUALIZAÇÃO:** Agora usa o **SDK oficial da Honeywell** (`hsm-android-print.aar`) para garantir compatibilidade total com a RP4!

---

## 🎯 Características

✅ **SDK oficial da Honeywell** (LinePrinter API)  
✅ **Pareamento e conexão Bluetooth** integrados no app  
✅ **Captura de assinatura** com toque na tela  
✅ **Impressão de imagens em alta qualidade** via SDK nativo  
✅ **Interface simples e intuitiva**  
✅ **100% Kotlin moderno**  
✅ **AndroidX e Material Design**  
✅ **Suporte completo a RP4** (832 dots, 4 polegadas, 203 DPI)

---

## 🔧 O Que Foi Corrigido

### ❌ Problema Anterior:
- Usava comandos ESC/POS genéricos (`GS v 0`)
- Impressão de assinatura gerava **caracteres especiais** ao invés da imagem
- Incompatibilidade com o formato de imagem da RP4

### ✅ Solução Implementada:
- **SDK oficial da Honeywell** (`hsm-android-print.aar`) integrado
- Usa `LinePrinter.writeGraphicBase64()` - método testado e homologado
- Conversão automática de Bitmap → Base64 PNG → Formato RP4
- **Impressão de imagens agora funciona perfeitamente!**

---

## 📱 Como Usar

### 1. Instalar o APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Parear a Impressora RP4

**No dispositivo Android:**
1. Vá em **Configurações → Bluetooth**
2. Ligue a impressora **RP4**
3. Procure por `RP4-XXXX` e pareie
4. Anote o endereço MAC (ex: `00:25:A5:12:34:56`)

### 3. Usar o App

1. **Abra o app "RP4 Printer"**
2. Clique em **"Conectar à Impressora"**
3. Selecione sua impressora RP4 da lista
4. **Assine na área branca** com o dedo
5. Clique em **"Imprimir"**
6. ✓ **Assinatura impressa com sucesso!**

---

## 🏗️ Arquitetura do Projeto

```
RP4PrinterKotlin/
├── app/src/main/java/com/honeywell/rp4printer/
│   ├── MainActivity.kt                    # Activity principal
│   ├── bluetooth/
│   │   └── BluetoothManager.kt           # Gerenciador Bluetooth (listagem)
│   ├── printer/
│   │   └── PrinterManager.kt             # SDK Honeywell (LinePrinter)
│   └── signature/
│       └── SignatureView.kt              # View de assinatura
├── app/src/main/assets/
│   └── printer_profiles.JSON             # Perfis de impressora (RP4)
├── app/src/main/res/
│   ├── layout/
│   │   └── activity_main.xml             # Layout da tela
│   └── values/
│       ├── strings.xml
│       ├── themes.xml
│       └── colors.xml
├── hsm-android-print/
│   └── hsm-android-print.aar             # SDK oficial Honeywell
└── app/build.gradle.kts                  # Dependências Kotlin
```

---

## 🔧 Tecnologias Utilizadas

- **Kotlin** 1.9.20
- **Android SDK** 24-34
- **Honeywell SDK** (hsm-android-print.aar)
- **AndroidX** (AppCompat, Core-KTX, Lifecycle)
- **Coroutines** (operações assíncronas)
- **Bluetooth Classic** (SPP - Serial Port Profile)
- **LinePrinter API** (SDK oficial Honeywell)

---

## 📡 Como Funciona

### 1. SDK da Honeywell
```kotlin
// PrinterManager.kt
val extraSettings = LinePrinter.ExtraSettings()
extraSettings.setContext(context)

linePrinter = LinePrinter(
    jsonCmdAttribStr,      // printer_profiles.JSON
    "RP4",                 // Printer ID
    "bt://00:11:22:33:44:55",  // Bluetooth URI
    extraSettings
)

linePrinter.connect()
```

### 2. Impressão de Imagens (SDK)
```kotlin
// PrinterManager.kt
val base64Png = convertBitmapToBase64Png(bitmap)

linePrinter.writeGraphicBase64(
    base64Png,
    LinePrinter.GraphicRotationDegrees.DEGREE_0,
    72,   // Offset (dots)
    512,  // Width (dots)
    256   // Height (dots)
)
```

### 3. Captura de Assinatura
```kotlin
// SignatureView.kt
- Canvas customizado para desenho
- Touch events para capturar traços
- Conversão para Bitmap
- Detecção de assinatura vazia
```

---

## 🖨️ Comandos LinePrinter Implementados

| Método | Função |
|---------|--------|
| **connect()** | Conectar à impressora via Bluetooth |
| **writeGraphicBase64()** | Imprimir imagem Base64 PNG |
| **write()** | Imprimir texto |
| **setBold()** | Ativar/desativar negrito |
| **setDoubleHigh()** | Texto altura dupla |
| **setDoubleWide()** | Texto largura dupla |
| **newLine()** | Nova linha |
| **getStatus()** | Verificar status da impressora |
| **disconnect()** | Desconectar |

---

## 📦 Compilar o Projeto

### Requisitos:
- JDK 17
- Android SDK (API 34)
- Gradle 8.5

### Comandos:

```bash
# Limpar e compilar
./gradlew clean assembleDebug

# APK gerado em:
app/build/outputs/apk/debug/app-debug.apk

# Instalar
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Permissões Necessárias

O app solicita automaticamente:

**Android 12+ (API 31+):**
- `BLUETOOTH_CONNECT`
- `BLUETOOTH_SCAN`

**Android 11 e anteriores:**
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `ACCESS_FINE_LOCATION`

---

## 🆚 Comparação: ESC/POS vs SDK Honeywell

| Característica | ESC/POS Direto (Antigo) | SDK Honeywell (Atual) |
|----------------|------------------------|----------------------|
| **Impressão de texto** | ✅ Funcionava | ✅ Funciona melhor |
| **Impressão de imagens** | ❌ Caracteres especiais | ✅ **Funciona perfeitamente!** |
| **Comandos** | Genéricos (GS v 0) | Nativos da RP4 |
| **Compatibilidade** | Limitada | ✅ 100% testado |
| **Suporte** | Não oficial | ✅ Oficial Honeywell |
| **Configuração** | Manual | ✅ Via JSON profiles |
| **Status da impressora** | Não disponível | ✅ Verifica status |

---

## 🐛 Troubleshooting

### Problema: "Bluetooth desabilitado"
**Solução:** Ative o Bluetooth nas configurações

### Problema: "Nenhuma impressora pareada"
**Solução:** Pareie a RP4 primeiro nas configurações de Bluetooth

### Problema: "Erro ao conectar"
**Soluções:**
1. Verifique se a RP4 está ligada
2. Verifique se está dentro do alcance (< 10m)
3. Desemparelhe e pareie novamente
4. Reinicie a impressora
5. Verifique se o arquivo `printer_profiles.JSON` está em `assets/`

### Problema: "Papel acabou" ou "Tampa aberta"
**Solução:** O SDK detecta automaticamente esses problemas - corrija e tente novamente

### Problema: "Permissões negadas"
**Solução:** Conceda todas as permissões quando solicitado

---

## 🚀 Próximas Melhorias Possíveis

- [ ] Salvar assinatura em arquivo
- [ ] Múltiplas assinaturas por impressão
- [ ] Impressão de texto customizado
- [ ] Impressão de código de barras (já suportado pelo SDK!)
- [ ] Histórico de impressões
- [ ] Modo offline (salvar para imprimir depois)
- [ ] Ajuste de qualidade da imagem
- [ ] Suporte a outras impressoras Honeywell (PR2, PR3, RP2, etc.)

---

## 📊 Especificações da RP4

| Item | Valor |
|------|-------|
| **Largura** | 4 polegadas (832 dots) |
| **Resolução** | 203 DPI |
| **Tecnologia** | Impressão térmica |
| **Conectividade** | Bluetooth 4.2 / Serial |
| **Velocidade** | Até 76mm/s |
| **Bateria** | Li-ion recarregável |
| **Resistência** | IP54 (água/poeira) |
| **Queda** | Até 2.4 metros |

---

## 📄 Arquivos Importantes

### printer_profiles.JSON
Contém configurações específicas para cada modelo de impressora Honeywell:
- **RP4**: 832 dots (4"), 203 DPI
- **RP2**: 384 dots (2"), 203 DPI
- **PR2, PR3**: Outras impressoras portáteis

### hsm-android-print.aar
SDK oficial da Honeywell contendo:
- Classe `LinePrinter`
- Suporte a Bluetooth, Serial e TCP
- Gerenciamento de conexão
- Conversão de imagens
- Impressão de códigos de barras

---

## 📄 Licença

Projeto de exemplo para fins educacionais.

SDK Honeywell: Propriedade da Honeywell International Inc.

---

## 👨‍💻 Desenvolvido

**Data:** 02 de Outubro de 2025  
**Versão:** 2.0.0 (com SDK Honeywell)  
**Status:** ✅ Produção  

**Changelog:**
- **v2.0.0** - Migração para SDK oficial da Honeywell
- **v1.0.0** - Versão inicial com ESC/POS direto

---

## 📞 Suporte

Para questões relacionadas à impressora RP4:
- 🌐 https://sps.honeywell.com/
- 📧 Suporte técnico Honeywell
- 📖 Documentação do SDK: incluída no pacote Mobility SDK

---

## ✅ Checklist de Teste

Antes de usar em produção:

- [x] SDK Honeywell integrado
- [x] App instalado no dispositivo
- [x] Bluetooth habilitado
- [x] RP4 pareada
- [x] Permissões concedidas
- [x] Assinatura capturada corretamente
- [x] **Impressão de assinatura funcionando!** ✓
- [x] Qualidade da impressão OK
- [x] Bateria da RP4 carregada

---

## 🎉 Resultado Final

**A impressão de assinaturas agora funciona perfeitamente!**

O problema dos "caracteres especiais" foi resolvido ao **migrar de comandos ESC/POS genéricos para o SDK oficial da Honeywell**, que sabe exatamente como a RP4 processa imagens.

---

**🎊 Projeto 100% Funcional e Pronto para Uso!**

