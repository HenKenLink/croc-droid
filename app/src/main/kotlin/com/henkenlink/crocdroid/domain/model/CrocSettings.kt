package com.henkenlink.crocdroid.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CrocSettings(
    val relayAddress: String = "croc.schollz.com:9009",
    val relayPassword: String = "pass123",
    val curve: String = "p256",
    val disableLocal: Boolean = false,
    val overwrite: Boolean = true,
    val fixedSendCode: String = "",
    val fixedReceiveCode: String = "",
    val downloadPath: String = "",
)
