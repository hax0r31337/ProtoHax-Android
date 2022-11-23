package dev.sora.protohax

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

object AntiModification {

    private val SIGN_HASH = "7804ec4ecb06606c042a896fc75eb10a"
    var call = 0
        private set

    fun validateAppSignature(context: Context): Pair<Boolean, String> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        //note sample just checks the first signature
        call++
        for (signature in packageInfo.signingInfo.apkContentsSigners) {
            val hash = hash(signature.toByteArray())
            if (hash == SIGN_HASH) return true to "fuck signature killer"
        }
        return false to "ur mom"
    }

    private fun hash(sig: ByteArray): String {
        call++
        val digest = MessageDigest.getInstance("MD5")
        digest.update(sig)
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}