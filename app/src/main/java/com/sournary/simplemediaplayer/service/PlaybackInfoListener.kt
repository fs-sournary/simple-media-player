package com.sournary.simplemediaplayer.service

import android.support.v4.media.session.PlaybackStateCompat

interface PlaybackInfoListener {

    fun onPlaybackStateChange(state: PlaybackStateCompat)

    fun onPlaybackComplete()
}
