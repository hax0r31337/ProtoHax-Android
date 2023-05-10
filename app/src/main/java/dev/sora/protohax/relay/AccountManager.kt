package dev.sora.protohax.relay

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import dev.sora.protohax.MyApplication
import dev.sora.protohax.util.ContextUtils.readString
import dev.sora.protohax.util.ContextUtils.writeString
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import java.io.File
import java.lang.reflect.Type

object AccountManager {

    private const val KEY_CURRENT_MICROSOFT_REFRESH_TOKEN = "MICROSOFT_REFRESH_TOKEN"

    val accounts = mutableListOf<Account>()
    private var currentRefreshToken: String?
        get() = MyApplication.instance.readString(KEY_CURRENT_MICROSOFT_REFRESH_TOKEN)?.ifEmpty { null }
        set(value) = MyApplication.instance.writeString(KEY_CURRENT_MICROSOFT_REFRESH_TOKEN, value ?: "")
    var currentAccount: Account?
        get() = currentRefreshToken?.let { t -> accounts.find { it.refreshToken == t } }
        set(value) { if (value == null) currentRefreshToken = null else if (accounts.contains(value)) currentRefreshToken = value.refreshToken }

    private val storeFile = File(MyApplication.instance.filesDir, "credentials.json")
    private val gson = GsonBuilder()
        .registerTypeAdapter(XboxDeviceInfo::class.java, DeviceInfoAdapter())
        .create()

    init {
        load()
    }

    fun load() {
        accounts.clear()
        if (!storeFile.exists()) {
            currentRefreshToken = null
            return
        }
        accounts.addAll(gson.fromJson(storeFile.reader(Charsets.UTF_8), Array<Account>::class.java))
        // clean up current refresh token from legacy version
        cleanupCurrentRefreshToken()
    }

    fun save() {
        storeFile.writeText(gson.toJson(accounts.toTypedArray(), Array<Account>::class.java))
    }

    private fun cleanupCurrentRefreshToken() {
        val current = currentRefreshToken
        accounts.forEach {
            if (it.refreshToken == current) {
                return
            }
        }
        currentRefreshToken = null
    }

    private class DeviceInfoAdapter : JsonSerializer<XboxDeviceInfo>, JsonDeserializer<XboxDeviceInfo> {

        override fun serialize(src: XboxDeviceInfo, typeOf: Type?, ctx: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src.deviceType)
        }

        override fun deserialize(json: JsonElement, typeOf: Type?, ctx: JsonDeserializationContext?): XboxDeviceInfo {
            return XboxDeviceInfo.devices[json.asString] ?: XboxDeviceInfo.DEVICE_ANDROID
        }
    }
}

class Account(
    @SerializedName("remark") var remark: String,
    @SerializedName("device") val platform: XboxDeviceInfo,
    @SerializedName("refresh_token") var refreshToken: String
) {

    /**
     * @return accessToken
     */
    fun refresh(): String {
        val isCurrent = AccountManager.currentAccount == this
		val (accessToken, refreshToken) = platform.refreshToken(refreshToken)
        this.refreshToken = refreshToken
        if (isCurrent) {
            // refreshes the token field
            AccountManager.currentAccount = this
        }
        AccountManager.save()
        return accessToken
    }
}
