package dev.sora.protohax.relay.service

interface ServiceListener {

    fun onServiceStarted()

    fun onServiceStopped()
}