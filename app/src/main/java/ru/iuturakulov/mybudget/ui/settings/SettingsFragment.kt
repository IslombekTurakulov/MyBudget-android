package ru.iuturakulov.mybudget.ui.settings

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentSettingsBinding
import ru.iuturakulov.mybudget.domain.models.UserSettings
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()

    override fun getViewBinding(view: View) = FragmentSettingsBinding.bind(view)

    override fun setupViews() {
        setupLanguageSelector()

        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleDarkTheme(isChecked)
        }

        binding.changeUsernameCard.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    override fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.userSettings.collect { settings ->
                settings?.let { updateUI(it) }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.message.collect { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.fetchUserSettings()
    }

    private fun setupLanguageSelector() {
        val languages = resources.getStringArray(R.array.languages)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.adapter = adapter
        binding.spinnerLanguage.setSelection(0, false)
        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.saveUserSettings(
                        settings = UserSettings(
                            name = binding.tvUserName.text.toString(),
                            email = binding.tvUserEmail.text.toString(),
                            language = parent.getItemAtPosition(position) as? String ?: "ru",
                            notificationsEnabled = binding.switchNotifications.isChecked
                        )
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // no-op
                }
            }
    }

    private fun updateUI(settings: UserSettings) {
        binding.tvUserName.text = settings.name
        binding.tvUserEmail.text = settings.email
        binding.switchNotifications.isChecked = settings.notificationsEnabled

        val languages = resources.getStringArray(R.array.languages)
        val index = languages.indexOf(settings.language)
        binding.spinnerLanguage.setSelection(if (index >= 0) index else 0, true)
    }


    private fun showChangePasswordDialog() {
        val action = SettingsFragmentDirections
            .actionSettingsToChangepassword(binding.tvUserEmail.text.toString())
        findNavController().navigate(action)
    }

    private fun showChangeUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_username, null)
        val newUsername = dialogView.findViewById<TextInputEditText>(R.id.etNewUserName)
        val etNameInputLayout = dialogView.findViewById<TextInputLayout>(R.id.etNewNameInputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Сменить") { _, _ ->
                val name = newUsername.text?.trim().toString()

                fun showError(inputLayout: TextInputLayout, error: String?): Boolean {
                    inputLayout.error = error
                    inputLayout.isErrorEnabled = error != null
                    if (error != null) inputLayout.requestFocus()
                    return error == null
                }

                if (!showError(
                        inputLayout = etNameInputLayout,
                        error = "Имя не может быть пустым".takeIf { name.isBlank() }
                    )
                ) {
                    return@setPositiveButton
                }

                if (!showError(
                        inputLayout = etNameInputLayout,
                        error = "Имя должен быть больше 4 символов!".takeIf { name.trim().length < 4 }
                    )
                ) {
                    return@setPositiveButton
                }

                viewModel.saveUserSettings(
                    settings = UserSettings(
                        name = name,
                        email = binding.tvUserEmail.text.toString(),
                        language = binding.spinnerLanguage.selectedItem as? String ?: "ru",
                        notificationsEnabled = binding.switchNotifications.isChecked
                    )
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

