package com.example.data.model

enum class DecoderMode(val shortLabel: String, val fullName: String, val summary: String) {
    HW(
        shortLabel = "HW",
        fullName = "Hardware",
        summary = "Strict MediaCodec hardware pipeline on SurfaceView for lowest battery draw."
    ),
    HW_PLUS(
        shortLabel = "HW+",
        fullName = "Hardware+",
        summary = "Hardware decoding with async buffering, automatic fallback, and zoom/crop transforms."
    ),
    SW(
        shortLabel = "SW",
        fullName = "Software",
        summary = "Software codec priority for tricky MKV/AVI files or hardware-incompatible profiles."
    );

    fun next(): DecoderMode = when (this) {
        HW -> HW_PLUS
        HW_PLUS -> SW
        SW -> HW
    }

    companion object {
        fun fromName(value: String?): DecoderMode =
            entries.firstOrNull { it.name == value } ?: HW_PLUS
    }
}

enum class PowerProfile(
    val label: String,
    val tagline: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val tickerIntervalMs: Long,
    val preferredDecoder: DecoderMode
) {
    ECO(
        label = "Eco Saver",
        tagline = "Strict HW zero-copy decoding, compact buffers & 250ms low-wakeup UI polling",
        minBufferMs = 8_000,
        maxBufferMs = 18_000,
        tickerIntervalMs = 250L,
        preferredDecoder = DecoderMode.HW
    ),
    BALANCED(
        label = "Balanced",
        tagline = "Smart HW+ decoding with adaptive buffering and low idle drain",
        minBufferMs = 15_000,
        maxBufferMs = 35_000,
        tickerIntervalMs = 100L,
        preferredDecoder = DecoderMode.HW_PLUS
    ),
    CINEMA(
        label = "Cinema Max",
        tagline = "Extended pre-buffering and 65ms ultra-smooth timeline & subtitle sync",
        minBufferMs = 25_000,
        maxBufferMs = 60_000,
        tickerIntervalMs = 65L,
        preferredDecoder = DecoderMode.HW_PLUS
    );

    companion object {
        fun fromName(value: String?): PowerProfile =
            entries.firstOrNull { it.name == value } ?: BALANCED
    }
}

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Off", 0),
    MIN_15("15 min", 15),
    MIN_30("30 min", 30),
    MIN_45("45 min", 45),
    MIN_60("60 min", 60),
    END_OF_VIDEO("End of Video", -1)
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Crop"),
    STRETCH("Stretch"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3");

    fun next(): AspectRatioMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(value: String?): AspectRatioMode =
            entries.firstOrNull { it.name == value } ?: FIT
    }
}

enum class OrientationMode(val label: String) {
    AUTO("Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait");

    fun next(): OrientationMode = entries[(ordinal + 1) % entries.size]
}

data class SubtitleStylePrefs(
    val fontSizeSp: Float = 20f,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val backgroundOpacity: Float = 0.55f,
    val hasOutline: Boolean = true,
    val bottomMarginPercent: Float = 9f
)

data class PlaybackPrefs(
    val defaultDecoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val powerProfile: PowerProfile = PowerProfile.BALANCED,
    val backgroundPlayEnabled: Boolean = true,
    val autoEcoOnBatterySaver: Boolean = true,
    val seekStepSeconds: Int = 10,
    val autoResume: Boolean = true,
    val subtitleStyle: SubtitleStylePrefs = SubtitleStylePrefs()
)

data class MediaTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val id: String,
    val label: String,
    val language: String,
    val mimeType: String,
    val bitrate: Int,
    val channelCount: Int = 0,
    val isSelected: Boolean
)

data class CodecTelemetry(
    val videoCodecName: String = "Probing...",
    val isHardwareAccelerated: Boolean? = null,
    val mimeType: String = "—",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val droppedFrames: Int = 0,
    val audioCodecName: String = "—",
    val audioChannels: Int = 0,
    val audioSampleRateHz: Int = 0,
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS
)

data class VideoItem(
    val uriString: String,
    val title: String,
    val folderUri: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val externalSubtitleUri: String? = null,
    val subtitleDelayMs: Long = 0L,
    val aspectMode: AspectRatioMode = AspectRatioMode.FIT
)

data class FolderItem(
    val treeUriString: String,
    val displayName: String,
    val videoCount: Int = 0,
    val addedAt: Long = 0L
)
