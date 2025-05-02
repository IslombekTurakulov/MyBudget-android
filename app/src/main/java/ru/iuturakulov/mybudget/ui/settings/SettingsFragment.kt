package ru.iuturakulov.mybudget.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Patterns
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
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

        binding.tilNotifications.setOnClickListener {
            binding.switchNotifications.isChecked = !binding.switchNotifications.isChecked
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.saveUserSettings(
                settings = viewModel.userSettings.value!!.copy(
                    notificationsEnabled = isChecked
                )
            )
        }

        binding.profileCard.setOnClickListener {
            showChangeUsernameDialog()
        }

        binding.tilChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.tilLogout.setOnClickListener {
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

        binding.tvCurrentHost.text = getString(R.string.current_host_label, viewModel.host.value)
        binding.tvDebugChangeHost.setOnClickListener {
            showChangeHostDialog()
        }
    }

    private fun showChangeHostDialog() {
        val dlgView = layoutInflater.inflate(R.layout.dialog_change_host, null)
        val inputLayout = dlgView.findViewById<TextInputLayout>(R.id.inputLayoutHost)
        val etHost = dlgView.findViewById<TextInputEditText>(R.id.etHost)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.debug_change_host)
            .setView(dlgView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnSave.isEnabled = false

            etHost.addTextChangedListener {
                val url = it?.toString().orEmpty().trim()
                val ok = Patterns.WEB_URL.matcher(url).matches()
                inputLayout.error =
                    if (!ok && url.isNotEmpty()) getString(R.string.error_invalid_host) else null
                btnSave.isEnabled = ok
            }

            btnSave.setOnClickListener {
                btnSave.text = getString(R.string.saving)
                btnSave.isEnabled = false

                val newHost = etHost.text!!.toString().trim()
                viewModel.updateHost(newHost)

                dialog.dismiss()
                Snackbar.make(
                    binding.root,
                    getString(R.string.debug_host_changed, newHost),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        etHost.setText(viewModel.host.value)
        dialog.show()
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
        binding.tilChangeLanguage.setOnClickListener {
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

                setAppLocale(requireContext(), selectedLanguageCode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

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
        binding.tvUserName.text = settings.name
        binding.tvUserEmail.text = settings.email
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
        val etNameInputLayout =
            dialogView.findViewById<TextInputLayout>(R.id.etNewNameInputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_change_username_title)
            .setView(dialogView)
            .setPositiveButton(R.string.change) { _, _ ->
                val name = newUsername.text?.trim().toString()

                fun showError(inputLayout: TextInputLayout, @StringRes errRes: Int?): Boolean {
                    if (errRes != null) {
                        inputLayout.error = getString(errRes)
                        inputLayout.isErrorEnabled = true
                        inputLayout.requestFocus()
                        return false
                    } else {
                        inputLayout.error = null
                        inputLayout.isErrorEnabled = false
                        return true
                    }
                }

                if (!showError(
                        etNameInputLayout,
                        if (name.isBlank()) R.string.error_empty_username else null
                    )
                ) return@setPositiveButton

                if (!showError(
                        etNameInputLayout,
                        if (name.length < 4) R.string.error_username_too_short else null
                    )
                ) return@setPositiveButton

                val settings = viewModel.userSettings.value!!
                viewModel.saveUserSettings(
                    settings.copy(
                        name = name,
                        email = settings.email,
                        language = settings.language,
                        notificationsEnabled = settings.notificationsEnabled,
                        darkThemeEnabled = false, // settings.darkThemeEnabled,
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

