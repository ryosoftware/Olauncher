package app.olauncher.folders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R

/** Lista de carpetas del selector; «New folder» siempre es el último elemento. */
class FolderPickerAdapter(
    private val onFolderClick: (FolderDestination) -> Unit,
    private val onMoveToRootClick: () -> Unit,
    private val onNewFolderClick: () -> Unit,
) : ListAdapter<FolderPickerAdapter.Row, FolderPickerAdapter.ViewHolder>(DIFF) {

    sealed class Row {
        data class Folder(val destination: FolderDestination) : Row()
        data object MoveToRoot : Row()
        data object NewFolder : Row()
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean = when {
                oldItem is Row.Folder && newItem is Row.Folder ->
                    oldItem.destination.id == newItem.destination.id

                oldItem is Row.MoveToRoot && newItem is Row.MoveToRoot -> true
                oldItem is Row.NewFolder && newItem is Row.NewFolder -> true
                else -> false
            }

            override fun areContentsTheSame(oldItem: Row, newItem: Row): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_folder_picker_row, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    fun submitDestinations(destinations: List<FolderDestination>, showMoveToRoot: Boolean) {
        val rows = buildList {
            if (showMoveToRoot) add(Row.MoveToRoot)
            addAll(destinations.map { Row.Folder(it) })
            add(Row.NewFolder)
        }
        submitList(rows)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label = view as TextView

        fun bind(row: Row) {
            when (row) {
                is Row.Folder -> {
                    label.text = row.destination.path
                    label.setOnClickListener { onFolderClick(row.destination) }
                }

                is Row.MoveToRoot -> {
                    label.setText(R.string.move_to_root)
                    label.setOnClickListener { onMoveToRootClick() }
                }

                is Row.NewFolder -> {
                    label.setText(R.string.new_folder)
                    label.setOnClickListener { onNewFolderClick() }
                }
            }
        }
    }
}
