package ru.iuturakulov.mybudget.ui.projects.participants

import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentProjectParticipantsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ProjectParticipantsFragment :
    BaseFragment<FragmentProjectParticipantsBinding>(R.layout.fragment_project_participants) {
    override fun getViewBinding(view: View): FragmentProjectParticipantsBinding {
        return FragmentProjectParticipantsBinding.bind(view)
    }
}
