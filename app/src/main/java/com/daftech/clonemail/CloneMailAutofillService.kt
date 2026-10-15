package com.daftech.clonemail

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

/**
 * v2: menyarankan alias email CloneMail pada kolom email di aplikasi/browser lain.
 * Tidak menyimpan password, tidak membaca isi form — hanya mengisi alias yang dipilih user.
 */
class CloneMailAutofillService : android.service.autofill.AutofillService() {

    override fun onFillRequest(request: FillRequest, signal: CancellationSignal, callback: FillCallback) {
        val context = request.fillContexts.lastOrNull()
        if (context == null) {
            callback.onSuccess(null)
            return
        }
        val emailIds = mutableListOf<AutofillId>()
        collectEmailFields(context.structure, emailIds)
        if (emailIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }
        val aliases = try {
            AliasStore(this).activeAliases()
        } catch (e: Exception) {
            emptyList()
        }
        if (aliases.isEmpty()) {
            callback.onSuccess(null)
            return
        }
        val response = FillResponse.Builder()
        for (alias in aliases.take(20)) {
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            presentation.setTextViewText(android.R.id.text1, alias.aliasEmail)
            val ds = Dataset.Builder()
            for (id in emailIds) {
                ds.setValue(id, AutofillValue.forText(alias.aliasEmail), presentation)
            }
            response.addDataset(ds.build())
        }
        callback.onSuccess(response.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // CloneMail tidak menyimpan kredensial — abaikan permintaan simpan.
        callback.onSuccess()
    }

    private fun collectEmailFields(structure: AssistStructure, out: MutableList<AutofillId>) {
        for (i in 0 until structure.windowNodeCount) {
            collectFromNode(structure.getWindowNodeAt(i).rootViewNode, out)
        }
    }

    private fun collectFromNode(node: AssistStructure.ViewNode, out: MutableList<AutofillId>) {
        val hints = node.autofillHints
        val hintSaysEmail = hints?.any { it.contains("email", ignoreCase = true) } == true ||
            (node.hint?.contains("email", ignoreCase = true) == true)
        val inputIsEmail = (node.inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0
        if (hintSaysEmail || inputIsEmail) {
            node.autofillId?.let { out.add(it) }
        }
        for (i in 0 until node.childCount) {
            collectFromNode(node.getChildAt(i), out)
        }
    }
}
