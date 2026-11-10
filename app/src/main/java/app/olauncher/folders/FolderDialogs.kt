package app.olauncher.folders

import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import app.olauncher.R
import app.olauncher.databinding.DialogBaseBinding
import app.olauncher.helper.OlDialog
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast

/** Diálogo para pedir el nombre de una carpeta. No se cierra con un nombre vacío. */
fun showFolderNameDialog(
    context: Context,
    initialName: String = "",
    onConfirm: (name: String) -> Unit,
) {
    val dialog = OlDialog(context)
    val binding = DialogBaseBinding.inflate(LayoutInflater.from(dialog.context))
    binding.tvTitle.setText(R.string.new_folder)
    binding.tvAction.setText(R.string.create)
    binding.ivClose.setOnClickListener { dialog.dismiss() }

    val content = LayoutInflater.from(dialog.context)
        .inflate(R.layout.dialog_folder_name, binding.contentContainer, false)
    binding.contentContainer.addView(content)
    binding.contentContainer.isVisible = true

    val editText = content.findViewById<AppCompatEditText>(R.id.etFolderName)
    editText.setText(initialName)
    editText.setSelectAllOnFocus(true)
    editText.imeOptions = EditorInfo.IME_ACTION_DONE
    editText.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            binding.tvAction.performClick()
            true
        } else false
    }

    binding.tvAction.setOnClickListener {
        val name = editText.text.toString().trim()
        if (name.isEmpty()) {
            context.showToast(context.getString(R.string.folder_name_required))
            return@setOnClickListener
        }
        onConfirm(name)
        dialog.dismiss()
    }

    dialog.setView(binding.root)
    dialog.showRespectingStatusBar()
    editText.requestFocus()
    editText.showKeyboard()
}
