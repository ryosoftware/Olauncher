package app.olauncher.backup

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.olauncher.MainActivity
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.helper.OlDialog
import app.olauncher.helper.createDialog
import app.olauncher.helper.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsula la UI y la E/S del backup/restore para que el fragmento original solo tenga que
 * delegar. La lógica de serialización vive en [PrefsBackup].
 */
class BackupRestoreController(
    private val fragment: Fragment,
    private val viewModel: MainViewModel,
) {

    private var dialog: OlDialog? = null

    private val createDocument = fragment.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        keepScreenOnStop(false)
        uri?.let(::writeBackup)
    }

    private val openDocument = fragment.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        keepScreenOnStop(false)
        uri?.let(::readBackup)
    }

    fun backup() {
        keepScreenOnStop(true)
        try {
            createDocument.launch(suggestedFileName())
        } catch (_: Exception) {
            keepScreenOnStop(false)
            fragment.context?.showToast(R.string.backup_failed)
        }
    }

    fun restore() {
        keepScreenOnStop(true)
        try {
            openDocument.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            keepScreenOnStop(false)
            fragment.context?.showToast(R.string.restore_failed)
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun keepScreenOnStop(keep: Boolean) {
        (fragment.activity as? MainActivity)?.setKeepScreenOnStop(keep)
    }

    private fun writeBackup(uri: Uri) {
        val context = fragment.requireContext()
        val appContext = context.applicationContext
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        PrefsBackup.write(appContext, output)
                    } != null
                } catch (_: Exception) {
                    false
                }
            }
            appContext.showToast(
                if (success) R.string.backup_saved else R.string.backup_failed
            )
        }
    }

    private fun readBackup(uri: Uri) {
        val context = fragment.requireContext()
        val appContext = context.applicationContext
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val values = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        PrefsBackup.read(input)?.let(PrefsBackup::decode)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (values == null) {
                appContext.showToast(R.string.backup_invalid)
                return@launch
            }
            confirmRestore(values)
        }
    }

    private fun confirmRestore(values: Map<String, Any>) {
        val context = fragment.requireContext()
        dialog?.dismiss()
        dialog = context.createDialog(
            title = R.string.restore_confirm_title,
            action = R.string.restore_settings_action,
            message = R.string.restore_confirm_message,
            neutral = R.string.cancel,
            onAction = { applyRestore(values) },
        ).also { it.showRespectingStatusBar() }
    }

    private fun applyRestore(values: Map<String, Any>) {
        val appContext = fragment.requireContext().applicationContext
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                PrefsBackup.restore(appContext, values)
            }
            if (!success) {
                appContext.showToast(R.string.restore_failed)
                return@launch
            }

            if (Prefs(appContext).dailyWallpaper) viewModel.setWallpaperWorker()
            else viewModel.cancelWallpaperWorker()

            appContext.showToast(R.string.restore_done)
            fragment.requireActivity().recreate()
        }
    }

    private fun suggestedFileName(): String =
        "olauncher-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json"
}
