package dev.sora.protohax.ui.activity

import android.app.Activity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.JsonParser
import dev.sora.protohax.MainActivity
import dev.sora.protohax.R
import dev.sora.protohax.util.ContextUtils.writeString
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
<div id="loading"></div>
        """.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        webview.loadData(data, "text/html; charset=UTF-8", "base64")
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