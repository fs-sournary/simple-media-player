package com.sournary.simplemediaplayer.client

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaControllerCompat.Callback
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat

/**
 * Helper class for a MediaBrowser that handles connecting, disconnecting and basic browsing with simplified callbacks
 */
abstract class MediaBrowserHelper<T : MediaBrowserServiceCompat>(
    private val context: Context, private val serviceClass: Class<T>
) {

    private val callbacks = mutableListOf<Callback>()

    var mediaController: MediaControllerCompat? = null
    private var mediaBrowser: MediaBrowserCompat? = null

    // Receives callbacks from the MediaController and updates the UI state.
    // Ex: which is the current item, where it is playing or paused.
    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        // Handle changes to the current metadata.
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            performAllCallbacks { onMetadataChanged(metadata) }
        }

        // Handle changes to the playback state.
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            performAllCallbacks { onPlaybackStateChanged(state) }
        }

        // Handle the session being destroyed.
        // The session is no longer valid after this call and calls to it will be ignored.
        override fun onSessionDestroyed() {
            resetState()
            onPlaybackStateChanged(null)
            onDisconnected()
        }
    }

    // Receive callbacks from the MediaBrowser when the MediaBrowserService
    private var mediaBrowserSubscriptionCallback =
        object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                this@MediaBrowserHelper.onChildrenLoaded(parentId, children)
            }
        }

    // Receive callback from MediaBrowser when it connected successfully to the MusicService.
    private var mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {

        /**
         * Invoked after MediaBrowserCompat.connect() when the request has successfully completed.
         * Also called when the service is next running after it crashed or has been killed.
         * Happens as a result of onStart().
         */
        override fun onConnected() {
            try {
                // Get a MediaController for the MediaSession
                if (mediaBrowser == null) {
                    return
                }
                mediaController = MediaControllerCompat(context, mediaBrowser!!.sessionToken).also {
                    it.registerCallback(mediaControllerCallback)
                    this@MediaBrowserHelper.onConnected(it)
                }
                mediaControllerCallback.apply {
                    onMetadataChanged(mediaController!!.metadata)
                    onPlaybackStateChanged(mediaController!!.playbackState)
                }
                mediaBrowser!!.apply { subscribe(root, mediaBrowserSubscriptionCallback) }
            } catch (e: Exception) {
                Log.d(TAG, "MediaBrowserConnectionCallback error: ${e.message ?: DEF_ERROR}")
                throw RuntimeException(e)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d(TAG, "MediaBrowserConnectionCallback failed")
        }
    }


    fun onStart() {
        if (mediaBrowser == null) {
            mediaBrowser = MediaBrowserCompat(
                context, ComponentName(context, serviceClass), mediaBrowserConnectionCallback, null
            ).apply { connect() }
            Log.d(TAG, "onStart: Creating MediaBrowser and connecting")
        }
    }

    fun onStop() {
        mediaController?.also {
            it.unregisterCallback(mediaControllerCallback)
            mediaController = null
        }
        mediaBrowser?.also {
            if (it.isConnected) {
                it.disconnect()
                mediaBrowser = null
            }
        }
        // The internet state of the app needs to revert to what it looks like when it is started before
        // any connections to the MusicService happens via the MediaSessionCompat
        resetState()
        Log.d(TAG, "Releasing MediaController and disconnecting from MediaBrowser")
    }

    private fun resetState() {
        performAllCallbacks { onPlaybackStateChanged(null) }
        Log.d(TAG, "Reset state...")
    }

    private fun performAllCallbacks(callbackCommand: MediaControllerCompat.Callback.() -> Unit) {
        callbacks.forEach { callbackCommand.invoke(it) }
    }

    /**
     * Call after connecting with a MediaBrowserServiceCompat.
     * Override to perform processing after a connection is established.
     */
    abstract fun onConnected(mediaController: MediaControllerCompat)

    /**
     * Called after loading a browser
     *
     * @param parentId: The media Id of the parent item.
     * @param children: List of child items.
     */
    abstract fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>)

    /**
     *  Called when the MediaBrowserServiceCompat connection is lost.
     */
    abstract fun onDisconnected()

    fun registerCallback(callback: MediaControllerCompat.Callback?) {
        callback?.apply {
            callbacks.add(this)
            mediaController?.also {
                it.metadata?.let { metadata -> this.onMetadataChanged(metadata) }
                it.playbackState?.let { state -> this.onPlaybackStateChanged(state) }
            }
        }
    }

    companion object {

        private const val TAG = "MediaBrowserHelper"
        private const val DEF_ERROR = "Unknown error"
    }
}
