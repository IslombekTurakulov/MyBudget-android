package ru.iuturakulov.mybudget.ui.resetPassword

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.DialogVerificationCodeBinding
import ru.iuturakulov.mybudget.databinding.FragmentResetPasswordBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ResetPasswordFragment : BaseFragment<FragmentResetPasswordBinding>(R.layout.fragment_reset_password) {

    private val viewModel: ResetPasswordViewModel by viewModels()

    private var resetDialog: AlertDialog? = null

    override fun getViewBinding(view: View): FragmentResetPasswordBinding {
        return FragmentResetPasswordBinding.bind(view)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            if (email.isNotBlank()) {
                viewModel.resetPassword(email)
            } else {
                binding.etEmailInputLayout.error = "Введите Email"
            }
        }
    }

    override fun setupObservers() {
        viewModel.resetPasswordState.observe(viewLifecycleOwner) { state ->
            binding.btnResetPassword.isEnabled =
                state !is ResetPasswordViewModel.ResetPasswordState.Loading
            binding.progressBar.isVisible =
                state is ResetPasswordViewModel.ResetPasswordState.Loading

            when (state) {
                is ResetPasswordViewModel.ResetPasswordState.PasswordReset -> {
                    // Переход на экран смены пароля
                    resetDialog?.dismiss()
                    Snackbar.make(binding.root, getString(R.string.password_change_success), Snackbar.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }

                is ResetPasswordViewModel.ResetPasswordState.CodeSent -> {
                    // Показываем диалог с PinView для ввода кода
                    showVerificationDialog()
                    Snackbar.make(binding.root, getString(R.string.verification_code_sent_success), Snackbar.LENGTH_LONG).show()
                    binding.etEmailInputLayout.isEnabled = false
                    binding.btnResetPassword.isEnabled = false
                }

                is ResetPasswordViewModel.ResetPasswordState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                is ResetPasswordViewModel.ResetPasswordState.Loading -> {
                    // Отобразить индикатор загрузки
                }
            }
        }
    }

    private fun showVerificationDialog() {
        val dialogBinding = DialogVerificationCodeBinding.inflate(layoutInflater)

        resetDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.enter_verification_code))  // Локализованный заголовок
            .setView(dialogBinding.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.submit), null)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                binding.etEmailInputLayout.isEnabled = true
                binding.btnResetPassword.isEnabled = true
            }
            .show()

        val pinView = dialogBinding.pinView
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        pinView.requestFocus()
        imm.showSoftInput(pinView, InputMethodManager.SHOW_IMPLICIT)

        resetDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.let { positiveButton ->
            positiveButton.setOnClickListener {
                val code = pinView.text.toString().trim()
                if (code.isNotBlank()) {
                    // Вызываем сброс пароля с кодом
                    viewModel.resetPassword(binding.etEmail.text.toString().trim(), code)
                } else {
                    pinView.error = getString(R.string.code_error)
                }
            }
        }
    }
}