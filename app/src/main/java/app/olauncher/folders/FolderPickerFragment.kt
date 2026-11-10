package app.olauncher.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.olauncher.R
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.appLabelMatches
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.showToast
import app.olauncher.ui.BaseFragment

/**
 * Pantalla de selección de carpeta, con la misma presentación que el App Drawer.
 * Muestra las rutas completas (p. ej. `Trabajo/Proyectos`) y «New folder» al final.
 */
class FolderPickerFragment : BaseFragment() {

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: FolderRepository
    private lateinit var adapter: FolderPickerAdapter
    private lateinit var itemId: String
    private var currentFolderId: String? = null
    private var destinations: List<FolderDestination> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = FolderRepository(requireContext())

        itemId = arguments?.getString(ARG_PICKER_ITEM_ID) ?: run {
            findNavController().popBackStack()
            return
        }
        currentFolderId = arguments?.getString(ARG_PICKER_CURRENT_FOLDER)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = finishAndRestore()
            },
        )

        adapter = FolderPickerAdapter(
            onFolderClick = { addToFolder(it.id) },
            onNewFolderClick = { showNewFolderDialog() },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
        binding.search.queryHint = getString(R.string.add_to_folder)
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.search.hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                applyFilter(newText)
                return true
            }
        })

        loadDestinations()
    }

    private fun loadDestinations() {
        destinations = repository.destinationsFor(itemId)
        applyFilter(binding.search.query.toString())
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) destinations
        else destinations.filter { appLabelMatches(it.path, query) }
        adapter.submitDestinations(filtered)
    }

    private fun addToFolder(folderId: String) {
        handleResult(repository.addItemToFolder(folderId, itemId))
    }

    private fun showNewFolderDialog() {
        showFolderNameDialog(requireContext()) { name ->
            handleResult(repository.createRootFolderAndAddItem(name, itemId))
        }
    }

    private fun handleResult(result: FolderStore.AddResult) {
        when (result) {
            FolderStore.AddResult.WOULD_CREATE_CYCLE,
            FolderStore.AddResult.FOLDER_NOT_FOUND -> {
                requireContext().showToast(getString(R.string.folder_move_failed))
                loadDestinations()
            }

            else -> finishAndRestore()
        }
    }

    private fun finishAndRestore() {
        val previous = findNavController().previousBackStackEntry
        val folderId = currentFolderId
        if (folderId != null) previous?.savedStateHandle?.set(FOLDER_PICKER_RESULT, folderId)
        else previous?.savedStateHandle?.remove<String>(FOLDER_PICKER_RESULT)
        findNavController().popBackStack()
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
