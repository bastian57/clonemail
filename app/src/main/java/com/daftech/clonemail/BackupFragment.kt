package com.daftech.clonemail

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.daftech.clonemail.databinding.FragmentBackupBinding
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: AliasStore

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        try {
            val text = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            if (store.importJson(text)) {
                Toast.makeText(requireContext(), "Import berhasil", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "File bukan backup CloneMail yang valid", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Import gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AliasStore(requireContext())

        binding.btnExportJson.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "clonemail_backup_$stamp.json"
            if (writeToDownload(name, store.exportJson())) {
                Toast.makeText(requireContext(), "Tersimpan: Download/$name", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Gagal menulis ke Download", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnExportCsv.setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "clonemail_backup_$stamp.csv"
            if (writeToDownload(name, buildCsv())) {
                Toast.makeText(requireContext(), "Tersimpan: Download/$name", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Gagal menulis ke Download", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnImport.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Import backup?")
                .setMessage("Data saat ini akan DIGANTI seluruhnya dengan isi file backup.")
                .setPositiveButton("Lanjut") { _, _ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    importLauncher.launch(intent)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnEnableAutofill.setOnClickListener {
            try {
                val cn = ComponentName(requireContext(), CloneMailAutofillService::class.java)
                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                    data = android.net.Uri.parse("package:com.daftech.clonemail")
                    putExtra(Settings.EXTRA_AUTOFILL_SERVICE, cn)
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), "Buka Pengaturan manual: Sistem → Bahasa & masukan → Layanan isi-otomatis", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun writeToDownload(name: String, content: String): Boolean {
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            File(dir, name).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildCsv(): String {
        val sb = StringBuilder("siteName,siteUrl,aliasEmail,baseEmail,createdAt,notes,status,spamHits\n")
        for (e in store.aliases()) {
            fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
            sb.append(listOf(esc(e.siteName), esc(e.siteUrl), esc(e.aliasEmail), esc(e.baseEmail),
                e.createdAt.toString(), esc(e.notes), esc(e.status), e.spamHits.toString()).joinToString(","))
            sb.append("\n")
        }
        return sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
