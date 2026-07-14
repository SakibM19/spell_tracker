package com.example.spelltracker.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.LinearLayout
import com.example.spelltracker.data.AppDatabase
import com.example.spelltracker.data.MistakeWord
import com.example.spelltracker.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

/**
 * A minimal custom keyboard.
 *
 * Why a custom keyboard instead of an Accessibility Service:
 * - The user has to deliberately switch to this keyboard, which IS the consent step.
 * - This service only ever sees characters typed while THIS keyboard is active.
 *   It cannot read other apps' screens, other keyboards' input, or anything
 *   outside of what is typed here.
 * - Only words flagged as misspelled are saved (just the word + a timestamp).
 *   The full sentence / message content is never stored.
 */
class SpellTrackerIME : InputMethodService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var prefs: Prefs
    private lateinit var db: AppDatabase

    private var spellCheckerSession: SpellCheckerSession? = null
    private val currentWord = StringBuilder()
    private var isShifted = false
    private var isSymbolMode = false

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        db = AppDatabase.getDatabase(this)

        val tsm = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        spellCheckerSession = tsm.newSpellCheckerSession(
            null,
            null, // use device default locale
            spellListener,
            true
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        spellCheckerSession?.close()
        spellCheckerSession = null
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(0xFFE0E0E0.toInt())
            setPadding(4, 4, 4, 4)
        }

        buildRows(root)
        return root
    }

    private fun buildRows(root: LinearLayout) {
        root.removeAllViews()

        val letterRows = listOf(
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        )
        val symbolRows = listOf(
            "1234567890",
            "@#\$%&*-+()",
            "!\"':;/?,."
        )

        val rows = if (isSymbolMode) symbolRows else letterRows

        rows.forEachIndexed { index, rowChars ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // On the last letter row, add a shift key at the start
            if (!isSymbolMode && index == 2) {
                rowLayout.addView(makeKey("SHIFT", "\u21E7", 1.5f))
            }

            rowChars.forEach { c ->
                val label = if (!isSymbolMode && isShifted) c.uppercaseChar().toString() else c.toString()
                rowLayout.addView(makeKey(label, label, 1f))
            }

            if (!isSymbolMode && index == 2) {
                rowLayout.addView(makeKey("BACKSPACE", "\u232B", 1.5f))
            }

            root.addView(rowLayout)
        }

        // Bottom function row
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        bottomRow.addView(makeKey("MODE", if (isSymbolMode) "ABC" else "123", 1.2f))
        bottomRow.addView(makeKey(",", ",", 0.8f))
        bottomRow.addView(makeKey("SPACE", "space", 3f))
        bottomRow.addView(makeKey(".", ".", 0.8f))
        bottomRow.addView(makeKey("ENTER", "\u23CE", 1.2f))
        root.addView(bottomRow)
    }

    private fun makeKey(key: String, label: String, weight: Float): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            setOnClickListener { handleKey(key) }
        }
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "SHIFT" -> {
                isShifted = !isShifted
                refreshKeyboardView()
            }
            "MODE" -> {
                isSymbolMode = !isSymbolMode
                refreshKeyboardView()
            }
            "BACKSPACE" -> {
                ic.deleteSurroundingText(1, 0)
                if (currentWord.isNotEmpty()) {
                    currentWord.deleteCharAt(currentWord.length - 1)
                }
            }
            "SPACE" -> {
                checkCurrentWord()
                ic.commitText(" ", 1)
            }
            "ENTER" -> {
                checkCurrentWord()
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            "," , "." -> {
                checkCurrentWord()
                ic.commitText(key, 1)
            }
            else -> {
                // A regular letter or symbol
                ic.commitText(key, 1)
                if (!isSymbolMode) {
                    currentWord.append(key.lowercase())
                }
                if (isShifted && !isSymbolMode) {
                    // Simple "shift is one-shot" behavior
                    isShifted = false
                    refreshKeyboardView()
                }
            }
        }
    }

    private fun refreshKeyboardView() {
        (currentInputConnection != null).let {
            setInputView(onCreateInputView())
        }
    }

    /**
     * Sends the just-finished word to the system spell checker.
     * Only misspelled words are ever saved — nothing else.
     */
    private fun checkCurrentWord() {
        val word = currentWord.toString().trim()
        currentWord.clear()

        if (!prefs.consentGiven) return
        if (word.length < 2) return
        if (word.all { it.isDigit() }) return

        pendingWord = word
        spellCheckerSession?.getSuggestions(TextInfo(word), 3)
    }

    // The word currently awaiting a spell-check result
    private var pendingWord: String? = null

    private val spellListener = object : SpellCheckerSession.SpellCheckerSessionListener {
        override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
            val word = pendingWord ?: return
            pendingWord = null
            val info = results?.firstOrNull() ?: return

            val looksLikeTypo = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
            val inDictionary = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0

            if (looksLikeTypo && !inDictionary) {
                val topSuggestion = if (info.suggestionsCount > 0) info.getSuggestionAt(0) else null
                saveMistake(word, topSuggestion)
            }
        }

        override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
            // Not used — we check word-by-word instead of full sentences.
        }
    }

    private fun saveMistake(word: String, suggestion: String?) {
        serviceScope.launch(Dispatchers.IO) {
            db.mistakeDao().insert(MistakeWord(word = word, suggestion = suggestion, timestamp = System.currentTimeMillis()))
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        currentWord.clear()
    }
}
