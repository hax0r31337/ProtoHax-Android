package dev.sora.protohax.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContract
import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.util.EncryptionUtils
import dev.sora.protohax.R
import dev.sora.protohax.relay.Account
import dev.sora.protohax.relay.AccountManager
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.base64Decode
import java.io.IOException
import kotlin.concurrent.thread

class MicrosoftLoginActivity : Activity() {

    private lateinit var device: RakNetRelaySessionListenerMicrosoft.DeviceInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microsoft_login)
        setResult(RESULT_CANCELED)

        // clear cookies to make sure its a fresh login
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { /* do nothing */ }

        val webview = findViewById<WebView>(R.id.webview)
        webview.settings.apply {
            javaScriptEnabled = true
        }
        webview.webViewClient = CustomWebViewClient(this)

        device = intent.extras?.getString(EXTRAS_KEY_DEVICE_TYPE)?.let { RakNetRelaySessionListenerMicrosoft.devices[it] } ?: kotlin.run {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        webview.loadUrl("https://login.live.com/oauth20_authorize.srf?client_id=${device.appId}&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL")
    }

    fun showLoadingPage(text: String) {
        val webview = findViewById<WebView>(R.id.webview)
        // we need convert the body to base64 to make sure it loading properly
        // https://stackoverflow.com/questions/3961589/android-webview-and-loaddata
        val data = Base64.encodeToString("""
<h1>$text</h1>
<style>
body { margin-top: 100px; background-color: #4b4b4b; color: #fff; text-align:center; }
h1 {
 font: 2em sans-serif;
 margin-bottom: 40px;
}
#loading {
 display: inline-block;
 width: 50px;
 height: 50px;
 border: 3px solid rgba(255,255,255,.3);
 border-radius: 50%;
 border-top-color: #fff;
 animation: spin 1s linear infinite;
 -webkit-animation: spin 1s linear infinite;
}
@keyframes spin {
 to { -webkit-transform: rotate(360deg); }
}
@-webkit-keyframes spin {
 to { -webkit-transform: rotate(360deg); }
}
</style>
<div id="loading"></div>""".toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        webview.loadData(data, "text/html; charset=UTF-8", "base64")
    }

    fun loadData(text: String) {
        findViewById<WebView>(R.id.webview).loadData(text, "text/html", "UTF-8")
    }

    class CustomWebViewClient(private val activity: MicrosoftLoginActivity) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.url.toString().startsWith("https://login.live.com/oauth20_desktop.srf?", true)) {
                Log.e("ProtoHax", "invalid url ${request.url}")
                return false
            }
            val query = mutableMapOf<String, String>()
            (request.url.query ?: "").split("&").forEach {
                if (it.contains("=")) {
                    val idx = it.indexOf("=")
                    query[it.substring(0, idx)] = it.substring(idx + 1)
                } else query[it] = ""
            }

            if (!query.contains("code")) {
                Log.e("ProtoHax", "no token found in redirected url ${request.url}")
                activity.finish()
                return true
            }

            // convert m.r3_bay token to refresh token
            activity.showLoadingPage("Still loading (0/3)")
            thread {
                try {
                    val conn = HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
                        "client_id=${activity.device.appId}&redirect_uri=https://login.live.com/oauth20_desktop.srf&grant_type=authorization_code&code=${query["code"]}",
                        mapOf("Content-Type" to "application/x-www-form-urlencoded"))
                    val json = JsonParser.parseReader(try {
                        conn.inputStream.reader(Charsets.UTF_8)
                    } catch (t: IOException) {
                        conn.errorStream.reader(Charsets.UTF_8)
                    }).asJsonObject
                    if (json.has("refresh_token") && json.has("access_token") && json.has("user_id")) {
                        activity.runOnUiThread { activity.showLoadingPage("Still loading (1/3)") }
                        // fetch username through chain
                        val username = try {
                            val identityToken = RakNetRelaySessionListenerMicrosoft.fetchIdentityToken(json.get("access_token").asString, activity.device)
                            activity.runOnUiThread { activity.showLoadingPage("Still loading (2/3)") }
                            getUsernameFromChain(RakNetRelaySessionListenerMicrosoft.fetchRawChain(identityToken, EncryptionUtils.createKeyPair().public).readText())
                        } catch (t: Throwable) {
                            Log.e("ProtoHax", "fetch username", t)
                            "user ${json.get("user_id").asString}"
                        }

                        AccountManager.accounts.add(Account(username, activity.device, json.get("refresh_token").asString))
                        AccountManager.save()

                        activity.setResult(RESULT_OK)
                        activity.finish()
                        return@thread
                    } else if(json.has("error")) {
                        Log.e("ProtoHax", "error during token convertion: ${json.get("error").asString}")
                    }
                    throw java.lang.RuntimeException("error during token convertion")
                } catch (t: Throwable) {
                    Log.e("ProtoHax", "token convert", t)
                    activity.runOnUiThread { activity.loadData(t.toString())}
                }
            }
            return true
        }

        private fun getUsernameFromChain(chains: String): String {
            val body = JsonParser.parseString(chains).asJsonObject.getAsJsonArray("chain")
            for (chain in body) {
                val chainBody = JsonParser.parseString(base64Decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val extraData = chainBody.getAsJsonObject("extraData")
                    return extraData.get("displayName").asString
                }
            }
            error("no username found")
        }
    }

    class LauncherContract : ActivityResultContract<RakNetRelaySessionListenerMicrosoft.DeviceInfo, Boolean>() {
        override fun createIntent(context: Context, device: RakNetRelaySessionListenerMicrosoft.DeviceInfo): Intent {
            return Intent(context, MicrosoftLoginActivity::class.java).apply {
                putExtra(EXTRAS_KEY_DEVICE_TYPE, device.deviceType)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == RESULT_OK
        }
    }

    companion object {
        const val EXTRAS_KEY_DEVICE_TYPE = "device"
    }
}