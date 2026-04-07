package com.henkenlink.crocdroid.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RelayConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relayAddress: String,
    val relayPorts: String,
    val relayPassword: String
) {
    companion object {
        val DEFAULT = RelayConfig(
            id = "default",
            name = "Default (croc)",
            relayAddress = "croc.schollz.com",
            relayPorts = "9009,9010,9011,9012,9013",
            relayPassword = "pass123"
        )
    }
}
