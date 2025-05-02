package ru.iuturakulov.mybudget.ui.notifications

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.core.UiState
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.databinding.FragmentNotificationsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.projects.list.ProjectListFragmentDirections

@AndroidEntryPoint
class NotificationsFragment :
    BaseFragment<FragmentNotificationsBinding>(R.layout.fragment_notifications) {

    private val vm: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun getViewBinding(v: View) = FragmentNotificationsBinding.bind(v)

    override fun setupViews() = with(binding) {
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        swipeRefreshLayout.setOnRefreshListener { vm.refresh() }

        adapter = NotificationsAdapter(
            onClick = { item ->
                if (!item.isRead) vm.markRead(item.id)
                if (item.type == NotificationType.PROJECT_EDITED || item.type == NotificationType.TRANSACTION_ADDED || item.type == NotificationType.TRANSACTION_REMOVED) {
                    val action = NotificationsFragmentDirections.actionNotificationsProjectsToDetails(projectId = item.projectId!!)
                    findNavController().navigate(action)
                }
            },
            onLong = { item ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_delete_transaction_title)
                    .setMessage(R.string.confirm_delete_transaction_message)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        vm.remove(item.id)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        ).apply {
            setHasStableIds(true)
        }
        rvNotifications.adapter = adapter
    }

    override fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            vm.state.collect { list ->
                when (list) {
                    is UiState.Loading -> binding.swipeRefreshLayout.isRefreshing = true
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val data = list.data.orEmpty()
                        binding.rvNotifications.isVisible      = data.isNotEmpty()
                        binding.emptyNotifications.root.isVisible = data.isEmpty()
                        adapter.submitList(data)
                    }
                    is UiState.Error -> binding.swipeRefreshLayout.isRefreshing = false
                    else              -> { /* no-op */ }
                }
            }
        }
    }
}
