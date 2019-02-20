package com.sournary.simplemediaplayer.widget

import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class MediaSeekBar : AppCompatSeekBar {

    private var isTracking = false

    private var mediaController: MediaControllerCompat? = null
    private var progressAnimator: ValueAnimator? = null
    private var controllerCallback: ControllerCallback? = null

    /**
     * Notice when progress level has been changed.
     */
    private val onSeekBarChangeListener = object : OnSeekBarChangeListener {
        // Notice when the progress has been changed
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            // val  duration = ((max - progress) / playbackSpeed).toLong()
        }

        // Notice when the user has started a touch gesture.
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            isTracking = true
        }

        // Notice when the user has finished a touch gesture.
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            mediaController?.transportControls?.seekTo(progress.toLong())
            isTracking = false
        }
    }

    constructor(context: Context) : super(context) {
        super.setOnSeekBarChangeListener(onSeekBarChangeListener)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        super.setOnSeekBarChangeListener(onSeekBarChangeListener)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        super.setOnSeekBarChangeListener(onSeekBarChangeListener)
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        // Prohibit adding seek listener to this subclass.
        throw UnsupportedOperationException("Can\'t add listener to a MediaSeekBar !")
    }

    fun setMediaController(controller: MediaControllerCompat?) {
        if (controller != null) {
            controllerCallback = ControllerCallback().apply {
                controller.registerCallback(this)
            }
        } else if (mediaController != null && controllerCallback != null) {
            mediaController!!.unregisterCallback(controllerCallback!!)
            controllerCallback = null
        }
        mediaController = controller
    }

    fun disconnectMediaController() {
        if (mediaController != null) {
            if (controllerCallback != null) {
                mediaController!!.unregisterCallback(controllerCallback!!)
                controllerCallback = null
            }
            mediaController = null
        }
    }

    /**
     * Receiving updates from the session
     */
    private inner class ControllerCallback :
        MediaControllerCompat.Callback(), ValueAnimator.AnimatorUpdateListener {

        /**
         * Handle changes in playback state.
         */
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            // If there is an ongoing animation, stop it now.
            progressAnimator?.also {
                it.cancel()
                progressAnimator = null
            }
            progress = state?.position?.toInt() ?: 0
            // if the media is playing, SeekBar should follow it.
            // The easiest way to do that is create a ValueAnimator to update it.
            state?.apply {
                if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                    progressAnimator = ValueAnimator.ofInt(progress, max).apply {
                        duration = ((max - progress) / playbackSpeed).toLong()
                        interpolator = LinearInterpolator()
                        addUpdateListener(this@ControllerCallback)
                        start()
                    }
                }
            }
        }

        /**
         * Handle changes to the new metadata
         */
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            val max = metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)?.toInt() ?: 0
            progress = 0
            setMax(max)
        }

        override fun onAnimationUpdate(animation: ValueAnimator?) {
            // If the user is changing the slider, cancel the animation.
            animation?.apply {
                if (isTracking) {
                    cancel()
                    return
                }
                progress = animation.animatedValue as Int
            }
        }
    }
}
