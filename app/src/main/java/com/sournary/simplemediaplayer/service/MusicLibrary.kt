package com.sournary.simplemediaplayer.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.sournary.simplemediaplayer.BuildConfig
import com.sournary.simplemediaplayer.R
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

object MusicLibrary {

    private val musics = TreeMap<String, MediaMetadataCompat>()
    private val albumRes = HashMap<String, Int>()
    private val musicFileNames = HashMap<String, String>()

    init {
        createMediaMetadataCompat(
            "Jazz_In_Paris",
            "Jazz in Paris",
            "Media Right Productions",
            "Jazz & Blues",
            "Jazz",
            103,
            TimeUnit.SECONDS,
            "jazz_in_paris.mp3",
            R.drawable.album_jazz_blues,
            "album_jazz_blues"
        )
        createMediaMetadataCompat(
            "The_Coldest_Shoulder",
            "The Coldest Shoulder",
            "The 126ers",
            "Youtube Audio Library Rock 2",
            "Rock",
            160,
            TimeUnit.SECONDS,
            "the_coldest_shoulder.mp3",
            R.drawable.album_youtube_audio_library_rock_2,
            "album_youtube_audio_library_rock_2"
        )
    }

    private fun createMediaMetadataCompat(
        mediaId: String, title: String, artist: String,
        album: String, genre: String, duration: Long, durationUnit: TimeUnit,
        musicFileName: String, albumArtistResId: Int, albumArtistResName: String
    ) {
        val mediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                TimeUnit.MILLISECONDS.convert(duration, durationUnit)
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                getAlbumArtUri(
                    albumArtistResName
                )
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                getAlbumArtUri(
                    albumArtistResName
                )
            )
            .build()
        musics[mediaId] = mediaMetadataCompat
        musicFileNames[mediaId] = musicFileName
        albumRes[mediaId] = albumArtistResId
    }

    fun getMetadataCompat(context: Context, mediaId: String): MediaMetadataCompat? {
        return musics[mediaId]?.let { metadataWithoutBitmap ->
            val albumArt =
                getAlbumBitmap(context, mediaId)
            // Because MediaMetadataCompat is immutable, we need to create a copy to set the album art.
            // We don't set it initially on all items so that they don't take unnecessary memory.
            val builder = MediaMetadataCompat.Builder()
            arrayOf(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                MediaMetadataCompat.METADATA_KEY_GENRE,
                MediaMetadataCompat.METADATA_KEY_TITLE
            ).forEach { builder.putString(it, metadataWithoutBitmap.getString(it)) }
            builder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                metadataWithoutBitmap.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            builder.build()
        }
    }

    fun getRoot(): String = "root"

    private fun getAlbumArtUri(albumArtResName: String): String =
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/drawable/$albumArtResName"

    fun getMusicFileName(mediaId: String): String {
        return if (musicFileNames.containsKey(mediaId)) {
            musicFileNames[mediaId] ?: ""
        } else {
            ""
        }
    }

    fun getAlbumBitmap(context: Context, musicId: String): Bitmap =
        BitmapFactory.decodeResource(context.resources,
            getAlbumRes(musicId)
        )

    private fun getAlbumRes(mediaId: String): Int {
        return if (albumRes.containsKey(mediaId)) {
            albumRes[mediaId] ?: 0
        } else {
            0
        }
    }

    fun getMediaItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val result = mutableListOf<MediaBrowserCompat.MediaItem>()
        musics.values.forEach {
            val mediaItem = MediaBrowserCompat.MediaItem(
                it.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            result.add(mediaItem)
        }
        return result
    }
}
