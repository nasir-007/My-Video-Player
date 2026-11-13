package com.example

import com.example.player.SubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun parseSrtAndApplyDelayOffset() {
        val srt = """
            1
            00:00:02,000 --> 00:00:05,000
            <i>Hello KinoX</i>

            2
            00:00:06,500 --> 00:00:09,000
            {\an8}Line one
            Line two
        """.trimIndent()

        val cues = SubtitleParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals("Hello KinoX", cues[0].text)
        assertEquals("Line one\nLine two", cues[1].text)

        // At 3000ms with 0 delay -> first cue active
        assertEquals("Hello KinoX", SubtitleParser.activeTextAt(cues, 3000L, 0L))

        // At 3000ms with +1500ms delay (cues appear later) -> targetMs = 1500ms -> null
        assertNull(SubtitleParser.activeTextAt(cues, 3000L, 1500L))

        // At 6000ms with +1500ms delay -> targetMs = 4500ms -> first cue still active
        assertEquals("Hello KinoX", SubtitleParser.activeTextAt(cues, 6000L, 1500L))
    }

    @Test
    fun parseWebVttShortTimestamps() {
        val vtt = """
            WEBVTT

            00:10.500 --> 00:14.000
            Welcome to theater mode
        """.trimIndent()

        val cues = SubtitleParser.parse(vtt)
        assertEquals(1, cues.size)
        assertEquals(10_500L, cues[0].startMs)
        assertEquals(14_000L, cues[0].endMs)
        assertEquals("Welcome to theater mode", SubtitleParser.activeTextAt(cues, 12_000L, 0L))
    }
}
