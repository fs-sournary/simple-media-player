package com.sournary.simplemediaplayer.service

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import java.io.FileNotFoundException
import java.io.IOException

class MusicPlayerAdapter(context: Context, private val listener: PlaybackInfoListener) :
    PlayerAdapter(context) {

    private var fileName: String? = null
    private var state: Int? = null
    // Work-around for a MediaPlayer bug related to the behavior of MediaPlayer.seekTo() while not playing
    private var seekWhileNotPlaying = -1

    var mediaPlayer: MediaPlayer? = null
    private var currentMedia: MediaMetadataCompat? = null
    private var currentMediaPlayedToCompletion: Boolean? = null
    private val applicationContext = context.applicationContext

    /**
     * Once the MediaPlayer is released, it can't be used again and another one has to be created.
     * Method onStop() of MainActivity, MediaPlayer is released.
     * Method onStart() of MainActivity
     */
    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    listener.onPlaybackComplete()
                    setNewState(PlaybackStateCompat.STATE_PAUSED)
                }
            }
        }
    }

    private fun setNewState(@PlaybackStateCompat.State newPlayerState: Int) {
        state = newPlayerState
        if (state == PlaybackStateCompat.STATE_STOPPED) {
            currentMediaPlayedToCompletion = true
        }
        val reportPosition: Long
        if (seekWhileNotPlaying >= 0) {
            reportPosition = seekWhileNotPlaying.toLong()
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                seekWhileNotPlaying = -1
            }
        } else {
            reportPosition = mediaPlayer?.currentPosition?.toLong() ?: 0
        }
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            setActions(getAvailableActions())
            state?.let { setState(it, reportPosition, 1.0F, SystemClock.elapsedRealtime()) }
        }
        listener.onPlaybackStateChange(stateBuilder.build())
    }

    private fun getAvailableActions(): Long {
        var actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        when (state) {
            PlaybackStateCompat.STATE_STOPPED -> {
                actions = actions or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                actions = actions or PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                actions = actions or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP
            }
            else -> {
                actions = actions or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE
            }
        }
        return actions
    }

    override fun onPlay() {
        mediaPlayer?.apply {
            if (isPlaying.not()) {
                start()
                setNewState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    override fun onPause() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
                setNewState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    override fun onStop() {
        setNewState(PlaybackStateCompat.STATE_STOPPED)
        release()
    }

    private fun release() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    override fun seekTo(position: Long) {
        mediaPlayer?.apply {
            if (isPlaying.not()) {
                seekWhileNotPlaying = position.toInt()
            }
            seekTo(position.toInt())
            // Set the new state (to the current state) because the position changed and should be reported to clients.
            state?.let { setNewState(it) }
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.apply { setVolume(volume, volume) }
    }

    // Implements PlaybackControls
    override fun playFromMedia(metadata: MediaMetadataCompat) {
        currentMedia = metadata
        metadata.description.mediaId?.let { playFile(MusicLibrary.getMusicFileName(it)) }
    }

    private fun playFile(newFileName: String) {
        var mediaChanged = newFileName != fileName
        currentMediaPlayedToCompletion?.apply {
            if (this) {
                mediaChanged = true
                currentMediaPlayedToCompletion = false
            }
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play()
            }
            return
        } else {
            release()
        }
        // Play new media file
        fileName = newFileName
        initializeMediaPlayer()
        try {
            applicationContext.assets.openFd(newFileName).apply {
                mediaPlayer?.setDataSource(fileDescriptor, startOffset, length)
            }
        } catch (e: FileNotFoundException) {
            throw RuntimeException("Failed to open file $fileName: ${e.message}")
        }
        try {
            mediaPlayer?.prepare()
        } catch (e: Exception) {
            when (e) {
                is IOException -> throw IOException("Failed to open file $fileName: ${e.message}")
                is IllegalStateException -> throw IllegalStateException("Failed to play in $state state: ${e.message}")
            }
        }
        play()
    }

    override fun getCurrentMedia(): MediaMetadataCompat? = currentMedia

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    companion object {

        private const val TAG = "MusicPlayerAdapter"
    }
}
