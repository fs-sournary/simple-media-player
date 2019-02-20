package com.sournary.simplemediaplayer.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sournary.simplemediaplayer.BR
import com.sournary.simplemediaplayer.R
import com.sournary.simplemediaplayer.client.MediaBrowserHelper
import com.sournary.simplemediaplayer.databinding.FragmentHomeBinding
import com.sournary.simplemediaplayer.service.MusicLibrary
import com.sournary.simplemediaplayer.service.MusicService
import com.sournary.simplemediaplayer.util.toTimeString
import kotlinx.android.synthetic.main.fragment_home.*

/**
 * Create on 3/10/19 by Sang
 * Description:
 **/
class HomeFragment : Fragment() {

    private var isPlaying = false

    private lateinit var mediaBrowserHelper: MediaBrowserHelper<MusicService>

    private val viewModel: HomeViewModel by lazy {
        ViewModelProviders.of(this).get(HomeViewModel::class.java)
    }
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            setVariable(BR.viewModel, this@HomeFragment.viewModel)
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupClient()
        setupViewModel()
    }

    private fun setupClient() {
        // Callback
        val callback = object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                showPlayMediaState(state)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                showMetadataChange(metadata)
            }
        }
        // Implement MediaBrowserHelper
        mediaBrowserHelper =
            object : MediaBrowserHelper<MusicService>(context!!, MusicService::class.java) {
                override fun onConnected(mediaController: MediaControllerCompat) {
                    seekBar.setMediaController(mediaController)
                }

                override fun onChildrenLoaded(
                    parentId: String, children: List<MediaBrowserCompat.MediaItem>
                ) {
                    mediaController?.apply {
                        children.forEach { addQueueItem(it.description) }
                        transportControls.prepare()
                    }
                }

                override fun onDisconnected() {}
            }.apply { registerCallback(callback) }
    }

    private fun showPlayMediaState(state: PlaybackStateCompat?) {
        isPlaying = state?.state == PlaybackStateCompat.STATE_PLAYING
        if (isPlaying) {
            playImageView.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp)
        } else {
            playImageView.setImageResource(R.drawable.ic_play_circle_outline_black_24dp)
        }
    }

    private fun showMetadataChange(metadata: MediaMetadataCompat?) {
        metadata?.apply {
            val timeLong = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            durationText.text = timeLong.toTimeString("mm:ss")
            titleMusicTextView.text = getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            artistTextView.text = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            val thumbnailImage = MusicLibrary.getAlbumBitmap(
                context = context ?: return,
                musicId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            )
            thumbnailImageView.setImageBitmap(thumbnailImage)
        }
    }

    private fun setupViewModel() {
        viewModel.apply {
            // Event
            playPreviousMediaEvent.observe(viewLifecycleOwner, Observer {
                mediaBrowserHelper.mediaController?.transportControls?.skipToPrevious()
            })
            playNextMediaEvent.observe(viewLifecycleOwner, Observer {
                mediaBrowserHelper.mediaController?.transportControls?.skipToNext()
            })
            playPauseMediaEvent.observe(viewLifecycleOwner, Observer {
                if (isPlaying) {
                    mediaBrowserHelper.mediaController?.transportControls?.pause()
                } else {
                    mediaBrowserHelper.mediaController?.transportControls?.play()
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserHelper.onStart()
    }

    override fun onStop() {
        super.onStop()
        seekBar.disconnectMediaController()
        mediaBrowserHelper.onStop()
    }

    companion object {

        fun newInstance() = HomeFragment()
    }
}
