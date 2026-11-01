package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.ui.theme.AntTheme

class CustomPhrasesDialog(
    private val context: Context,
    private val preferences: PreferenceStore,
) {
    fun show(onSaved: () -> Unit = {}) {
        val actions = listOf("Frases Bloqueadas") + SkipAction.entries.map { "Ação: ${it.label}" }

        AlertDialog.Builder(context)
            .setTitle("Editor de Frases e Palavras-Chave")
            .setItems(actions.toTypedArray()) { _, which ->
                if (which == 0) {
                    showBlockedPhrasesEditor(onSaved)
                } else {
                    val action = SkipAction.entries[which - 1]
                    showActionPhraseEditor(action, onSaved)
                }
            }
            .setNegativeButton("Voltar", null)
            .show()
    }

    private fun showActionPhraseEditor(action: SkipAction, onSaved: () -> Unit) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        val title = TextView(context).apply {
            text = "Frases Customizadas: ${action.label}"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }

        val hintView = TextView(context).apply {
            text = "Digite uma frase por linha. Se o app mostrar um botão com essa frase, o AntSKIP disparará a ação ${action.label}."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 12))
        }

        val editText = EditText(context).apply {
            val phrases = preferences.customPhrases(action)
            setText(phrases.joinToString("\n"))
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setHintTextColor(AntTheme.TEXT_MUTED)
            setHint("Exemplo:\nPular Abertura Especial\nSaltar al episodio")
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12))
            minLines = 6
        }

        container.addView(title)
        container.addView(hintView)
        container.addView(editText)

        AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val lines = editText.text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toCollection(linkedSetOf())
                preferences.setCustomPhrases(action, lines)
                Toast.makeText(context, "Frases salvas com sucesso!", Toast.LENGTH_SHORT).show()
                onSaved()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showBlockedPhrasesEditor(onSaved: () -> Unit) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        val title = TextView(context).apply {
            text = "Frases Bloqueadas"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }

        val hintView = TextView(context).apply {
            text = "Digite uma frase por linha. Se um botão contiver qualquer uma dessas frases, ele será estritamente IGNORADO."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 12))
        }

        val editText = EditText(context).apply {
            val phrases = preferences.blockedPhrases()
            setText(phrases.joinToString("\n"))
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setHintTextColor(AntTheme.TEXT_MUTED)
            setHint("Exemplo:\nAssinar Premium\nComprar Episódio")
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12))
            minLines = 6
        }

        container.addView(title)
        container.addView(hintView)
        container.addView(editText)

        AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val lines = editText.text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toCollection(linkedSetOf())
                preferences.setBlockedPhrases(lines)
                Toast.makeText(context, "Lista de bloqueios salva!", Toast.LENGTH_SHORT).show()
                onSaved()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
