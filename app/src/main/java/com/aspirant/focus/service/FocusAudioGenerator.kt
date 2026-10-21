package com.aspirant.focus.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.aspirant.focus.data.model.FocusAudioType
import kotlinx.coroutines.*
import java.util.Random
import kotlin.math.sin

class FocusAudioGenerator {

    private var audioTrack: AudioTrack? = null
    private var generatorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    var currentAudioType: FocusAudioType = FocusAudioType.NONE
        private set

    fun play(type: FocusAudioType) {
        stop()
        if (type == FocusAudioType.NONE) return
        currentAudioType = type

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        generatorJob = scope.launch {
            val random = Random()
            val buffer = ShortArray(bufferSize / 2)
            var lastBrownLeft = 0.0
            var lastBrownRight = 0.0
            var phaseLeft = 0.0
            var phaseRight = 0.0

            while (isActive) {
                for (i in buffer.indices step 2) {
                    when (type) {
                        FocusAudioType.WHITE_NOISE -> {
                            val sample = ((random.nextDouble() * 2.0 - 1.0) * 0.15 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i] = sample
                            buffer[i + 1] = sample
                        }
                        FocusAudioType.BROWN_NOISE -> {
                            val whiteLeft = random.nextDouble() * 2.0 - 1.0
                            val whiteRight = random.nextDouble() * 2.0 - 1.0
                            lastBrownLeft = (lastBrownLeft + (0.02 * whiteLeft)) / 1.02
                            lastBrownRight = (lastBrownRight + (0.02 * whiteRight)) / 1.02
                            buffer[i] = (lastBrownLeft * 0.8 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i + 1] = (lastBrownRight * 0.8 * Short.MAX_VALUE).toInt().toShort()
                        }
                        FocusAudioType.BINAURAL_ALPHA -> {
                            // 200Hz Left, 210Hz Right -> 10Hz Alpha beat
                            val freqL = 200.0
                            val freqR = 210.0
                            val sampleL = (sin(phaseLeft) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                            val sampleR = (sin(phaseRight) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i] = sampleL
                            buffer[i + 1] = sampleR
                            phaseLeft += 2.0 * Math.PI * freqL / sampleRate
                            phaseRight += 2.0 * Math.PI * freqR / sampleRate
                        }
                        FocusAudioType.BINAURAL_BETA -> {
                            // 220Hz Left, 240Hz Right -> 20Hz Beta beat
                            val freqL = 220.0
                            val freqR = 240.0
                            val sampleL = (sin(phaseLeft) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                            val sampleR = (sin(phaseRight) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                            buffer[i] = sampleL
                            buffer[i + 1] = sampleR
                            phaseLeft += 2.0 * Math.PI * freqL / sampleRate
                            phaseRight += 2.0 * Math.PI * freqR / sampleRate
                        }
                        FocusAudioType.NONE -> {
                            buffer[i] = 0
                            buffer[i + 1] = 0
                        }
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        generatorJob?.cancel()
        generatorJob = null
        currentAudioType = FocusAudioType.NONE
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    companion object {
        val instance by lazy { FocusAudioGenerator() }
    }
}
