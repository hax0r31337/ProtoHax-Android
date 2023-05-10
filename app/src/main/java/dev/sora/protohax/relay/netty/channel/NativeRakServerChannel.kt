package dev.sora.protohax.relay.netty.channel

import io.netty.channel.AbstractServerChannel
import io.netty.channel.DefaultChannelConfig
import io.netty.channel.EventLoop
import libmitm.Libmitm
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress


class NativeRakServerChannel : AbstractServerChannel() {

	private val config = DefaultChannelConfig(this)

	private var readPending = false

	override fun config() = config

	override fun isOpen(): Boolean {
		return true
	}

	override fun isActive(): Boolean {
//		return AppService.isActive
		return true
	}

	override fun isCompatible(p0: EventLoop): Boolean {
		return true
	}

	override fun localAddress0(): SocketAddress {
		return InetSocketAddress(InetAddress.getLocalHost(), 1337)
	}

	override fun doBind(p0: SocketAddress) {
	}

	override fun doClose() {
	}

	override fun doBeginRead() {
		if (readPending) return

		readPending = true
		eventLoop().execute {
			if (!readPending) {
				// We have to check readPending here because the Runnable to read could have been scheduled and later
				// during the same read loop readPending was set to false.
				return@execute
			}
			readPending = false

			val pipeline = pipeline()
			try {
				val channel = Libmitm.pollConnection()

				if (channel != null) {
					pipeline.fireChannelRead(NativeRakChannel(this, channel))
				}
			} catch (t: Throwable) {
				pipeline.fireExceptionCaught(t)
			}

			if (readPending || config().isAutoRead) {
				read()
			}
		}
	}
}
