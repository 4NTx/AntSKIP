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
                "Para evitar pulo cedo, deixe Creditos desligado. Proximo episodio usa o botao final Proximo."
            StreamingProvider.PRIME_VIDEO ->
                "Suporte mais conservador. Mantenha Proximo episodio desligado se notar qualquer clique cedo."
            StreamingProvider.CRUNCHYROLL ->
                "Mais confiavel para intros e recaps. Proximo episodio deve ser ligado so se voce quer avancar sem confirmar."
            else ->
                "Experimental. Comece com Aberturas e intros, teste um episodio, depois ligue outras opcoes."
        }

    fun actionDescription(provider: StreamingProvider, action: SkipAction): String =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS ->
                "Ligado: pode tocar em Pular creditos. Desligado: evita avancar antes do fim real do episodio."
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE ->
                "Ligado: toca no botao final Proximo. Desligado: nunca avanca episodio sozinho na Netflix."
            provider == StreamingProvider.PRIME_VIDEO && action == SkipAction.NEXT_EPISODE ->
                "Ligado: tenta avancar episodio no Prime Video. Desligado recomendado para evitar cliques cedo."
            action == SkipAction.NEXT_EPISODE ->
                "Ligado: pode avancar para outro episodio. Desligado: voce escolhe manualmente quando continuar."
            else -> action.description
        }

    fun actionRecommendation(provider: StreamingProvider, action: SkipAction): String? =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS -> "Recomendado desligado"
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE -> "Opcional para maratona"
            provider == StreamingProvider.PRIME_VIDEO && action == SkipAction.NEXT_EPISODE -> "Recomendado desligado"
            action == SkipAction.PREVIEW -> "Opcional"
            action in setOf(SkipAction.INTRO, SkipAction.RECAP) -> "Recomendado ligado"
            else -> null
        }

    fun actionWarning(provider: StreamingProvider, action: SkipAction): String? =
        when {
            provider == StreamingProvider.NETFLIX && action == SkipAction.NEXT_EPISODE ->
                "Na Netflix, esta opcao deve tocar no Proximo final. Se pular cedo, desligue e mande os logs."
            provider == StreamingProvider.NETFLIX && action == SkipAction.CREDITS ->
                "Pode esconder o fim do episodio. Mantenha desligado se voce quer assistir os creditos."
            action == SkipAction.NEXT_EPISODE ->
                "Esta opcao muda de episodio automaticamente."
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
