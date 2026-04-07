package com.henkenlink.crocdroid.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CrocSettings(
    val relayAddress: String = "croc.schollz.com:9009",
    val relayPassword: String = "pass123",
    val relayPorts: String = "9009,9010,9011,9012,9013",
    val curve: String = "p256",
    val hashAlgorithm: String = "xxhash",
    val disableLocal: Boolean = false,
    val forceLocal: Boolean = false,
    val multicastAddress: String = "",
    val disableMultiplexing: Boolean = false,
    val disableCompression: Boolean = false,
    val uploadThrottle: String = "",
    val overwrite: Boolean = true,
    val fixedSendCode: String = "",
    val fixedReceiveCode: String = "",
    val downloadPath: String = "",
    val themeMode: String = "system",   // "system" | "light" | "dark"
    val debugMode: Boolean = false,
    val autoZipFolders: Boolean = true,
    val noPromptReceive: Boolean = false,
    val peerIp: String = "",
    val selectedRelayConfigId: String = "default"
)
