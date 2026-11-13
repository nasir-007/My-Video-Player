package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.example.data.model.DecoderMode

object DecoderFactory {

    @OptIn(UnstableApi::class)
    fun createRenderersFactory(context: Context, mode: DecoderMode): DefaultRenderersFactory {
        val codecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val available = MediaCodecUtil.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            orderCodecsForMode(available, mode)
        }

        val factory = DefaultRenderersFactory(context)
            .setMediaCodecSelector(codecSelector)

        return when (mode) {
            DecoderMode.HW -> {
                factory
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                    .setEnableDecoderFallback(false)
                    .forceEnableMediaCodecAsynchronousQueueing()
            }
            DecoderMode.HW_PLUS -> {
                factory
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    .setEnableDecoderFallback(true)
                    .forceEnableMediaCodecAsynchronousQueueing()
            }
            DecoderMode.SW -> {
                factory
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun orderCodecsForMode(
        codecs: List<MediaCodecInfo>,
        mode: DecoderMode
    ): List<MediaCodecInfo> {
        if (codecs.isEmpty()) return codecs
        return when (mode) {
            DecoderMode.HW -> {
                val hardwareOnly = codecs.filter { it.hardwareAccelerated }
                hardwareOnly.ifEmpty { codecs }
            }
            DecoderMode.HW_PLUS -> {
                codecs.sortedByDescending { it.hardwareAccelerated }
            }
            DecoderMode.SW -> {
                val softwareFirst = codecs.sortedBy { it.hardwareAccelerated }
                softwareFirst
            }
        }
    }
}
