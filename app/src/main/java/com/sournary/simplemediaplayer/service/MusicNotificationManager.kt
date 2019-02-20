package com.sournary.simplemediaplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.sournary.simplemediaplayer.R

class MusicNotificationManager(private val service: MusicService) {

    val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_circle_outline_black_24dp,
        service.getString(R.string.play_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY)
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_circle_outline_black_24dp,
        service.getString(R.string.pause_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE)
    )
    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_black_24dp,
        service.getString(R.string.next_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private val previousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_black_24dp,
        service.getString(R.string.previous_label),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    init {
        // Cancel all previous shown notifications.
        notificationManager.cancelAll()
    }

    fun getNotification(
        state: PlaybackStateCompat, metadata: MediaMetadataCompat, token: MediaSessionCompat.Token
    ): Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val description = metadata.description
        return buildNotification(state, token, isPlaying, description).build()
    }

    private fun buildNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        description: MediaDescriptionCompat
    ): NotificationCompat.Builder {
        // Begin Android O, Each notification must have belong to a channel or will be not active.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val mediaStyle = MediaStyle().setMediaSession(token)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true) // For Android L and earlier
        val deletePendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            service, PlaybackStateCompat.ACTION_STOP
        )
        val notificationBuilder = NotificationCompat.Builder(service, CHANNEL_ID)
        notificationBuilder.setSmallIcon(R.drawable.ic_music_note_black_24dp)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setOngoing(true)
            .setLargeIcon(MusicLibrary.getAlbumBitmap(service, description.mediaId ?: ""))
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
        // If skip to next button is enabled.
        if ((state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L) {
            notificationBuilder.addAction(previousAction)
        }
        notificationBuilder.addAction(if (isPlaying) pauseAction else playAction)
        // If skip to prev button is enable
        if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            notificationBuilder.addAction(nextAction)
        }
        return notificationBuilder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "MediaSession" // The visible name of the channel
            val description = "MediaSession and MediaPlayer" // The description of the channel
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun onDestroy() {
        Log.d(TAG, "MusicNotificationManager: onDestroy")
    }

    companion object {

        private const val TAG = "NotificationManager"
        private const val CHANNEL_ID = "com.sournary.simplemediaplayer.channel"
        const val NOTIFICATION_ID = 412
    }
}
