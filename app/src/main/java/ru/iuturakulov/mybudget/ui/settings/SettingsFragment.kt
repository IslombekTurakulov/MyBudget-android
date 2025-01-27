package ru.iuturakulov.mybudget.ui.settings

import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentSettingsBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    override fun getViewBinding(view: View): FragmentSettingsBinding {
        return FragmentSettingsBinding.bind(view)
    }

    override fun setupViews() {
        setupToolbar()
    }

    private fun setupToolbar() {
    }
}
