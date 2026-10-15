package com.daftech.clonemail.license

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Sistem lisensi offline CloneMail (format v2 universal, sama keypair dgn AIR KITA / CukurBoss / WarungBoss).
 * Format: CM1-<base64url(data)>-<base64url(signature)>
 * data v2 = "deviceId|buyerName|expiryEpoch|productId" (expiryEpoch 0 = permanen)
 * signature = RSA-SHA256(data) dengan private key milik DAFTECH.
 * Private key TIDAK ada di repo ini - hanya public key untuk verifikasi.
 * Kode dibuat via License Tool (tambah produk: CloneMail / CM1- / clonemail).
 */
sealed class LicenseResult {
    data class Valid(val buyerName: String, val expiry: Long) : LicenseResult()
    data class Invalid(val reason: String) : LicenseResult()
}

object LicenseManager {
    /** Public key RSA-2048 (X.509 DER, base64). SAMA persis dgn AIR KITA / CukurBoss / WarungBoss (rotasi 2026-10-13). */
    const val PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmymReWmXPPR+CvKPXUMi" +
        "Xjs6iW9XYUcJfsJK1fO3AqZ4xwqQr91RJBoTXDxhQzVmYflcPJd+oT8wuHRnPlxk" +
        "JLnFX4hamoy+db0RkLKOfyoTbksf14ETEjUHp/NN6WWEceg37lrMHEj1Mdc9djuR" +
        "pQ6oos/k4dmLSo8+N73evWW5LvNeSNQJNeZZFyvjN+0CrOe2ZCvYFvWZz7g49tV6" +
        "nhB2cQXbVNdTgK4cb7P8OrdQ++ujRSU4AiV4hcX+Mgk5MtTuL80QV/eOe6B+9ZMh" +
        "+cnIAU7qwujieegEAxSgr5g9L7fGI5OUqTxzeILftCjkpFbbhCg6TY+KOgm4aL5N" +
        "hwIDAQAB"

    const val PREFS_NAME = "clonemail_license"
    private const val KEY_ACTIVATED = "activated"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_BUYER = "buyer"
    private const val KEY_EXPIRY = "expiry"
    const val PRODUCT_ID = "clonemail"
    const val LICENSE_PREFIX = "CM1-"
    const val ALLOW_LEGACY_V1 = false

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    fun isActivated(prefs: SharedPreferences): Boolean {
        if (!prefs.getBoolean(KEY_ACTIVATED, false)) return false
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        return expiry == 0L || expiry > System.currentTimeMillis()
    }

    fun validateLicense(rawKey: String, deviceId: String): LicenseResult {
        val key = rawKey.trim()
        if (!key.startsWith(LICENSE_PREFIX)) return LicenseResult.Invalid("Format kode salah (harus diawali $LICENSE_PREFIX)")
        val rest = key.removePrefix(LICENSE_PREFIX)
        if (rest.length <= 343) return LicenseResult.Invalid("Format kode salah")
        val dataB64 = rest.dropLast(343)
        val sigB64 = rest.takeLast(342)
        if (dataB64.isBlank()) return LicenseResult.Invalid("Format kode salah")
        val dataBytes: ByteArray
        val sigBytes: ByteArray
        try {
            dataBytes = b64UrlDecode(dataB64)
            sigBytes = b64UrlDecode(sigB64)
        } catch (_: Exception) {
            return LicenseResult.Invalid("Kode tidak valid (encoding rusak)")
        }
        val ok = try {
            val pubKey = KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT))
            )
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update(dataBytes)
            sig.verify(sigBytes)
        } catch (_: Exception) {
            return LicenseResult.Invalid("Kode tidak valid (verifikasi gagal)")
        }
        if (!ok) return LicenseResult.Invalid("Kode tidak valid (signature salah)")
        val dataStr = try { String(dataBytes, Charsets.UTF_8) } catch (_: Exception) { return LicenseResult.Invalid("Kode tidak valid") }
        val fields = dataStr.split('|')
        val licDevice: String
        val buyer: String
        val expiry: Long
        when (fields.size) {
            4 -> {
                if (fields[3] != PRODUCT_ID) return LicenseResult.Invalid("Kode ini untuk produk lain")
                licDevice = fields[0]
                buyer = fields[1]
                expiry = fields[2].toLongOrNull() ?: return LicenseResult.Invalid("Kode tidak valid")
            }
            3 -> {
                if (!ALLOW_LEGACY_V1) return LicenseResult.Invalid("Format kode kedaluwarsa - minta kode baru")
                licDevice = fields[0]
                buyer = fields[1]
                expiry = fields[2].toLongOrNull() ?: return LicenseResult.Invalid("Kode tidak valid")
            }
            else -> return LicenseResult.Invalid("Kode tidak valid")
        }
        if (licDevice != deviceId) return LicenseResult.Invalid("Kode ini untuk perangkat lain")
        if (expiry != 0L && expiry <= System.currentTimeMillis()) return LicenseResult.Invalid("Lisensi sudah kedaluwarsa")
        return LicenseResult.Valid(buyerName = buyer, expiry = expiry)
    }

    fun saveLicense(prefs: SharedPreferences, key: String, buyerName: String, expiry: Long) {
        prefs.edit().putBoolean(KEY_ACTIVATED, true).putString(KEY_LICENSE, key.trim()).putString(KEY_BUYER, buyerName).putLong(KEY_EXPIRY, expiry).apply()
    }

    private fun b64UrlDecode(s: String): ByteArray {
        var str = s.trim()
        val pad = (4 - str.length % 4) % 4
        repeat(pad) { str += "=" }
        return Base64.decode(str, Base64.URL_SAFE)
    }
}
