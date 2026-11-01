# AntSKIP

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-active-blue)
![Size](https://img.shields.io/badge/APK%20Size-~680%20KB-success)
![License](https://img.shields.io/badge/license-private-lightgrey)

O **AntSKIP** é um aplicativo Android profissional e ultra-leve projetado para automatizar cliques em botões de pular em streamings (**Netflix**, **Crunchyroll** e **Prime Video**) e fornecer um **Temporizador de Sono (Sleep Timer)** universal que desliga a tela e pausa qualquer mídia sem jamais afetar seus alarmes.

Construído em **Kotlin puro com zero dependências externas**, o APK tem apenas **~680 KB**, consome o mínimo absoluto de bateria e memória RAM, e roda 100% offline via `AccessibilityService`.

---

## 🌟 Principais Recursos

- **Pulo Automático Instantâneo (~100ms)**: Toca em aberturas, resumos, créditos e avança episódios automaticamente no momento exato em que o botão surge na tela.
- **Clique Inteligente com Fallback por Gestos**: Se o player do streaming estiver usando camadas customizadas (SurfaceView/Compose) que bloqueiam o clique de acessibilidade comum, o AntSKIP toca automaticamente por coordenadas no centro do botão.
- **🌙 Temporizador de Sono & Desligar Tela (Sleep Timer)**:
  - **Pausa Universal**: Pausa Spotify, YouTube, Netflix, podcasts ou qualquer áudio/vídeo tocando no aparelho.
  - **🛡️ 100% Seguro para Alarmes**: O canal de alarmes do sistema (`STREAM_ALARM`) **nunca é tocado ou silenciado**. O despertador tocará normalmente no dia seguinte.
  - **Desliga / Bloqueia a Tela**: Apaga a tela do celular via acessibilidade de forma suave, mantendo o desbloqueio biométrico (digital e facial) ativo ao religar.
  - **Notificação Interativa**: Notificação ao vivo com botões de ajuste direto (`+5 min`, `-5 min` e `Parar`).
- **Visual Dark OLED Profundo**: Interface elegante, limpa e pensada para telas AMOLED (`#0B0E14`), sem menus desnecessários ou opções confusas.
- **Simulador de Teste**: Ferramenta no app para testar frases de botões em tempo real.

---

## 🎬 Streamings Suportados

| Streaming | Pacotes Android Suportados | Recursos |
| --- | --- | --- |
| **Netflix** | `com.netflix.mediaclient`, `com.netflix.ninja` | Pular Abertura, Resumo, Créditos e botão final Próximo |
| **Crunchyroll** | `com.crunchyroll.crunchyroid`, `com.crunchyroll.crunchyrollapp` | Pular Abertura, Recaps e Avanço de Episódio |
| **Prime Video** | `com.amazon.avod.thirdpartyclient`, `com.amazon.avod`, `com.amazon.amazonvideo.livingroom` | Pular Intro e Créditos com proteção anti-menus X-Ray |

---

## ⚡ Ações Suportadas

- **Pular Aberturas**: Pula intros e temas de abertura automaticamente.
- **Pular Resumos**: Pula recaps do episódio anterior.
- **Pular Créditos**: Pula os créditos finais para não perder tempo.
- **Próximo Episódio**: Avança para o próximo episódio automaticamente sem precisar de controle ou toque manual.

---

## 📱 Como Instalar e Usar

1. Compile o APK ou baixe o APK assinado:
   ```bash
   gradle assembleRelease
   ```
2. Instale o APK no seu aparelho Android.
3. Abra o **AntSKIP** e toque em **Ativar Acessibilidade**.
4. Ative o serviço **AntSKIP**.
5. Pronto! Basta abrir a Netflix, Crunchyroll ou Prime Video. Quando um botão surgir, o AntSKIP aperta sozinho para você.

---

## 🏗️ Estrutura do Projeto

```text
app/src/main/kotlin/com/artur/antskip
├── accessibility/
│   └── AntSkipAccessibilityService.kt      # Detecção rápida, cliques, gestos e desligamento de tela
├── data/
│   └── PreferenceStore.kt                 # Preferências simplificadas e limpas
├── domain/
│   ├── SkipAction.kt                      # Ações de pulo essenciais
│   └── StreamingProvider.kt               # Netflix, Crunchyroll e Prime Video
├── matcher/
│   ├── NodeText.kt                        # Extração de texto e IDs de botões
│   ├── SkipMatcher.kt                     # Validação de bounding box, IDs e anti-falso-clique
│   ├── SkipPhraseBank.kt                  # Dicionário poliglota de termos
│   └── TextNormalization.kt               # Normalizador de caracteres e acentos
├── service/
│   └── SleepTimerService.kt               # Foreground service do Sleep Timer com controles na notificação
└── ui/
    ├── MainActivity.kt                    # Atividade limpa, sem recriações de tela
    ├── theme/
    │   └── AntTheme.kt                    # Design tokens Dark OLED
    ├── components/
    │   ├── HeroStatusCard.kt              # Status principal e chave geral
    │   ├── SleepTimerCard.kt              # Card do Sleep Timer com presets e contagem ao vivo
    │   ├── ProviderCard.kt                # Cards de cada streaming com switch direto
    │   ├── ActionToggleCard.kt            # Toggles das 4 ações principais
    │   └── QuickToolsSection.kt           # Acesso rápido a Testar Botão, Logs e Atualizações
    ├── dialogs/
    │   ├── SimulatorDialog.kt             # Teste de frases em tempo real
    │   └── DiagnosticsDialog.kt           # Visualizador e copiador de logs
    └── updater/
        └── GitHubUpdateChecker.kt         # Checagem em background de releases no GitHub
```

---

## 🛡️ Segurança e Privacidade

- **100% Offline**: Sem coleta de dados, sem anúncios e sem rastreadores.
- **Sem Gravação de Tela**: Não usa APIs de captura de tela (`MediaProjection`).
- **Segurança para Alarmes**: O Sleep Timer nunca silencia o canal de alarmes do Android.
