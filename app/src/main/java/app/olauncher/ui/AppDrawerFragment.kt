package app.olauncher.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.folders.ARG_FOLDER_EXTERNAL
import app.olauncher.folders.ARG_FOLDER_ID
import app.olauncher.folders.ARG_PICKER_CURRENT_FOLDER
import app.olauncher.folders.ARG_PICKER_ITEM_ID
import app.olauncher.folders.FOLDER_PICKER_RESULT
import app.olauncher.folders.FolderDrawerController
import app.olauncher.folders.isFolderMarker
import app.olauncher.folders.itemIdOf
import app.olauncher.helper.deletePinnedShortcut
import app.olauncher.helper.getAppsList
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openSearch
import app.olauncher.helper.openUrl
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast
import app.olauncher.helper.uninstall
import kotlinx.coroutines.launch

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var folderController: FolderDrawerController
    private var defaultQueryHint: CharSequence? = ""
    private var folderBackCallback: OnBackPressedCallback? = null
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false
    private var folderApps: List<AppModel> = emptyList()

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

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
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        folderController = FolderDrawerController(requireContext())
        arguments?.getString(ARG_FOLDER_ID)?.let { folderId ->
            folderController.open(
                folderId,
                external = arguments?.getBoolean(ARG_FOLDER_EXTERNAL, false) ?: false,
            )
        }
        restorePickerFolder()

        initViews()
        defaultQueryHint = binding.search.queryHint
        updateSearchHint()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
        registerFolderBackCallback()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = "Please select an app"
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.allowAutoLaunch = !isSearchComposing()
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag // e.g. "zh-CN", "ja-JP", "en-US"
                else -> subtype.locale // deprecated fallback, e.g. "zh_CN"
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = { appModel ->
                if (flag == Constants.FLAG_LAUNCH_APP && folderController.openFolderRow(appModel)) {
                    loadFolderApps()
                    onFolderChanged(resetScroll = true)
                } else {
                    viewModel.selectedApp(appModel, flag)
                    if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                        findNavController().popBackStack(R.id.mainFragment, false)
                    else
                        closeDrawer()
                }
            },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                // Las carpetas no se desinstalan ni tienen información de app
                if (isFolderMarker(appModel.appPackage)) return@AppDrawerAdapter
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (appModel.user != Process.myUserHandle()) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (folderController.isInsideFolder) {
                    folderController.removeFromCurrentFolder(appModel)
                    onFolderChanged()
                    return@AppDrawerAdapter
                }
                if (isFolderMarker(appModel.appPackage)) {
                    folderController.removeFolderFromDrawer(appModel)
                    onFolderChanged()
                    return@AppDrawerAdapter
                }
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    closeDrawer()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                if (isFolderMarker(appModel.appPackage)) {
                    if (!folderController.renameFolder(appModel.appPackage, renameLabel)) {
                        requireContext().showToast(getString(R.string.folder_name_exists))
                    }
                    onFolderChanged()
                    return@AppDrawerAdapter
                }
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.identity
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
        )
        adapter.folderContext = folderController.isInsideFolder
        adapter.onFolderAction = { openFolderPicker(it) }

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay())
            binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        else if (requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val extraApps = if (flag == Constants.FLAG_LAUNCH_APP && !currentPrivateSpaceLocked)
            currentPrivateSpaceApps.orEmpty() else emptyList()
        // Dentro de una carpeta se resuelven también las apps ocultas en la vista principal
        val resolveApps = if (folderController.isInsideFolder && folderApps.isNotEmpty())
            folderApps else apps
        val combined = folderController.visibleItems(resolveApps, extraApps, flag)

        // El espacio privado solo se muestra en la raíz, no dentro de una carpeta
        if (flag == Constants.FLAG_LAUNCH_APP && !folderController.isInsideFolder &&
            currentPrivateSpaceAvailable
        ) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        adapter.setAppList(combined)
        adapter.filter.filter(binding.search.query)
    }

    private fun registerFolderBackCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (folderController.goUp()) {
                    onFolderChanged(resetScroll = true)
                } else {
                    isEnabled = false
                    closeDrawer()
                }
            }
        }
        callback.isEnabled = folderController.canHandleBack()
        folderBackCallback = callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun onFolderChanged(resetScroll: Boolean = false) {
        adapter.folderContext = folderController.isInsideFolder
        binding.search.setQuery("", false)
        updateSearchHint()
        folderBackCallback?.isEnabled = folderController.canHandleBack()
        updateCombinedAppList()
        if (resetScroll) resetListToTop()
    }

    private fun resetListToTop() {
        binding.recyclerView.stopScroll()
        linearLayoutManager.scrollToPositionWithOffset(0, 0)
        // La lista se actualiza de forma asíncrona: se repite en el siguiente frame
        binding.recyclerView.post {
            if (_binding != null) linearLayoutManager.scrollToPositionWithOffset(0, 0)
        }
    }

    private fun loadFolderApps() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            folderApps = getAppsList(
                context = appContext,
                prefs = prefs,
                includeRegularApps = true,
                includeHiddenApps = true,
            )
            if (folderController.isInsideFolder) updateCombinedAppList()
        }
    }

    private fun restorePickerFolder() {
        val entry = findNavController().currentBackStackEntry ?: return
        val folderId = entry.savedStateHandle.get<String>(FOLDER_PICKER_RESULT) ?: return
        entry.savedStateHandle.remove<String>(FOLDER_PICKER_RESULT)
        folderController.openPathTo(folderId)
    }

    private fun openFolderPicker(appModel: AppModel) {
        val itemId = itemIdOf(appModel) ?: return
        val args = bundleOf(ARG_PICKER_ITEM_ID to itemId)
        folderController.currentFolderId?.let { args.putString(ARG_PICKER_CURRENT_FOLDER, it) }
        findNavController().navigate(
            R.id.action_appListFragment_to_folderPickerFragment,
            args,
        )
    }

    private fun updateSearchHint() {
        val folderName = folderController.currentFolderName()
        if (folderName.isNullOrBlank()) {
            binding.folderPath.visibility = View.GONE
        } else {
            binding.folderPath.text = folderName
            binding.folderPath.visibility = View.VISIBLE
        }
        binding.search.queryHint = defaultQueryHint ?: ""
    }

    private fun initClickListeners() {
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            closeDrawer()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        if (!closeDrawer()) return
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    // Pops only the drawer. A plain popBackStack() from a late overscroll frame could pop the
    // home screen too, leaving a drawer that auto-launch and back can't close (#766)
    private fun closeDrawer() = findNavController().popBackStack(R.id.appListFragment, true)

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        restorePickerFolder()
        folderController.reload()
        adapter.folderContext = folderController.isInsideFolder
        updateSearchHint()
        folderBackCallback?.isEnabled = folderController.canHandleBack()
        updateCombinedAppList()
        if (folderController.isInsideFolder) loadFolderApps()
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        folderBackCallback?.isEnabled = false
        folderBackCallback = null
        searchTextView = null
        _binding = null
    }
}
