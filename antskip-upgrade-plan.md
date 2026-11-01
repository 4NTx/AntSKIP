# AntSKIP Upgrade Plan

## Objetivo
Transformar o AntSKIP em um aplicativo de controle de streaming referência: código modular, limpo, estável, com UI moderna Dark OLED e detecção rápida e infalível para Netflix, Crunchyroll, Prime Video, Disney+, Max e Paramount+.

## Fases de Execução

### Fase 1: Mecanismo de Detecção e Acessibilidade (Core Engine)
- [ ] Atualizar `ant_skip_accessibility_service.xml` para timeout ágil (120ms) e flags adequadas.
- [ ] Atualizar `StreamingProvider.kt` com todos os aliases de pacotes e paleta de cores oficial de cada serviço.
- [ ] Atualizar `AntSkipAccessibilityService.kt` com registro dinâmico de pacotes, fallback de gestos universal (toque por coordenadas quando `ACTION_CLICK` falha em overlays do player), eliminação de chamadas obsoletas (`recycle()`).
- [ ] Expandir `SkipPhraseBank.kt` e `SkipMatcher.kt` com banco abrangente de termos PT/EN/ES/JA/FR/DE, IDs de recursos conhecidos e validação aprimorada de bounding box.

### Fase 2: Design System Dark & Arquitetura Modular da UI
- [ ] Criar `AntTheme.kt`: Paleta Dark OLED (`#0D1117`, `#161B22`, `#21262D`), cores de acento, tipografia, elevação e utilitários de desenho com gradientes e cantos arredondados modernos.
- [ ] Criar `ui/components/`: Componentes reutilizáveis (HeroStatusCard, MetricBadge, ProviderCard, SwitchRow, ActionChip, AntButton).
- [ ] Criar `ui/dialogs/`: Modularizar os diálogos (ProviderConfigDialog, SleepModeDialog, PhraseEditorDialog, DiagnosticsDialog, SimulatorDialog).
- [ ] Criar `ui/updater/GitHubUpdateChecker.kt`: Isolar a lógica de atualização em segundo plano com callback tipado.

### Fase 3: Redesenho Completo da MainActivity
- [ ] Refatorar `MainActivity.kt` para orquestrador limpo e focado (< 300 linhas).
- [ ] Implementar suporte completo a Edge-to-Edge moderno (API 30+ Insets).
- [ ] Adicionar status visual em tempo real (indicador de serviço ativo/inativo, contagem de apps instalados).
- [ ] Adicionar "Simulador de Teste" em tempo real para verificar frases e botões diretamente na tela.

### Fase 4: Testes Unitários e Validação do Build
- [ ] Configurar JUnit no `app/build.gradle`.
- [ ] Criar testes unitários para `SkipMatcherTest.kt`, `SkipPhraseBankTest.kt` e `TextNormalizationTest.kt`.
- [ ] Executar `gradle test` e `gradle assembleDebug` para garantir 0 erros e 0 warnings impeditivos.
- [ ] Atualizar `README.md` refletindo as melhorias e novos recursos.
