package com.daftech.clonemail

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class AliasEntry(
    val id: String,
    val siteName: String,
    val siteUrl: String,
    val aliasEmail: String,
    val baseEmail: String,
    val createdAt: Long,
    val notes: String,
    val status: String,
    val spamHits: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("siteName", siteName)
        .put("siteUrl", siteUrl)
        .put("aliasEmail", aliasEmail)
        .put("baseEmail", baseEmail)
        .put("createdAt", createdAt)
        .put("notes", notes)
        .put("status", status)
        .put("spamHits", spamHits)

    companion object {
        fun fromJson(o: JSONObject): AliasEntry = AliasEntry(
            id = o.optString("id", UUID.randomUUID().toString()),
            siteName = o.optString("siteName", ""),
            siteUrl = o.optString("siteUrl", ""),
            aliasEmail = o.optString("aliasEmail", ""),
            baseEmail = o.optString("baseEmail", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            notes = o.optString("notes", ""),
            status = o.optString("status", "active"),
            spamHits = o.optInt("spamHits", 0)
        )
    }
}

/** Local offline JSON store. No network, no passwords, ever. */
class AliasStore(context: Context) {
    private val file: File = File(context.filesDir, "clonemail.json")
    private val lock = Any()

    private fun readRoot(): JSONObject = synchronized(lock) {
        if (!file.exists()) return JSONObject().put("bases", JSONArray()).put("aliases", JSONArray())
        try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            JSONObject().put("bases", JSONArray()).put("aliases", JSONArray())
        }
    }

    private fun writeRoot(root: JSONObject) = synchronized(lock) {
        file.writeText(root.toString())
    }

    fun bases(): MutableList<String> {
        val arr = readRoot().optJSONArray("bases") ?: JSONArray()
        return MutableList(arr.length()) { i -> arr.optString(i) }
    }

    fun saveBases(bases: List<String>) {
        val root = readRoot()
        val arr = JSONArray()
        bases.forEach { arr.put(it) }
        root.put("bases", arr)
        writeRoot(root)
    }

    fun aliases(): MutableList<AliasEntry> {
        val arr = readRoot().optJSONArray("aliases") ?: JSONArray()
        return MutableList(arr.length()) { i -> AliasEntry.fromJson(arr.optJSONObject(i) ?: JSONObject()) }
    }

    fun saveAliases(list: List<AliasEntry>) {
        val root = readRoot()
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        root.put("aliases", arr)
        writeRoot(root)
    }

    fun activeAliases(): List<AliasEntry> =
        aliases().filter { it.status == "active" }.sortedByDescending { it.createdAt }

    fun exportJson(): String = readRoot().toString(2)

    fun importJson(json: String): Boolean {
        return try {
            val root = JSONObject(json)
            if (!root.has("bases") || !root.has("aliases")) return false
            writeRoot(root)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        val EMAIL_GMAIL = Regex("^[A-Za-z0-9._%+-]+@gmail\\.com$")

        /** Sanitize a plus-addressing tag: lowercase alnum only. */
        fun sanitizeTag(raw: String): String =
            raw.lowercase().filter { it.isLetterOrDigit() }

        fun buildAlias(base: String, tag: String): String {
            val local = base.substringBefore("@")
            return "$local+$tag@gmail.com"
        }
    }
}
