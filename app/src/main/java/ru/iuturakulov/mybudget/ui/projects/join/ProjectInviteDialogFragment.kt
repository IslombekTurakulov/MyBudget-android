package ru.iuturakulov.mybudget.ui.projects.join

import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentProjectInviteBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ProjectInviteDialogFragment :
    BaseFragment<FragmentProjectInviteBinding>(R.layout.fragment_project_invite) {
    override fun getViewBinding(view: View): FragmentProjectInviteBinding {
        return FragmentProjectInviteBinding.bind(view)
    }
}