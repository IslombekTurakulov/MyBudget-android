package ru.iuturakulov.mybudget.ui.changePassword

import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentChangePasswordBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ChangePasswordFragment :
    BaseFragment<FragmentChangePasswordBinding>(R.layout.fragment_change_password) {

    private val viewModel: ChangePasswordViewModel by viewModels()
    override fun getViewBinding(view: View): FragmentChangePasswordBinding {
        return FragmentChangePasswordBinding.bind(view)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangePassword.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            val etOldPassword = binding.etOldPassword.text?.toString().orEmpty().trim()
            val etNewPassword = binding.etNewPassword.text?.toString().orEmpty().trim()

            if (validateInputs(email, etOldPassword, etNewPassword)) {
                viewModel.changePassword(email, etOldPassword, etNewPassword)
            }
        }
    }

    private fun validateInputs(
        email: String,
        etOldPassword: String,
        etNewPassword: String
    ): Boolean {
        fun showError(inputLayout: TextInputLayout, error: String?): Boolean {
            inputLayout.error = error
            inputLayout.isErrorEnabled = error != null
            if (error != null) inputLayout.requestFocus()
            return error == null
        }

        if (!showError(
                inputLayout = binding.etEmailInputLayout,
                error = when {
                    email.isBlank() -> "Email не может быть пустым"
                    !Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches() -> "Некорректный Email"

                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etOldPasswordInputLayout,
                error = when {
                    etOldPassword.isBlank() -> "Пароль не может быть пустым"
                    etOldPassword.length < 6 -> "Пароль должен содержать минимум 6 символов"
                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etNewPasswordInputLayout,
                error = when {
                    etNewPassword.isBlank() -> "Пароль не может быть пустым"
                    etNewPassword.length < 6 -> "Пароль должен содержать минимум 6 символов"
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
            binding.btnChangePassword.isEnabled =
                state !is ChangePasswordViewModel.ChangePasswordState.Loading
            binding.progressBar.isVisible =
                state is ChangePasswordViewModel.ChangePasswordState.Loading

            when (state) {
                is ChangePasswordViewModel.ChangePasswordState.Success -> {
                    Snackbar.make(binding.root, "Успех! Проверьте почту", Snackbar.LENGTH_LONG)
                        .show()
                    findNavController().navigateUp()
                }

                is ChangePasswordViewModel.ChangePasswordState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                is ChangePasswordViewModel.ChangePasswordState.Loading -> {
                    // Отобразить прогресс
                }
            }
        }
    }
}