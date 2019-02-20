package com.sournary.simplemediaplayer.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.sournary.simplemediaplayer.util.SingleLiveEvent

/**
 * Create on 3/10/19 by Sang
 * Description:
 **/
class HomeViewModel : ViewModel() {

    private val _playPreviousMediaEvent = SingleLiveEvent<Any>()
    private val _playNextMediaEvent = SingleLiveEvent<Any>()
    private val _playPauseMediaEvent = SingleLiveEvent<Any>()
    //--+--//
    val playPreviousMediaEvent: LiveData<Any> = _playPreviousMediaEvent
    val playNextMediaEvent: LiveData<Any> = _playNextMediaEvent
    val playPauseMediaEvent: LiveData<Any> = _playPauseMediaEvent

    fun playPreviousMedia() {
        _playPreviousMediaEvent.call()
    }

    fun playNextMedia() {
        _playNextMediaEvent.call()
    }

    fun playOrPauseMedia() {
        _playPauseMediaEvent.call()
    }
}
