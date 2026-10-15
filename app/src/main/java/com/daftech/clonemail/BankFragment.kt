package com.daftech.clonemail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daftech.clonemail.databinding.FragmentBankBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BankFragment : Fragment() {

    private var _binding: FragmentBankBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: AliasStore
    private val all = mutableListOf<AliasEntry>()
    private val shown = mutableListOf<AliasEntry>()
    private lateinit var adapter: AliasAdapter

    private val statuses = listOf("Semua", "active", "archived", "spam")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AliasStore(requireContext())
        adapter = AliasAdapter(shown,
            onCopy = { copyToClipboard(it.aliasEmail) },
            onShare = { shareAlias(it) },
            onSpam = { markSpam(it) },
            onArchive = { toggleArchive(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.rvBank.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBank.adapter = adapter

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFilterStatus.adapter = statusAdapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilter()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        binding.spFilterStatus.onItemSelectedListener = selListener()
        binding.spFilterBase.onItemSelectedListener = selListener()
    }

    private fun selListener() = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = applyFilter()
        override fun onNothingSelected(p: AdapterView<*>?) {}
    }

    override fun onResume() {
        super.onResume()
        all.clear()
        all.addAll(store.aliases().sortedByDescending { it.createdAt })
        val bases = listOf("Semua") + store.bases()
        val baseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, bases)
        baseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFilterBase.adapter = baseAdapter
        applyFilter()
    }

    private fun applyFilter() {
        val q = binding.etSearch.text.toString().trim().lowercase()
        val statusPos = binding.spFilterStatus.selectedItemPosition.coerceAtLeast(0)
        val statusFilter = statuses[statusPos]
        val baseFilter = (binding.spFilterBase.selectedItem as? String) ?: "Semua"
        shown.clear()
        shown.addAll(all.filter { e ->
            (statusFilter == "Semua" || e.status == statusFilter) &&
            (baseFilter == "Semua" || e.baseEmail == baseFilter) &&
            (q.isEmpty() || e.siteName.lowercase().contains(q) || e.aliasEmail.lowercase().contains(q))
        })
        adapter.notifyDataSetChanged()
        binding.tvCount.text = "${shown.size} dari ${all.size} alias"
    }

    private fun copyToClipboard(email: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("CloneMail", email))
        Toast.makeText(requireContext(), "Disalin: $email", Toast.LENGTH_SHORT).show()
    }

    private fun shareAlias(e: AliasEntry) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${e.siteName}: ${e.aliasEmail}")
        }
        startActivity(Intent.createChooser(intent, "Bagikan alias"))
    }

    private fun markSpam(e: AliasEntry) {
        update(e.copy(status = "spam", spamHits = e.spamHits + 1))
        Toast.makeText(requireContext(), "Ditandai spam (${e.spamHits + 1}x)", Toast.LENGTH_SHORT).show()
    }

    private fun toggleArchive(e: AliasEntry) {
        val next = if (e.status == "archived") "active" else "archived"
        update(e.copy(status = next))
    }

    private fun confirmDelete(e: AliasEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus alias?")
            .setMessage(e.aliasEmail)
            .setPositiveButton("Hapus") { _, _ ->
                all.removeAll { it.id == e.id }
                store.saveAliases(all)
                applyFilter()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun update(e: AliasEntry) {
        val idx = all.indexOfFirst { it.id == e.id }
        if (idx >= 0) all[idx] = e
        store.saveAliases(all)
        applyFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class AliasAdapter(
        private val items: List<AliasEntry>,
        private val onCopy: (AliasEntry) -> Unit,
        private val onShare: (AliasEntry) -> Unit,
        private val onSpam: (AliasEntry) -> Unit,
        private val onArchive: (AliasEntry) -> Unit,
        private val onDelete: (AliasEntry) -> Unit
    ) : RecyclerView.Adapter<AliasAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmail: TextView = v.findViewById(R.id.tvAliasEmail)
            val tvSite: TextView = v.findViewById(R.id.tvSiteName)
            val tvMeta: TextView = v.findViewById(R.id.tvMeta)
            val btnCopy: Button = v.findViewById(R.id.btnCopy)
            val btnShare: Button = v.findViewById(R.id.btnShare)
            val btnSpam: Button = v.findViewById(R.id.btnSpam)
            val btnArchive: Button = v.findViewById(R.id.btnArchive)
            val btnDelete: Button = v.findViewById(R.id.btnDelete)
        }

        private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale("id"))

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_alias, p, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val e = items[pos]
            h.tvEmail.text = e.aliasEmail
            h.tvSite.text = e.siteName
            val statusLabel = when (e.status) {
                "archived" -> "arsip"
                "spam" -> "spam (${e.spamHits}x)"
                else -> "aktif"
            }
            h.tvMeta.text = "${e.baseEmail} • ${dateFmt.format(Date(e.createdAt))} • $statusLabel"
            h.btnArchive.text = if (e.status == "archived") "Aktifkan" else "Arsip"
            val p2 = h.bindingAdapterPosition
            h.btnCopy.setOnClickListener { if (p2 != RecyclerView.NO_POSITION) onCopy(items[p2]) }
            h.btnShare.setOnClickListener { if (p2 != RecyclerView.NO_POSITION) onShare(items[p2]) }
            h.btnSpam.setOnClickListener { if (p2 != RecyclerView.NO_POSITION) onSpam(items[p2]) }
            h.btnArchive.setOnClickListener { if (p2 != RecyclerView.NO_POSITION) onArchive(items[p2]) }
            h.btnDelete.setOnClickListener { if (p2 != RecyclerView.NO_POSITION) onDelete(items[p2]) }
        }
    }
}
