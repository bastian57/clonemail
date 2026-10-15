package com.daftech.clonemail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daftech.clonemail.databinding.ActivityActivationBinding
import com.daftech.clonemail.license.LicenseManager
import com.daftech.clonemail.license.LicenseResult

/**
 * Layar aktivasi lisensi device-bound (offline RSA).
 * Dibuka otomatis oleh MainActivity bila perangkat belum teraktivasi.
 */
class ActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationBinding
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = LicenseManager.getDeviceId(this)
        binding.tvDeviceId.text = deviceId

        val copyDeviceId = {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
            Toast.makeText(this, "Device ID disalin", Toast.LENGTH_SHORT).show()
        }
        binding.rowDeviceId.setOnClickListener { copyDeviceId() }

        binding.btnActivate.setOnClickListener {
            val code = binding.etLicenseCode.text.toString()
            if (code.isBlank()) {
                showError("Masukkan kode aktivasi dulu.")
                return@setOnClickListener
            }
            when (val r = LicenseManager.validateLicense(code, deviceId)) {
                is LicenseResult.Valid -> {
                    LicenseManager.saveLicense(LicenseManager.prefs(this), code, r.buyerName, r.expiry)
                    Toast.makeText(this, "Aktivasi berhasil. Selamat datang, " + r.buyerName, Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is LicenseResult.Invalid -> showError(r.reason)
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvLicenseError.text = msg
        binding.tvLicenseError.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        // Jangan biarkan user kabur tanpa aktivasi: tutup aplikasi.
        finishAffinity()
    }
}
