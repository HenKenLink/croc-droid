package com.henkenlink.crocdroid.ui.relay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.henkenlink.crocdroid.data.croc.CrocEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RelayViewModel(
    private val crocEngine: CrocEngine
) : ViewModel() {

    private val _relayRunning = MutableStateFlow(false)
    val relayRunning: StateFlow<Boolean> = _relayRunning.asStateFlow()

    private var currentRelayId: String? = null

    fun startRelay(host: String, port: String, password: String) {
        if (!_relayRunning.value) {
            currentRelayId = crocEngine.startRelay(host, port, password)
            _relayRunning.value = true
        }
    }

    fun stopRelay() {
        currentRelayId?.let {
            crocEngine.stopRelay(it)
            currentRelayId = null
            _relayRunning.value = false
        }
    }

    companion object {
        fun provideFactory(
            crocEngine: CrocEngine
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RelayViewModel(crocEngine) as T
            }
        }
    }
}
