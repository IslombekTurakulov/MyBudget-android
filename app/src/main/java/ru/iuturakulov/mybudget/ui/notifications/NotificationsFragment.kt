package ru.iuturakulov.mybudget.ui.notifications

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                if (!item.isRead) {
                    vm.markRead(item.id)
                }
                
                when (item.type) {
                    NotificationType.PROJECT_INVITE_SEND,
                    NotificationType.PROJECT_INVITE_ACCEPT,
                    NotificationType.PARTICIPANT_ROLE_CHANGE,
                    NotificationType.PARTICIPANT_REMOVED,
                    NotificationType.PROJECT_EDITED,
                    NotificationType.PROJECT_REMOVED,
                    NotificationType.PROJECT_ARCHIVED,
                    NotificationType.PROJECT_UNARCHIVED,
                    NotificationType.TRANSACTION_ADDED,
                    NotificationType.TRANSACTION_UPDATED,
                    NotificationType.TRANSACTION_REMOVED -> {
                        item.projectId?.let { projectId ->
                            val action = NotificationsFragmentDirections.actionNotificationsProjectsToDetails(
                                projectId = projectId
                            )
                            findNavController().navigate(action)
                        }
                    }
                    NotificationType.SYSTEM_ALERT -> {
                        Snackbar.make(
                            binding.root,
                            item.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Snackbar.make(
                            binding.root,
                            item.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onLong = { item ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_delete_notification_title)
                    .setMessage(R.string.confirm_delete_notification_message)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { list ->
                    when (list) {
                        is UiState.Loading -> binding.swipeRefreshLayout.isRefreshing = true
                        is UiState.Success -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            val data = list.data.orEmpty()
                            binding.rvNotifications.isVisible = data.isNotEmpty()
                            binding.emptyRootContainer.isVisible = data.isEmpty()
                            adapter.submitList(data)
                        }

                        is UiState.Error -> {
                            Snackbar.make(
                                binding.root,
                                list.message ?: getString(R.string.error_loading_data),
                                Snackbar.LENGTH_LONG
                            ).show()
                            binding.rvNotifications.isGone = true
                            binding.emptyRootContainer.isGone = false
                        }

                        else -> { /* no-op */
                        }
                    }
                }
            }
        }
    }
}
