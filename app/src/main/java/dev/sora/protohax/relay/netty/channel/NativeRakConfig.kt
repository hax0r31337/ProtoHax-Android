package dev.sora.protohax.relay.netty.channel

import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.DefaultChannelConfig
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption
import java.net.InetSocketAddress

class NativeRakConfig(channel: Channel?) : DefaultChannelConfig(channel) {

    var targetAddress: InetSocketAddress? = null
    var protocolVersion = 0

    override fun getOptions(): Map<ChannelOption<*>, Any> {
        return this.getOptions(super.getOptions(), RakChannelOption.RAK_PROTOCOL_VERSION, RAK_NATIVE_TARGET_ADDRESS)
    }

    override fun <T> getOption(option: ChannelOption<T>): T {
        if (option === RakChannelOption.RAK_PROTOCOL_VERSION) {
            return Integer.valueOf(protocolVersion) as T
        } else if (option === RAK_NATIVE_TARGET_ADDRESS) {
            return targetAddress as T
        }
        return super.getOption(option)
    }

    override fun <T> setOption(option: ChannelOption<T>, value: T): Boolean {
        validate(option, value)

		if (option === RakChannelOption.RAK_PROTOCOL_VERSION) {
			protocolVersion = value as Int
		} else if (option === RAK_NATIVE_TARGET_ADDRESS) {
			targetAddress = value as InetSocketAddress
		} else {
			return super.setOption(option, value)
		}
		return true
    }

    companion object {
        val RAK_NATIVE_TARGET_ADDRESS: ChannelOption<InetSocketAddress> = ChannelOption.valueOf(RakChannelOption::class.java, "RAK_NATIVE_TARGET_ADDRESS")
    }
}
