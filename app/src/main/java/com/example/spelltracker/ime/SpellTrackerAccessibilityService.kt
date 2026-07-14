package com.example.spelltracker.ime

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import com.example.spelltracker.data.AppDatabase
import com.example.spelltracker.data.MistakeWord
import com.example.spelltracker.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads text as it's typed in any app, using any keyboard, and checks each
 * finished word against the system spell checker. Only words flagged as
 * misspelled are saved (the word + a timestamp) — the rest of what's typed
 * is never stored or sent anywhere.
 *
 * This only runs at all if the user has:
 *  1. Given consent in the app (Prefs.consentGiven), AND
 *  2. Manually turned this service on in Settings > Accessibility.
 */
class SpellTrackerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var prefs: Prefs
    private lateinit var db: AppDatabase
    private var spellCheckerSession: SpellCheckerSession? = null

    // Tracks the last known text of the field currently being typed in,
    // so we can tell what was just added.
    private var lastFieldKey: Int? = null
    private var lastFieldText: String = ""
    private var pendingWord: String? = null

    private val wordBoundaryChars = charArrayOf(' ', '\n', '\t', '.', ',', '!', '?', ';', ':')

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        db = AppDatabase.getDatabase(this)

        val tsm = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        spellCheckerSession = tsm.newSpellCheckerSession(null, null, spellListener, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        spellCheckerSession?.close()
        spellCheckerSession = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !prefs.consentGiven) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                // Switched to a different text field — reset tracking so we
                // don't compare text across unrelated fields.
                lastFieldKey = fieldKey(event)
                lastFieldText = currentText(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val key = fieldKey(event)
                val newText = currentText(event)

                if (key != lastFieldKey) {
                    // First change seen in this field — just record the baseline.
                    lastFieldKey = key
                    lastFieldText = newText
                    return
                }

                handleTextChange(lastFieldText, newText)
                lastFieldText = newText
            }
        }
    }

    override fun onInterrupt() {
        // Required override — nothing to clean up here.
    }

    private fun fieldKey(event: AccessibilityEvent): Int {
        // Identify "the same field" using the source node + the app package.
        return (event.source?.hashCode() ?: 0) * 31 + (event.packageName?.hashCode() ?: 0)
    }

    private fun currentText(event: AccessibilityEvent): String {
        return event.text?.joinToString(" ") ?: ""
    }

    /**
     * Compares old vs. new text of the same field. If a word boundary
     * (space, punctuation, newline) was just typed, check the word that
     * came right before it.
     */
    private fun handleTextChange(oldText: String, newText: String) {
        if (newText.length <= oldText.length) return // deletion / autocorrect rewrite — skip

        // Find the shared prefix so we know what was actually appended.
        var prefixLen = 0
        val minLen = minOf(oldText.length, newText.length)
        while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) prefixLen++
        val added = newText.substring(prefixLen)

        if (added.isEmpty()) return
        if (added.last() !in wordBoundaryChars) return // word isn't finished yet

        // Walk back from just before the boundary character to find the word.
        val textBeforeBoundary = newText.substring(0, newText.length - 1)
        val word = textBeforeBoundary.takeLastWhile { it !in wordBoundaryChars }.trim()

        checkWord(word)
    }

    private fun checkWord(word: String) {
        if (word.length < 2) return
        if (word.all { it.isDigit() }) return

        pendingWord = word
        spellCheckerSession?.getSuggestions(TextInfo(word), 3)
    }

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
            // Not used — words are checked individually as they're finished.
        }
    }

    private fun saveMistake(word: String, suggestion: String?) {
        serviceScope.launch(Dispatchers.IO) {
            db.mistakeDao().insert(MistakeWord(word = word, suggestion = suggestion, timestamp = System.currentTimeMillis()))
        }
    }
}
