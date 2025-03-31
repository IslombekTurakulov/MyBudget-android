package ru.iuturakulov.mybudget.ui.changePassword

import android.util.Patterns
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentChangePasswordBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ChangePasswordFragment :
    BaseFragment<FragmentChangePasswordBinding>(R.layout.fragment_change_password) {

    private val viewModel: ChangePasswordViewModel by viewModels()

    private val args: ChangePasswordFragmentArgs by navArgs()

    override fun getViewBinding(view: View): FragmentChangePasswordBinding =
        FragmentChangePasswordBinding.bind(view)

    override fun setupViews() {
        args.email?.let {
            binding.etEmail.setText(args.email)
            binding.etEmail.isEnabled = false
        }
        binding.apply {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            btnChangePassword.setOnClickListener {
                val email = etEmail.text?.toString().orEmpty().trim()
                val oldPassword = etOldPassword.text?.toString().orEmpty().trim()
                val newPassword = etNewPassword.text?.toString().orEmpty().trim()

                if (validateInputs(email, oldPassword, newPassword)) {
                    viewModel.changePassword(email, oldPassword, newPassword)
                }
            }
            // Автоматическая очистка ошибок при вводе
            etEmail.doOnTextChanged { _, _, _, _ ->
                etEmailInputLayout.error = null
            }
            etOldPassword.doOnTextChanged { _, _, _, _ ->
                etOldPasswordInputLayout.error = null
            }
            etNewPassword.doOnTextChanged { _, _, _, _ ->
                etNewPasswordInputLayout.error = null
            }
        }
    }

    private fun validateInputs(email: String, oldPassword: String, newPassword: String): Boolean {
        fun showError(
            inputLayout: com.google.android.material.textfield.TextInputLayout,
            error: String?
        ): Boolean {
            inputLayout.error = error
            inputLayout.isErrorEnabled = error != null
            if (error != null) inputLayout.requestFocus()
            return error == null
        }

        if (!showError(
                inputLayout = binding.etEmailInputLayout,
                error = when {
                    email.isBlank() -> getString(R.string.error_empty_email)
                    !Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches() -> getString(R.string.error_invalid_email)

                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etOldPasswordInputLayout,
                error = when {
                    oldPassword.isBlank() -> getString(R.string.error_empty_password)
                    oldPassword.length < 6 -> getString(R.string.error_short_password)
                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etNewPasswordInputLayout,
                error = when {
                    newPassword.isBlank() -> getString(R.string.error_empty_password)
                    newPassword.length < 6 -> getString(R.string.error_short_password)
                    else -> null
                }
            )
        ) {
            return false
        }
        return true
    }

    override fun setupObservers() {
        viewModel.changePasswordState.observe(viewLifecycleOwner) { state ->
            binding.apply {
                btnChangePassword.isEnabled =
                    state !is ChangePasswordViewModel.ChangePasswordState.Loading
                progressBar.isVisible = state is ChangePasswordViewModel.ChangePasswordState.Loading
            }

            when (state) {
                is ChangePasswordViewModel.ChangePasswordState.Success -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.password_change_success),
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                    findNavController().navigateUp()
                }

                is ChangePasswordViewModel.ChangePasswordState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .show()
                }

                is ChangePasswordViewModel.ChangePasswordState.Loading -> {
                    // Прогресс-бар уже отображается
                }
            }
        }
    }
}
