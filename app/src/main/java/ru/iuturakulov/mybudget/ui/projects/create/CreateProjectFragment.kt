package ru.iuturakulov.mybudget.ui.projects.create

import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentProjectCreateBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class CreateProjectFragment :
    BaseFragment<FragmentProjectCreateBinding>(R.layout.fragment_project_create) {
    override fun getViewBinding(view: View): FragmentProjectCreateBinding {
        return FragmentProjectCreateBinding.bind(view)
    }
}
