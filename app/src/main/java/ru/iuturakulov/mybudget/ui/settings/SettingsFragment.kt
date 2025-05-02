package ru.iuturakulov.mybudget.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
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
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()

    override fun getViewBinding(view: View) = FragmentSettingsBinding.bind(view)

    override fun setupViews() {
        setupLanguageSelector()

//        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
//            viewModel.toggleDarkTheme(isChecked)
//        }

        binding.profileCard.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout_button) { _, _ ->
                    viewModel.logout()
                    findNavController().navigate(R.id.action_settings_to_login)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.userSettings.collect { settings ->
                binding.profileCard.isGone = settings == null
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
        updateSelectedLanguage(Locale.getDefault().language)
        binding.tvSelectedLanguage.setOnClickListener {
            showLanguageSelectionDialog(languages)
        }
    }

    private fun updateSelectedLanguage(languageCode: String) {
        val languages = resources.getStringArray(R.array.languages)
        val languageCodes = resources.getStringArray(R.array.language_codes)

        val index = languageCodes.indexOf(languageCode)
        val displayLanguage = if (index >= 0) languages[index] else languages[0]
        binding.tvSelectedLanguage.text = displayLanguage
    }

    private fun showLanguageSelectionDialog(languages: Array<String>) {
        val languageCodes = resources.getStringArray(R.array.language_codes)
        val currentIndex = languageCodes.indexOf(Locale.getDefault().language).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                viewModel.saveCurrentLocale(selectedLanguageCode)
                viewModel.saveUserSettings(
                    settings = UserSettings(
                        name = binding.tvUserName.text.toString(),
                        email = binding.tvUserEmail.text.toString(),
                        language = selectedLanguageCode,
                        notificationsEnabled = binding.switchNotifications.isChecked,
                        darkThemeEnabled = false, // binding.switchDarkTheme.isChecked
                    )
                )
                updateSelectedLanguage(selectedLanguageCode)
                dialog.dismiss()

                // Здесь можно добавить смену языка приложения
                setAppLocale(requireContext(), selectedLanguageCode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @SuppressLint("NewApi")
    private fun setAppLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        resources.updateConfiguration(configuration, resources.displayMetrics)

        requireActivity().recreate()
    }

    private fun updateUI(settings: UserSettings) {
        binding.tvUserName.setText(settings.name)
        binding.tvUserEmail.setText(settings.email)
        binding.switchNotifications.isChecked = settings.notificationsEnabled
//        binding.switchDarkTheme.isChecked = settings.darkThemeEnabled
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

                val languages = resources.getStringArray(R.array.languages)
                val languageCodes = resources.getStringArray(R.array.language_codes)
                val index = languages.indexOf(binding.tvSelectedLanguage.text?.toString())
                val displayLanguage = if (index >= 0) languageCodes[index] else languageCodes[0]

                viewModel.saveUserSettings(
                    settings = UserSettings(
                        name = name,
                        email = binding.tvUserEmail.text.toString(),
                        language = displayLanguage,
                        notificationsEnabled = binding.switchNotifications.isChecked,
                        darkThemeEnabled = false // binding.switchDarkTheme.isChecked
                    )
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

