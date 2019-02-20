package com.sournary.simplemediaplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat


/**
 * Abstract player implementation that handles playing music with proper handling of headphones and audio focus.
 */
private const val MEDIA_VOLUME_DEFAULT = 1.0f
private const val MEDIA_VOLUME_DUCK = 0.2f

abstract class PlayerAdapter(context: Context) {

    private var audioReceiverRegistered = false
    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action && isPlaying()) {
                pause()
            }
        }
    }
    private val applicationContext = context.applicationContext
    private val audioManager =
        applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusHelper = AudioFocusHelper()
    private var playOnAudioFocus = false

    fun play() {
        if (audioFocusHelper.requestAudioFocus()) {
            registerAudioNoisyReceiver()
            onPlay()
        }
    }

    abstract fun onPlay()

    private fun registerAudioNoisyReceiver() {
        if (!audioReceiverRegistered) {
            val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            applicationContext.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter)
            audioReceiverRegistered = true
        }
    }

    fun pause() {
        if (!playOnAudioFocus) {
            audioFocusHelper.abandonAudioFocus()
        }
        unregisterAudioNoisyReceiver()
        onPause()
    }

    abstract fun onPause()

    private fun unregisterAudioNoisyReceiver() {
        if (audioReceiverRegistered) {
            applicationContext.unregisterReceiver(audioNoisyReceiver)
            audioReceiverRegistered = false
        }
    }

    fun stop() {
        audioFocusHelper.abandonAudioFocus()
        unregisterAudioNoisyReceiver()
        onStop()
    }

    abstract fun onStop()

    abstract fun seekTo(position: Long)

    abstract fun setVolume(volume: Float)

    abstract fun playFromMedia(metadata: MediaMetadataCompat)

    abstract fun getCurrentMedia(): MediaMetadataCompat?

    abstract fun isPlaying(): Boolean

    /**
     * Helper class for managing audio focus related tasks.
     */
    private inner class AudioFocusHelper : AudioManager.OnAudioFocusChangeListener {

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playOnAudioFocus && !isPlaying()) {
                        play()
                    } else if (isPlaying()) {
                        setVolume(MEDIA_VOLUME_DEFAULT)
                    }
                    playOnAudioFocus = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    setVolume(MEDIA_VOLUME_DUCK)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isPlaying()) {
                        playOnAudioFocus = true
                        pause()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    audioManager.abandonAudioFocus(this)
                    playOnAudioFocus = false
                    stop()
                }
            }
        }

        fun requestAudioFocus(): Boolean {
            val result = audioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun abandonAudioFocus() {
            audioManager.abandonAudioFocus(this)
        }
    }
}
