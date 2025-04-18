package ru.iuturakulov.mybudget.ui.notifications

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentNotificationsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment
import ru.iuturakulov.mybudget.ui.notifications.NotificationsViewModel.NotificationsUiState

@AndroidEntryPoint
class NotificationsFragment :
    BaseFragment<FragmentNotificationsBinding>(R.layout.fragment_notifications) {

    private val vm: NotificationsViewModel by viewModels()
    // private val adapter by lazy { NotificationsAdapter(::onClick) }

    override fun getViewBinding(v: View) = FragmentNotificationsBinding.bind(v)

    override fun setupViews() = with(binding) {
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
      //  recyclerView.adapter = adapter
        swipeRefreshLayout.setOnRefreshListener { vm.refresh() }
    }

    override fun setupObservers() {
//        with(binding) {
//            lifecycleScope.launchWhenStarted {
//                vm.state.collectLatest { state ->
//                    when (state) {
//                        is NotificationsUiState.Loading -> swipeRefreshLayout.isRefreshing = true
//                        is NotificationsUiState.Error -> {
//                            swipeRefreshLayout.isRefreshing = false
//                            Snackbar.make(root, state.t.message ?: "Ошибка", Snackbar.LENGTH_LONG)
//                                .show()
//                        }
//
//                        is NotificationsUiState.Success -> {
//                            swipeRefreshLayout.isRefreshing = false
//                            adapter.submitList(state.list)
//                            emptyState.isVisible = state.list.isEmpty()
//                        }
//
//                        is NotificationsUiState.Empty -> {
//
//                        }
//                    }
//                }
//            }
//
//            lifecycleScope.launchWhenStarted {
//                vm.event.collect { event ->
//                    if (event is Event.ShowError) {
//                        Snackbar.make(
//                            requireView(),
//                            event.t.message ?: "Ошибка", Snackbar.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
    }

    private fun onClick(item: NotificationsViewModel.NotificationUi) {
        if (!item.read) vm.markAsRead(item.id)
        // Навигация на детальный экран при желании
    }
}

