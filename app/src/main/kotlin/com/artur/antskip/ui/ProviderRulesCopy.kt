package com.artur.antskip.ui

import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider

object ProviderRulesCopy {
    fun summary(provider: StreamingProvider, preferences: PreferenceStore): String {
        if (!preferences.isProviderEnabled(provider)) {
            return "O AntSKIP ignora este app ate voce ligar em Apps monitorados."
        }

        val enabledActions = SkipAction.entries
            .filter { action -> preferences.isActionEnabledForProvider(provider, action) }
            .map { action -> action.shortLabel() }

        if (enabledActions.isEmpty()) {
            return "Monitorado, mas nenhum tipo de botao esta permitido."
        }

        return "Permitido: ${enabledActions.joinToString(", ")}."
    }

    fun guidance(provider: StreamingProvider): String =
        when (provider) {
            StreamingProvider.NETFLIX ->
                "A Netflix vem configurada para intros, recaps, creditos e Proximo. Para assistir encerramentos completos, desligue Creditos."
            StreamingProvider.PRIME_VIDEO ->
                "Suporte mais conservador. Mantenha Proximo episodio desligado se notar qualquer avanco antecipado."
            StreamingProvider.CRUNCHYROLL ->
                "Mais confiavel para intros e recaps. Proximo episodio deve ficar ligado apenas se voce quer avancar sem confirmar."
            else ->
                "Experimental. Comece com Aberturas e intros, teste um episodio e depois ligue outras opcoes."
        }

    fun actionDescription(provider: StreamingProvider, action: SkipAction): String =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS ->
                "Ligado: toca em Pular creditos. Desligado: preserva o encerramento do episodio."
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE ->
                "Ligado: toca no botao final Proximo. Desligado: nao avanca episodios sozinho na Netflix."
            provider == StreamingProvider.PRIME_VIDEO && action == SkipAction.NEXT_EPISODE ->
                "Ligado: tenta avancar episodios no Prime Video. Mantenha desligado para evitar cliques antecipados."
            action == SkipAction.NEXT_EPISODE ->
                "Ligado: avanca para outro episodio. Desligado: voce escolhe manualmente quando continuar."
            else -> action.description
        }

    fun actionRecommendation(provider: StreamingProvider, action: SkipAction): String? =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS -> "Ligado por padrao"
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE -> "Ligado por padrao"
            provider == StreamingProvider.PRIME_VIDEO && action == SkipAction.NEXT_EPISODE -> "Recomendado desligado"
            action == SkipAction.PREVIEW -> "Opcional"
            action in setOf(SkipAction.INTRO, SkipAction.RECAP) -> "Recomendado ligado"
            else -> null
        }

    fun actionWarning(provider: StreamingProvider, action: SkipAction): String? =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE ->
                "Na Netflix, esta opcao deve tocar no Proximo final. Se houver avanco antecipado, desligue e envie os logs de diagnostico."
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS ->
                "Pode pular parte do encerramento. Desligue se voce prefere assistir os creditos."
            action == SkipAction.NEXT_EPISODE ->
                "Esta opcao avanca para outro episodio automaticamente."
            else -> null
        }

    private fun SkipAction.shortLabel(): String =
        when (this) {
            SkipAction.INTRO -> "intro"
            SkipAction.RECAP -> "recap"
            SkipAction.CREDITS -> "creditos"
            SkipAction.PREVIEW -> "preview"
            SkipAction.NEXT_EPISODE -> "proximo"
        }
}
