package ru.iuturakulov.mybudget.ui.projects.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.data.remote.dto.NotificationType
import ru.iuturakulov.mybudget.data.remote.dto.ParticipantRole
import ru.iuturakulov.mybudget.databinding.FragmentProjectNotificationsBinding
import ru.iuturakulov.mybudget.ui.BaseBottomSheetDialogFragment

@AndroidEntryPoint
class ProjectNotificationsBottomSheet(
    private val userRole: ParticipantRole,
    private val projectId: String
) : BaseBottomSheetDialogFragment<FragmentProjectNotificationsBinding>(
    R.layout.fragment_project_notifications
) {

    private val viewModel: ProjectNotificationsViewModel by viewModels()
    private var adapter: NotificationSettingsAdapter? = null

    private val allNotificationTypes: List<NotificationType> by lazy {
        NotificationType.entries
            .filter { it != NotificationType.UNKNOWN }
            .sortedWith(
                compareBy(
                    { it.category().ordinal },
                    { getString(it.titleRes()) }
                ))
    }

    override fun getViewBinding(view: View) = FragmentProjectNotificationsBinding.bind(view)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initNotifications(projectId)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setupViews() {
        adapter = NotificationSettingsAdapter { type, enabled ->
            // разрешаем/запрещаем конкретный тип
            viewModel.toggle(projectId = projectId, type = type, enabled = enabled)
        }

        binding.rvNotificationTypes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ProjectNotificationsBottomSheet.adapter
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }

        // Фильтруем в зависимости от роли
        val allowedTypes = permissionsByRole[userRole]!!.toList()
        adapter?.updateItems(allowedTypes)
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // получаем текущие prefs и передаём в адаптер
                viewModel.prefs.collect { enabledSet ->
                    adapter?.setEnabled(enabledSet)
                }
            }
        }
    }

    companion object {
        const val TAG = "ProjectNotificationsBottomSheet"
    }
}