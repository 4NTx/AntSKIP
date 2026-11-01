# AntSKIP

![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-active-blue)
![Size](https://img.shields.io/badge/APK%20Size-~700%20KB-success)
![License](https://img.shields.io/badge/license-private-lightgrey)

AntSKIP é um aplicativo Android ultra-leve que detecta e aperta automaticamente botões visíveis de pular em plataformas de streaming (Crunchyroll, Netflix, Prime Video, Disney+, Max, Paramount+ e mais). 

Construído em **Kotlin puro com zero dependências externas** (sem AndroidX, sem Compose, sem bibliotecas pesadas de terceiros), mantendo o APK em apenas **~700 KB** com velocidade máxima, consumo mínimo de memória e sem risco de travamentos.

O aplicativo opera de forma 100% ética e segura: não grava tela, não inspeciona tráfego de rede, não acessa contas ou DRM e roda totalmente offline no seu aparelho via `AccessibilityService`.

---

## Principais Novidades & Recursos

- **Visual Moderno Dark OLED**: Interface redesenhada com paleta dark profunda (`#0B0E14`), contraste alto, cantos arredondados modernos (16dp-20dp) e badges estilizados com as cores oficiais de cada streaming.
- **Detecção Ultrarrápida (120ms)**: Latência reduzida de 500ms para 120ms para acionar o botão instantaneamente assim que ele surgir na tela.
- **Clique Inteligente em Multi-camada**: 
  1. Clique nativo de acessibilidade (`ACTION_CLICK`).
  2. Escalamento automático na árvore de nós (para botões onde a ação de clique fica no container pai ou avô).
  3. **Universal Gesture Fallback**: Se o player do streaming estiver em um container customizado (SurfaceView, ExoPlayer customizado, Compose) que não responda a cliques de acessibilidade, o AntSKIP despacha um toque por coordenadas diretamente no centro do botão detectado.
- **Simulador Interativo Integrado**: Permite testar qualquer botão ou frase ("Pular abertura", "Skip Intro", etc.) em tempo real direto na interface para validar qual ação e app seriam acionados antes de assistir.
- **Detecção de Apps Instalados**: O app identifica automaticamente se a Netflix, Crunchyroll, Prime Video, Disney+, Max ou Paramount+ estão instalados no aparelho via declaração `<queries>`.
- **Prevenção de Falsos Cliques**: Validação rígida de bounding box para garantir que o app nunca clique na tela inteira do vídeo por engano (limite de 85% de largura e 28% de altura).
- **Filtro Especial Prime Video**: Regra que impede cliques acidentais nos menus e overlays do X-Ray ("Elenco", "Música", "Cenas").
- **Modo Dormir Inteligente**: Permite agendar horários recorrentes ou pausar o avanço de episódios por 1h, 2h ou 4h, além de opção para bloquear pulo de créditos durante o sono.
- **Editor Completo de Frases**: Adicione frases personalizadas em qualquer idioma ou bloqueie termos indesejados.

---

## Streamings Suportados

| App | Pacotes Android Suportados | Status |
| --- | --- | --- |
| **Crunchyroll** | `com.crunchyroll.crunchyroid`, `com.crunchyroll.crunchyrollapp` | Suporte Total (Intro, Recap, Créditos e Próximo Episódio) |
| **Netflix** | `com.netflix.mediaclient`, `com.netflix.ninja` | Suporte Total (Pular abertura, Resumo, Créditos e Próximo final) |
| **Prime Video** | `com.amazon.avod.thirdpartyclient`, `com.amazon.avod`, `com.amazon.amazonvideo.livingroom` | Suporte Total (Proteção avançada anti X-Ray) |
| **Disney+** | `com.disney.disneyplus`, `com.disney.disneyplus.tv` | Suporte Nativo |
| **Max** | `com.wbd.stream`, `com.hbo.hbonow`, `com.hbo.go` | Suporte Nativo |
| **Paramount+** | `com.cbs.app`, `com.paramountplus`, `com.cbs.ott` | Suporte Nativo |

---

## Ações de Pulo Suportadas

| Ação | Padrão | Exemplos de Texto Reconhecidos |
| --- | --- | --- |
| **Aberturas e intros** | Ativado | `Pular abertura`, `Skip Intro`, `Pular introdução`, `Skip Opening`, `Saltar intro` |
| **Recaps e resumos** | Ativado | `Pular resumo`, `Pular recapitulação`, `Skip Recap`, `Saltar resumen` |
| **Créditos e encerramentos** | Ativado | `Pular créditos`, `Pular encerramento`, `Skip Credits`, `Skip Ending` |
| **Previews e trailers** | Opcional | `Pular prévia`, `Pular trailer`, `Skip Preview` |
| **Próximo episódio** | Configurável | `Próximo`, `Próximo episódio`, `Next Episode`, `Play Next Episode` |

O banco de expressões nativo inclui termos em Português, Inglês, Espanhol, Francês, Alemão, Italiano, Holandês, Polonês, Japonês, Coreano, Chinês e Turco.

---

## Como Instalar e Ativar

1. Baixe o APK assinado ou gere o APK via Gradle:
   ```bash
   gradle assembleDebug
   ```
2. Instale o APK gerado no seu dispositivo Android.
3. Abra o **AntSKIP**.
4. Toque no botão **Ativar acessibilidade**.
5. Ative o serviço **AntSKIP**.
   *(Se o Android exibir a mensagem de "Configurações Restritas", use o botão "Permitir Configurações Restritas" dentro do AntSKIP para liberar o acesso).*
6. Escolha quais streamings e tipos de pulo você deseja ativos.
7. Abra seu streaming e assista ao seu conteúdo! Quando o AntSKIP pular um botão, aparecerá uma notificação discreta na tela (ex: `Pulando abertura`).

---

## Arquitetura do Projeto

O código foi totalmente modularizado e padronizado:

```text
app/src/main/kotlin/com/artur/antskip
├── accessibility/
│   └── AntSkipAccessibilityService.kt      # Serviço de Acessibilidade, timeout 120ms, cliques e gestos
├── data/
│   └── PreferenceStore.kt                 # Armazenamento e persistência das regras e preferências
├── domain/
│   ├── SkipAction.kt                      # Enum de ações de pulo
│   └── StreamingProvider.kt               # Enum de streamings com cores, aliases e regras padrão
├── matcher/
│   ├── NodeText.kt                        # Extração de textos e IDs de botões
│   ├── SkipMatcher.kt                     # Motor principal de matching e validação de bounding box
│   ├── SkipPhraseBank.kt                  # Dicionário poliglota de termos e expressões
│   └── TextNormalization.kt               # Normalizador de acentuação, casing e caracteres
└── ui/
    ├── MainActivity.kt                    # Atividade principal enxuta e desacoplada
    ├── ProviderRulesCopy.kt               # Textos de orientação e avisos por app
    ├── theme/
    │   └── AntTheme.kt                    # Design tokens Dark OLED, utilitários visuais e gradientes
    ├── components/
    │   ├── HeroStatusCard.kt              # Card de status em tempo real com indicador luminoso
    │   ├── ProviderCard.kt                # Card com cores oficiais, status de instalação e regras
    │   ├── ActionToggleCard.kt            # Linha de toggle para ações globais
    │   └── QuickToolsSection.kt           # Acesso rápido a Simulador, Frases, Logs e Updates
    ├── dialogs/
    │   ├── SimulatorDialog.kt             # Simulador de teste interativo de botões
    │   ├── ProviderConfigDialog.kt        # Diálogo de regras individuais por streaming
    │   ├── SleepModeDialog.kt             # Diálogo de modo sono e pausas temporárias
    │   ├── CustomPhrasesDialog.kt         # Editor de termos customizados e bloqueios
    │   └── DiagnosticsDialog.kt           # Visualizador e copiador de logs de diagnóstico
    └── updater/
        └── GitHubUpdateChecker.kt         # Checagem em background de novas releases no GitHub
```

---

## Modelo de Segurança & Privacidade

- **Zero Acesso a Vídeo**: Não acessa frames, codecs ou reprodução de vídeo.
- **Sem Captura de Tela**: Não utiliza APIs de `MediaProjection` ou screenshot.
- **Sem Interceptação de Rede**: Todas as regras de matching são avaliadas localmente.
- **100% Offline**: Nenhuma informação de uso, histórico ou diagnóstico sai do aparelho.
