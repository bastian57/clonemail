package com.daftech.clonemail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.daftech.clonemail.databinding.FragmentGeneratorBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GeneratorFragment : Fragment() {

    private var _binding: FragmentGeneratorBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: AliasStore
    private var previewEmail: String? = null
    private var previewBase: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AliasStore(requireContext())
        refreshBaseSpinner()

        binding.etCustomTag.visibility = View.GONE
        binding.rgPattern.setOnCheckedChangeListener { _, checkedId ->
            binding.etCustomTag.visibility = if (checkedId == R.id.rbCustom) View.VISIBLE else View.GONE
            updatePreview()
        }
        binding.etSite.setOnFocusChangeListener { _, _ -> updatePreview() }
        binding.etCustomTag.setOnFocusChangeListener { _, _ -> updatePreview() }

        binding.btnGenerate.setOnClickListener { updatePreview() }

        binding.btnSave.setOnClickListener {
            val email = previewEmail ?: run {
                Toast.makeText(requireContext(), "Buat pratinjau dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val base = previewBase ?: return@setOnClickListener
            val existing = store.aliases()
            if (existing.any { it.aliasEmail.equals(email, true) }) {
                Toast.makeText(requireContext(), "Alias ini sudah ada di bank", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val site = binding.etSite.text.toString().trim().ifEmpty { "tanpa-nama" }
            val entry = AliasEntry(
                id = UUID.randomUUID().toString(),
                siteName = site,
                siteUrl = binding.etSiteUrl.text.toString().trim(),
                aliasEmail = email,
                baseEmail = base,
                createdAt = System.currentTimeMillis(),
                notes = binding.etNotes.text.toString().trim(),
                status = "active",
                spamHits = 0
            )
            existing.add(entry)
            store.saveAliases(existing)
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("CloneMail", email))
            Toast.makeText(requireContext(), "Tersimpan & disalin: $email", Toast.LENGTH_LONG).show()
            binding.etSite.text.clear()
            binding.etSiteUrl.text.clear()
            binding.etNotes.text.clear()
            binding.etCustomTag.text.clear()
            previewEmail = null
            binding.tvPreview.text = "Pratinjau alias…"
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBaseSpinner()
    }

    private fun refreshBaseSpinner() {
        val bases = store.bases()
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, bases.ifEmpty { listOf("— tambah base dulu —") })
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spBase.adapter = adapter
    }

    private fun updatePreview() {
        val bases = store.bases()
        if (bases.isEmpty()) {
            binding.tvPreview.text = "Tambahkan email dasar di tab Bases dulu."
            previewEmail = null
            return
        }
        val pos = binding.spBase.selectedItemPosition.coerceIn(0, bases.size - 1)
        val base = bases[pos]
        val site = binding.etSite.text.toString().trim()
        val tag = when (binding.rgPattern.checkedRadioButtonId) {
            R.id.rbRandom -> AliasStore.sanitizeTag(site) + random4()
            R.id.rbDate -> AliasStore.sanitizeTag(site) + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            R.id.rbCustom -> AliasStore.sanitizeTag(binding.etCustomTag.text.toString())
            else -> AliasStore.sanitizeTag(site)
        }
        if (tag.isEmpty()) {
            binding.tvPreview.text = "Isi nama situs / tag dulu."
            previewEmail = null
            return
        }
        previewEmail = AliasStore.buildAlias(base, tag)
        previewBase = base
        binding.tvPreview.text = previewEmail
    }

    private fun random4(): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        return (1..4).map { chars.random() }.joinToString("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
