package ru.iuturakulov.mybudget.ui.projects.join

import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentProjectAcceptInviteBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ProjectAcceptInviteDialogFragment :
    BaseFragment<FragmentProjectAcceptInviteBinding>(R.layout.fragment_project_accept_invite) {
    override fun getViewBinding(view: View): FragmentProjectAcceptInviteBinding {
        return FragmentProjectAcceptInviteBinding.bind(view)
    }
}