# RELATÓRIO TÉCNICO: Migração Ez-Print → DPL com Assinatura Digital

## 📋 CONTEXTO

**Projeto:** `amelo/inbev/deliver-printer-android`  
**Package Afetada:** `com.abinbev.hive.printer.domain.documentprocessor.za`  
**Requisito:** Adicionar campo assinatura (JPEG) em TaxInvoice  
**POC Base:** RP4PrinterKotlin (Honeywell RP4 + DPL puro)

---

## 🎯 OBJETIVO

Refatorar documentos Ez-Print existentes para DPL puro, adicionando suporte para impressão de imagens (assinaturas) em formato JPEG.

---

## ⚠️ RISCOS CRÍTICOS

### 🔴 ALTO RISCO

#### 1. **Controle de altura do papel NÃO funciona**
- **Problema:** Durante toda a POC, não conseguimos fazer a impressora cortar no lugar certo
- **Tentativas falhadas:**
  - Comando `M####` (altura fixa) → Ignorado
  - Comando `o0` (altura automática) → Ignorado
  - Configuração GAP mode via SDK → Não aplicou
  - Comandos DPL puros → Ainda não testado com sucesso final
- **Impacto:** Desperdício de papel (~125mm por impressão vs ~30mm necessários)
- **Mitigação:** Investigar configuração física da impressora (sensor, tipo de papel)

#### 2. **Formato Ez-Print vs DPL são INCOMPATÍVEIS**
- **Ez-Print:** Linguagem proprietária Intermec/Honeywell (alto nível)
- **DPL:** Datamax Printer Language (baixo nível)
- **Impacto:** Reescrita TOTAL dos documentos, não é refatoração simples
- **Risco:** Perda de funcionalidades existentes se não mapeadas corretamente

#### 3. **SDK Honeywell gera comandos incorretos para DPL**
- **Descoberta:** `DocumentDPL.writeImage()` gera `XD`/`IDB` (download de gráfico)
- **Problema:** NÃO é um label DPL válido, comandos de altura são ignorados
- **Solução POC:** Implementação manual de DPL puro (sem SDK)
- **Risco:** Incompatibilidade com outros modelos de impressora

### 🟡 MÉDIO RISCO

#### 4. **Conversão JPEG → Bitmap monocromático**
- **Desafio:** JPEG colorido → 1 bit preto/branco
- **Impacto:** Qualidade da assinatura pode degradar
- **Mitigação:** Algoritmo de threshold ajustável (implementado na POC: 200/255)

#### 5. **Tamanho das assinaturas**
- **Problema:** Assinaturas grandes podem exceder largura da impressora (103mm = 743px @ 203dpi)
- **Solução POC:** Redimensionamento automático (1/3 do tamanho + fit to width)
- **Risco:** Assinatura muito pequena/ilegível

#### 6. **Desempenho - Conversão Bitmap → HEX**
- **Processo:** Cada pixel → bit → byte → HEX string
- **Exemplo:** 743x200px → ~18.000 bytes → ~36.000 chars HEX
- **Impacto:** Processamento pode demorar 1-3 segundos em dispositivos lentos
- **Mitigação:** Processar em background (coroutine)

### 🟢 BAIXO RISCO

#### 7. **Rotação de 180°**
- **POC:** Funciona perfeitamente com `Matrix.postRotate(180f)`
- **Motivo:** Impressora RP4 imprime de "cabeça para baixo"

#### 8. **Conexão Bluetooth**
- **POC:** `Connection_Bluetooth` do SDK funciona bem
- **Estável:** Não houve problemas de conexão durante testes

---

## 🏔️ DESAFIOS TÉCNICOS

### 1. **Corte de papel (NÃO RESOLVIDO na POC)** ⚠️
**Status:** 🔴 CRÍTICO - NÃO FUNCIONAL

**Tentativas realizadas:**
1. ✗ Comando `M1500` (altura fixa)
2. ✗ Comando `o0` (altura automática)
3. ✗ Comando `Q####` (label length)
4. ✗ Configuração de sensor via `MediaLabel_DPL.setSensorType(Gap)`
5. ✗ Comando `KcG` e `KcLW0405;`

**Hipóteses não testadas:**
- Impressora configurada para papel contínuo (não detecta gaps)
- Sensor físico posicionado incorretamente
- Firmware desatualizado
- Comando de corte explícito (`^`) não implementado

**Ações necessárias:**
1. Verificar tipo de papel (contínuo vs etiquetas com gap)
2. Acessar menu da impressora e verificar configurações de sensor
3. Testar com diferentes tipos de papel
4. Considerar módulo de corte automático (cutter) se disponível

**Impacto na produção:**
- Desperdício de papel: ~4x mais papel que necessário
- Custo operacional aumentado
- Possível rejeição pelo cliente

---

### 2. **Migração Ez-Print → DPL**

**Complexidade:** ALTA

**Mapeamento necessário:**

| Ez-Print | DPL Puro | Notas |
|----------|----------|-------|
| Texto simples | `A{x},{y},{rot},{font},{h},{v},{rev},"texto"` | Coordenadas diferentes |
| Formatação | Comandos individuais por linha | Sem auto-layout |
| Barcode | `B{type}...` | Sintaxe diferente |
| Imagem | `GW{x},{y},{w},{h},{hexData}` | Conversão manual |
| Layout | Manual (x,y absolutos) | Sem grid/table |

**Trabalho estimado:**
- Reescrever CADA documento
- Testar CADA campo/layout
- Validar com dados reais

---

### 3. **Processamento de JPEG**

**Passos necessários:**

```kotlin
JPEG → Bitmap (Android) → 
  Darken (threshold 200) → 
    Trim (remover espaços vazios) → 
      Scale (1/3 + fit width) → 
        Rotate 180° → 
          Monochrome (1 bit) → 
            HEX string → 
              DPL GW command
```

**Cada etapa pode falhar:**
- JPEG corrompido
- Out of memory (imagens grandes)
- Tempo de processamento
- Qualidade final

---

### 4. **Compatibilidade com múltiplos modelos**

**POC testada apenas em:** Honeywell RP4B

**Outros modelos podem ter:**
- Resoluções diferentes (300dpi vs 203dpi)
- Larguras diferentes
- Comandos DPL diferentes
- Bugs de firmware

**Necessário:** Teste em TODOS os dispositivos de produção

---

### 5. **Tratamento de erros**

**Cenários não cobertos na POC:**
- Impressora sem papel
- Bateria fraca
- Impressora desligada
- Dados corrompidos
- Timeout de conexão
- Impressão parcial

---

## ⏱️ ESTIMATIVA DE TEMPO

### 📊 Breakdown por etapa:

#### **FASE 1: Análise e Preparação** ⏱️ 2-3 dias
- [ ] Análise completa do código Ez-Print existente (1 dia)
- [ ] Mapeamento de todos os documentos/campos (0.5 dia)
- [ ] Setup ambiente de desenvolvimento (0.5 dia)
- [ ] Definição de estratégia de migração (0.5 dia)
- [ ] Resolução do problema de corte de papel (0.5 dia) ⚠️

#### **FASE 2: Desenvolvimento Core** ⏱️ 5-7 dias
- [ ] Implementar conversor JPEG → DPL HEX (1 dia)
- [ ] Criar classe base `DPLDocumentProcessor` (1 dia)
- [ ] Migrar TaxInvoice para DPL (2 dias)
  - Layout base
  - Campos de texto
  - Campos de assinatura (JPEG)
  - Testes unitários
- [ ] Tratamento de erros e edge cases (1 dia)
- [ ] Otimização de performance (1 dia)

#### **FASE 3: Migração Documentos Existentes** ⏱️ 3-5 dias
- [ ] Listar todos documentos Ez-Print (0.5 dia)
- [ ] Migrar documento por documento (2-3 dias)
  - Reescrever layout em DPL
  - Ajustar coordenadas
  - Validar campos
- [ ] Testes comparativos Ez-Print vs DPL (1 dia)

#### **FASE 4: Testes e QA** ⏱️ 3-5 dias
- [ ] Testes unitários (1 dia)
- [ ] Testes de integração (1 dia)
- [ ] Testes em múltiplos dispositivos (1 dia)
- [ ] Testes de stress (imagens grandes, muitas impressões) (0.5 dia)
- [ ] Validação com cliente/usuários (1 dia)

#### **FASE 5: Documentação e Deploy** ⏱️ 2-3 dias
- [ ] Documentação técnica (1 dia)
- [ ] Guia de troubleshooting (0.5 dia)
- [ ] Code review (0.5 dia)
- [ ] Deploy gradual (1 dia)

---

### 📈 TOTAL ESTIMADO

| Cenário | Tempo | Observações |
|---------|-------|-------------|
| **Otimista** | 15 dias | Sem problemas graves, corte de papel resolvido rápido |
| **Realista** | 20-25 dias | Alguns problemas, retrabalho moderado |
| **Pessimista** | 30-35 dias | Problema de corte não resolvido, muitos edge cases |

**⚠️ ATENÇÃO:** Estimativa NÃO inclui o tempo para resolver o problema de corte de papel se requerer:
- Troca de hardware (módulo de corte)
- Atualização de firmware
- Troca de tipo de papel
- Suporte técnico da Honeywell

---

## 📋 RECOMENDAÇÕES

### 🔴 CRÍTICAS (Fazer ANTES de iniciar)

1. **Resolver problema de corte de papel**
   - Testar com diferentes tipos de papel
   - Verificar configurações físicas da impressora
   - Contatar suporte Honeywell se necessário
   - Considerar aceitar desperdício de papel como limitação conhecida

2. **Validar viabilidade com cliente**
   - Demonstrar POC atual (funciona mas desperdiça papel)
   - Obter aprovação para prosseguir mesmo com limitação
   - Definir critérios de aceitação

3. **Setup ambiente de testes**
   - Múltiplos dispositivos RP4B
   - Diferentes tipos de papel
   - Dados de teste realistas (TaxInvoice reais)

### 🟡 IMPORTANTES

4. **Criar camada de abstração**
   - Interface `PrinterProcessor` para permitir múltiplas implementações
   - Manter Ez-Print como fallback durante transição
   - Feature flag para habilitar/desabilitar DPL

5. **Implementar logs detalhados**
   - Comandos DPL enviados (HEX + ASCII)
   - Tempo de processamento
   - Erros de impressora
   - Métricas de uso

6. **Testes automatizados**
   - Unit tests para conversão JPEG → HEX
   - Integration tests para geração DPL
   - Snapshot tests para layouts

### 🟢 DESEJÁVEIS

7. **Otimizações futuras**
   - Cache de imagens convertidas
   - Compressão de dados DPL
   - Impressão em background

8. **Monitoramento**
   - Taxa de sucesso de impressões
   - Tempo médio de impressão
   - Falhas mais comuns

---

## 🎯 CRITÉRIOS DE SUCESSO

### Mínimo Viável (MVP)
- [ ] TaxInvoice imprime com assinatura JPEG
- [ ] Assinatura legível (tamanho adequado)
- [ ] Rotação correta (180°)
- [ ] Sem crash/errors em produção
- [ ] ⚠️ OK se desperdiçar papel (problema conhecido)

### Ideal
- [ ] Tudo do MVP +
- [ ] Corte de papel no lugar correto
- [ ] Tempo de impressão < 5 segundos
- [ ] Funciona em todos os modelos de impressora
- [ ] 0 desperdício de papel

---

## 📚 REFERÊNCIAS TÉCNICAS

### Código POC
- `HoneywellSDKPrinter.kt` - Implementação DPL puro
- `bitmapToDplHex()` - Conversão bitmap para HEX
- `createRotatedLabel()` - Pipeline de processamento de imagem

### Comandos DPL Chave
```
\u0002n         - Reset
\u0002o0        - Altura automática (NÃO funcional na POC)
\u0002L         - Início do label
GW{x},{y},{w},{h},{hex} - Graphics Write (imagem)
P1              - Quantidade
\u0002E         - Fim/Imprime
```

### Documentação
- DPL Programming Guide (Datamax)
- Honeywell RP4 Technical Manual
- DO_AndroidSDK_v2.4.9 Documentation

---

## ⚡ DECISÕES URGENTES NECESSÁRIAS

1. **Aceitar limitação de corte de papel?**
   - SIM → Prosseguir com implementação
   - NÃO → Parar até resolver (pode levar semanas)

2. **Manter Ez-Print como fallback?**
   - SIM → Mais tempo de desenvolvimento, mais seguro
   - NÃO → Mais rápido, mais arriscado

3. **Deploy gradual ou big bang?**
   - Gradual → Feature flag, rollout por região
   - Big Bang → Todos de uma vez, mais arriscado

---

**Elaborado por:** AI Assistant  
**Data:** 2025-10-07  
**Versão:** 1.0  
**Status:** 🟡 AGUARDANDO DECISÕES CRÍTICAS
