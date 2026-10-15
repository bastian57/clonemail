package com.daftech.clonemail

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.daftech.clonemail.databinding.ActivityMainBinding
import com.daftech.clonemail.license.LicenseManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!LicenseManager.isActivated(LicenseManager.prefs(this))) {
            startActivity(Intent(this, ActivationActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = object : FragmentStateAdapter(this as FragmentActivity) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> BasesFragment()
                1 -> GeneratorFragment()
                2 -> BankFragment()
                else -> BackupFragment()
            }
        }
        binding.pager.isUserInputEnabled = false

        binding.bottomNav.setOnItemSelectedListener { item ->
            binding.pager.currentItem = when (item.itemId) {
                R.id.nav_bases -> 0
                R.id.nav_generator -> 1
                R.id.nav_bank -> 2
                else -> 3
            }
            true
        }
    }
}
