package com.sournary.simplemediaplayer.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat

class MusicService : MediaBrowserServiceCompat() {

    private var serviceInStartedState = false

    private lateinit var musicSession: MediaSessionCompat
    private lateinit var playback: MusicPlayerAdapter
    private lateinit var musicNotificationManager: MusicNotificationManager
    private lateinit var sessionCallback: MediaSessionCallback

    override fun onCreate() {
        super.onCreate()
        musicSession = MediaSessionCompat(this, TAG)
        musicSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        sessionCallback = MediaSessionCallback()
        musicSession.setCallback(sessionCallback)
        sessionToken = musicSession.sessionToken

        playback = MusicPlayerAdapter(this, MediaPlayerListener())
        musicNotificationManager = MusicNotificationManager(this)
        Log.d(TAG, "MusicService: onCreate...")
    }

    override fun onLoadChildren(
        parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(MusicLibrary.getMediaItems())
    }

    override fun onGetRoot(
        clientPackageName: String, clientUid: Int, rootHints: Bundle?
    ): BrowserRoot? = BrowserRoot(MusicLibrary.getRoot(), null)

    /**
     * Called if the service is currently running and the user has removed a task that comes from the service's application.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        musicNotificationManager.onDestroy()
        playback.onStop()
        musicSession.release()
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released")
    }

    fun getMediaPosition(): Int = playback.mediaPlayer?.currentPosition ?: 0

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        private var queueIndex = -1

        private var prepareMedia: MediaMetadataCompat? = null
        private val playlist = mutableListOf<MediaSessionCompat.QueueItem>()

        /**
         * MediaSessionCompat want to add a MediaSessionCompat.QueueItem with given description at the end of the play queue
         */
        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            val addedItem =
                MediaSessionCompat.QueueItem(description, description.hashCode().toLong())
            playlist.add(addedItem)
            queueIndex = if (queueIndex == -1) {
                0
            } else {
                queueIndex
            }
            musicSession.setQueue(playlist)
        }

        /**
         * MediaSessionCompat want to remove the first MediaSessionCompat.QueueItem in the play queue
         */
        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            val removedItem =
                MediaSessionCompat.QueueItem(description, description.hashCode().toLong())
            playlist.remove(removedItem)
            queueIndex = if (playlist.isEmpty()) {
                -1
            } else {
                queueIndex
            }
            musicSession.setQueue(playlist)
        }

        // Handle requests to prepare playback
        override fun onPrepare() {
            if (queueIndex < 0 && playlist.isEmpty()) {
                return
            }
            val mediaId = playlist[queueIndex].description.mediaId
            prepareMedia =
                MusicLibrary.getMetadataCompat(this@MusicService, mediaId ?: "") ?: return
            musicSession.setMetadata(prepareMedia)
            if (!musicSession.isActive) {
                musicSession.isActive = true
            }
        }

        override fun onPlay() {
            if (playlist.isEmpty()) {
                return
            }
            if (prepareMedia == null) {
                onPrepare()
            }
            prepareMedia?.let { playback.playFromMedia(it) }
        }

        override fun onPause() {
            playback.pause()
        }

        override fun onStop() {
            playback.onStop()
            musicSession.isActive = false
        }

        override fun onSkipToNext() {
            queueIndex = ++queueIndex % playlist.size
            prepareMedia = null
            onPlay()
        }

        override fun onSkipToPrevious() {
            queueIndex = if (queueIndex > 0) {
                queueIndex - 1
            } else {
                playlist.size - 1
            }
            prepareMedia = null
            onPlay()
        }

        override fun onSeekTo(pos: Long) {
            playback.seekTo(pos)
        }
    }

    private inner class MediaPlayerListener : PlaybackInfoListener {

        override fun onPlaybackStateChange(state: PlaybackStateCompat) {
            musicSession.setPlaybackState(state)
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> moveServiceToStartedState(state)
                PlaybackStateCompat.STATE_PAUSED -> updateNotificationForPause(state)
                PlaybackStateCompat.STATE_STOPPED -> moveServiceOutOfStartedState()
            }
        }

        override fun onPlaybackComplete() {}

        private fun moveServiceToStartedState(state: PlaybackStateCompat) {
            if (serviceInStartedState.not()) {
                ContextCompat.startForegroundService(
                    this@MusicService,
                    Intent(this@MusicService, MusicService::class.java)
                )
                serviceInStartedState = true
            }
            val currentMedia = playback.getCurrentMedia() ?: return
            val currentSession = sessionToken ?: return
            val notification = musicNotificationManager.getNotification(
                state, currentMedia, currentSession
            )
            startForeground(MusicNotificationManager.NOTIFICATION_ID, notification)
        }

        private fun updateNotificationForPause(state: PlaybackStateCompat) {
            stopForeground(false)
            val currentMedia = playback.getCurrentMedia() ?: return
            val currentSession = sessionToken ?: return
            val notification = musicNotificationManager.getNotification(
                state, currentMedia, currentSession
            )
            musicNotificationManager.notificationManager.notify(
                MusicNotificationManager.NOTIFICATION_ID, notification
            )
        }

        private fun moveServiceOutOfStartedState() {
            stopForeground(true)
            stopSelf()
            serviceInStartedState = false
        }
    }

    companion object {

        private const val TAG = "MusicService"
    }
}
