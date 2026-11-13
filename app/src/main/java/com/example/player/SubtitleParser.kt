package com.example.player

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object SubtitleParser {

    private val timeLineRegex = Regex(
        """((?:\d{1,2}:)?\d{2}:\d{2}[,.]\d{3})\s*-->\s*((?:\d{1,2}:)?\d{2}:\d{2}[,.]\d{3})"""
    )
    private val htmlTagRegex = Regex("""<[^>]+>""")
    private val assTagRegex = Regex("""\{\\[^}]*\}""")

    suspend fun loadFromUri(context: Context, uriString: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val rawText = runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

        if (rawText.isBlank()) emptyList() else parse(rawText)
    }

    fun parse(rawContent: String): List<SubtitleCue> {
        val normalized = rawContent
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val lines = normalized.split('\n')
        val cues = mutableListOf<SubtitleCue>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            val match = timeLineRegex.find(line)
            if (match != null) {
                val startMs = parseTimestampMs(match.groupValues[1])
                val endMs = parseTimestampMs(match.groupValues[2])
                i++
                val textLines = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank()) {
                    val cleaned = cleanCueLine(lines[i])
                    if (cleaned.isNotEmpty()) {
                        textLines += cleaned
                    }
                    i++
                }
                if (textLines.isNotEmpty() && endMs > startMs) {
                    cues += SubtitleCue(
                        startMs = startMs,
                        endMs = endMs,
                        text = textLines.joinToString("\n")
                    )
                }
            } else {
                i++
            }
        }
        return cues.sortedBy { it.startMs }
    }

    fun activeTextAt(cues: List<SubtitleCue>, positionMs: Long, delayMs: Long): String? {
        if (cues.isEmpty()) return null
        val targetMs = positionMs - delayMs
        val active = cues.filter { targetMs in it.startMs..it.endMs }
        return if (active.isEmpty()) null else active.joinToString("\n") { it.text }
    }

    private fun cleanCueLine(raw: String): String {
        return raw
            .replace(assTagRegex, "")
            .replace(htmlTagRegex, "")
            .trim()
    }

    private fun parseTimestampMs(timestamp: String): Long {
        val clean = timestamp.trim().replace(',', '.')
        val parts = clean.split(':')
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val secParts = parts[2].split('.')
                val seconds = secParts.getOrNull(0)?.toLongOrNull() ?: 0L
                val millis = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
                hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
            }
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val secParts = parts[1].split('.')
                val seconds = secParts.getOrNull(0)?.toLongOrNull() ?: 0L
                val millis = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
                minutes * 60_000L + seconds * 1_000L + millis
            }
            else -> 0L
        }
    }
}
