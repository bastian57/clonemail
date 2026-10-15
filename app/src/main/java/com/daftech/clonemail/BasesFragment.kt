package com.daftech.clonemail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daftech.clonemail.databinding.FragmentBasesBinding

class BasesFragment : Fragment() {

    private var _binding: FragmentBasesBinding? = null
    private val binding get() = _binding!!
    private lateinit var store: AliasStore
    private val bases = mutableListOf<String>()
    private lateinit var adapter: BaseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = AliasStore(requireContext())
        adapter = BaseAdapter(bases) { pos -> confirmDelete(pos) }
        binding.rvBases.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBases.adapter = adapter

        binding.btnAddBase.setOnClickListener {
            val email = binding.etBase.text.toString().trim()
            if (!AliasStore.EMAIL_GMAIL.matches(email)) {
                Toast.makeText(requireContext(), "Harus alamat @gmail.com yang valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (bases.any { it.equals(email, true) }) {
                Toast.makeText(requireContext(), "Sudah ada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bases.add(email)
            store.saveBases(bases)
            binding.etBase.text.clear()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        bases.clear()
        bases.addAll(store.bases())
        adapter.notifyDataSetChanged()
    }

    private fun confirmDelete(pos: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus base?")
            .setMessage(bases[pos] + "\nAlias yang sudah dibuat dari base ini tetap tersimpan.")
            .setPositiveButton("Hapus") { _, _ ->
                bases.removeAt(pos)
                store.saveBases(bases)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class BaseAdapter(
        private val items: List<String>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<BaseAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tvBaseEmail)
            val btn: Button = v.findViewById(R.id.btnDeleteBase)
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_base, p, false))
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            h.tv.text = items[pos]
            h.btn.setOnClickListener { onDelete(h.bindingAdapterPosition) }
        }
    }
}
