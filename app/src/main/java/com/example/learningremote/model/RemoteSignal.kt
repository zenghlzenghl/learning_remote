package com.example.learningremote.model

data class RemoteSignal(
    val id: Int,
    val type: String,
    val remoteId: String,
    val buttonName: String,
    val dataLen: Int
)