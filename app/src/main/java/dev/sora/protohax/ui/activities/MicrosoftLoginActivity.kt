package dev.sora.protohax.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContract
import com.google.gson.JsonParser
import dev.sora.protohax.R
import dev.sora.protohax.relay.Account
import dev.sora.protohax.relay.AccountManager
import dev.sora.relay.session.listener.xbox.RelayListenerXboxLogin
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import dev.sora.relay.session.listener.xbox.XboxGamerTagException
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.logError
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.random.nextInt

class MicrosoftLoginActivity : Activity() {

    private lateinit var device: XboxDeviceInfo

    @SuppressLint("SetJavaScriptEnabled")
	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microsoft_login)
        setResult(RESULT_CANCELED)

        // clear cookies to make sure its a fresh login
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { /* do nothing */ }

        val webView = findViewById<WebView>(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
        }
        webView.webViewClient = CustomWebViewClient(this)

        device = intent.extras?.getString(EXTRAS_KEY_DEVICE_TYPE)?.let { XboxDeviceInfo.devices[it] } ?: kotlin.run {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        webView.loadUrl("https://login.live.com/oauth20_authorize.srf?client_id=${device.appId}&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL")
    }

    fun showLoadingPage(text: String) {
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
		findViewById<WebView>(R.id.webview).loadData(data, "text/html; charset=UTF-8", "base64")
    }

    fun loadData(text: String) {
        findViewById<WebView>(R.id.webview).loadData(text, "text/html", "UTF-8")
    }

    class CustomWebViewClient(private val activity: MicrosoftLoginActivity) : WebViewClient() {

		private var account: Pair<String, String>? = null

        @SuppressLint("SuspiciousIndentation")
		override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
			if (account != null && (request.url.scheme ?: "").startsWith("ms-xal")) {
				thread {
					try {
						activity.runOnUiThread { activity.showLoadingPage("Still loading (0/2)") }
						// fetch username through chain
						val credentials = RelayListenerXboxLogin.fetchXboxCredentials(account!!.first, activity.device)
						activity.runOnUiThread { activity.showLoadingPage("Still loading (1/2)") }
						// activate account on Minecraft authentication server
						credentials.fetchMinecraftToken()
						val username = getUsernameFromChain(RelayListenerXboxLogin.fetchRawChain(credentials.fetchIdentityToken().token, EncryptionUtils.createKeyPair().public).readText())

						AccountManager.accounts.add(Account(username, activity.device, account!!.second))
						AccountManager.save()

						activity.setResult(RESULT_OK)
						activity.finish()
					} catch (t: Throwable) {
						logError("obtain access token", t)
						activity.runOnUiThread { activity.loadData(t.toString()) }
					}
				}
				return true
			}
			val url = request.url.toString().toHttpUrlOrNull() ?: return false
            if (url.host != "login.live.com" || url.encodedPath != "/oauth20_desktop.srf") {
				if (url.queryParameter("res") == "cancel") {
					logError("action cancelled")
					activity.setResult(RESULT_CANCELED)
					activity.finish()
					return false
				}
                logError("invalid url ${request.url}")
                return false
            }

			val authCode = url.queryParameter("code") ?: return false

			// convert m.r3_bay token to refresh token
            activity.showLoadingPage("Still loading (0/3)")
            thread {
                try {
					val (accessToken, refreshToken) = activity.device.refreshToken(authCode)
					activity.runOnUiThread { activity.showLoadingPage("Still loading (1/3)") }
					// fetch username through chain
					val username = try {
						val credentials = RelayListenerXboxLogin.fetchXboxCredentials(accessToken, activity.device)
						activity.runOnUiThread { activity.showLoadingPage("Still loading (2/3)") }
						// activate account on Minecraft authentication server
						credentials.fetchMinecraftToken()
						getUsernameFromChain(RelayListenerXboxLogin.fetchRawChain(credentials.fetchIdentityToken().token, EncryptionUtils.createKeyPair().public).readText())
					} catch (e: XboxGamerTagException) {
						account = accessToken to refreshToken
						activity.runOnUiThread {
							activity.findViewById<WebView>(R.id.webview).loadUrl(e.sisuStartUrl)
						}
						return@thread
					}

					val account = Account(username, activity.device, refreshToken)
					while (AccountManager.accounts.map { it.remark }.contains(account.remark)) {
						account.remark += Random.nextInt(0..9)
					}
					AccountManager.accounts.add(account)
					AccountManager.save()

					activity.setResult(RESULT_OK)
					activity.finish()
                } catch (t: Throwable) {
                    logError("obtain access token", t)
                    activity.runOnUiThread { activity.loadData(t.toString()) }
                }
            }
            return true
        }

        private fun getUsernameFromChain(chains: String): String {
            val body = JsonParser.parseString(chains).asJsonObject
			if (!body.has("chain")) {
				error("no field named \"chain\" found in json: $chains")
			}
            for (chain in body.getAsJsonArray("chain")) {
                val chainBody = JsonParser.parseString(base64Decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val extraData = chainBody.getAsJsonObject("extraData")
                    return extraData.get("displayName").asString
                }
            }
            error("no username found")
        }
    }

    class LauncherContract : ActivityResultContract<XboxDeviceInfo, Boolean>() {
        override fun createIntent(context: Context, input: XboxDeviceInfo): Intent {
            return Intent(context, MicrosoftLoginActivity::class.java).apply {
                putExtra(EXTRAS_KEY_DEVICE_TYPE, input.deviceType)
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
