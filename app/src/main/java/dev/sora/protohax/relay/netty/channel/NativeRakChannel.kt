package dev.sora.protohax.relay.netty.channel

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.AbstractChannel
import io.netty.channel.Channel
import io.netty.channel.ChannelConfig
import io.netty.channel.ChannelMetadata
import io.netty.channel.ChannelOutboundBuffer
import io.netty.channel.ChannelPromise
import io.netty.channel.DefaultEventLoop
import io.netty.channel.EventLoop
import io.netty.util.internal.StringUtil
import libmitm.RakConn
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage
import java.net.InetSocketAddress
import java.net.SocketAddress


class NativeRakChannel(parent: Channel, private val rakConn: RakConn) : AbstractChannel(parent) {

	private val metadata = ChannelMetadata(false)
	private val config = NativeRakConfig(this).also {
		it.protocolVersion = rakConn.version.toInt()
		it.targetAddress = InetSocketAddress(rakConn.localAddr, rakConn.localPort.toInt())
	}

	private var readPending = false
	private val eventLoopRead = DefaultEventLoop()

	override fun config(): ChannelConfig {
		return config
	}

	override fun isOpen(): Boolean {
		return rakConn.isOpen
	}

	override fun isActive(): Boolean {
		return rakConn.isOpen
	}

	override fun metadata(): ChannelMetadata {
		return metadata
	}

	override fun newUnsafe(): AbstractUnsafe {
		return Unsafe()
	}

	override fun isCompatible(p0: EventLoop): Boolean {
		return true
	}

	override fun localAddress0(): SocketAddress {
		return InetSocketAddress(rakConn.localAddr, rakConn.localPort.toInt())
	}

	override fun remoteAddress0(): SocketAddress {
		return InetSocketAddress(rakConn.remoteAddr, rakConn.remotePort.toInt())
	}

	override fun doBind(p0: SocketAddress?) {
	}

	override fun doDisconnect() {
		rakConn.close()
	}

	override fun doClose() {
		rakConn.close()
	}

	override fun doBeginRead() {
		if (readPending) return

		readPending = true
		// FIXME: find better solution
		eventLoopRead.execute {
			if (!readPending) {
				// We have to check readPending here because the Runnable to read could have been scheduled and later
				// during the same read loop readPending was set to false.
				return@execute
			}
			readPending = false

			val pipeline = pipeline()
			try {
				val message = rakConn.read()
				pipeline.fireChannelRead(Unpooled.wrappedBuffer(message))
			} catch (t: Throwable) {
				pipeline.fireExceptionCaught(t)
				if (!rakConn.isOpen) {
					pipeline.fireChannelInactive()
				}
				return@execute
			}

			if (readPending || config().isAutoRead) {
				read()
			}
		}
	}

	private fun writeByteBuf(buf: ByteBuf) {
		val data = ByteArray(buf.readableBytes())
		buf.readBytes(data)
		rakConn.write(data)
	}

	override fun doWrite(buf: ChannelOutboundBuffer) {
		while (true) {
			val msg = buf.current() ?: break

			if (msg is ByteBuf) {
				writeByteBuf(msg)
				buf.remove()
			} else if (msg is RakMessage) {
				writeByteBuf(msg.content())
			} else {
				buf.remove(UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg)))
			}
		}
	}

	private inner class Unsafe : AbstractUnsafe() {

		override fun connect(p0: SocketAddress?, p1: SocketAddress?, p2: ChannelPromise) {
			safeSetSuccess(p2)
		}
	}
}
