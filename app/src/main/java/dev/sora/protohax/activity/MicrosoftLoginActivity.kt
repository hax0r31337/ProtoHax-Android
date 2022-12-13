package dev.sora.protohax.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.JsonParser
import dev.sora.protohax.ContextUtils.writeString
import dev.sora.protohax.MainActivity
import dev.sora.protohax.R
import dev.sora.relay.utils.HttpUtils
import java.io.IOException
import kotlin.concurrent.thread

class MicrosoftLoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microsoft_login)

        // clear cookies to make sure its a fresh login
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { /* do nothing */ }

        val webview = findViewById<WebView>(R.id.webview)
        webview.settings.apply {
            javaScriptEnabled = true
        }
        webview.webViewClient = CustomWebViewClient(this)

        webview.loadUrl("https://login.live.com/oauth20_authorize.srf?client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&response_type=code&scope=service::user.auth.xboxlive.com::MBI_SSL")

        setResult(0)
    }

    fun loadingPage(text: String) {
        val webview = findViewById<WebView>(R.id.webview)
        webview.loadData("<html><body>$text</body></html>",
            "text/html", "UTF-8");
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
                    it.split("=").also { query[it[0]] = it[1] }
                } else query[it] = ""
            }

            if (!query.contains("code")) {
                Log.e("ProtoHax", "no token found in redirected url ${request.url}")
                activity.finish()
                return true
            }

            // convert m.r3_bay token to refresh token
            activity.loadingPage("Still loading...")
            thread {
                try {
                    val conn = HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
                        "client_id=00000000441cc96b&redirect_uri=https://login.live.com/oauth20_desktop.srf&grant_type=authorization_code&code=${query["code"]}",
                        mapOf("Content-Type" to "application/x-www-form-urlencoded"))
                    val json = JsonParser.parseReader(try {
                        conn.inputStream.reader(Charsets.UTF_8)
                    } catch (t: IOException) {
                        conn.errorStream.reader(Charsets.UTF_8)
                    }).asJsonObject
                    if (json.has("refresh_token")) {
                        activity.writeString(MainActivity.KEY_MICROSOFT_REFRESH_TOKEN, json.get("refresh_token").asString)
                        activity.setResult(MainActivity.RESPONSE_CODE_MICROSOFT_LOGIN_OK)
                        activity.finish()
                        return@thread
                    } else if(json.has("error")) {
                        Log.e("ProtoHax", "error during token convertion: ${json.get("error").asString}")
                    }
                    throw java.lang.RuntimeException("error during token convertion")
                } catch (t: Throwable) {
                    Log.e("ProtoHax", "token convert", t)
                    activity.finish()
                }
            }
            return true
        }
    }
}