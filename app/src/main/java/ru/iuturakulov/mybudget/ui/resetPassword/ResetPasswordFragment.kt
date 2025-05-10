package ru.iuturakulov.mybudget.ui.resetPassword

import android.content.DialogInterface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
                binding.etEmailInputLayout.error = getString(R.string.error_invalid_email)
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
                    resetDialog?.dismiss()
                    resetDialog = null
                    Snackbar.make(binding.root, getString(R.string.password_change_success), Snackbar.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }

                is ResetPasswordViewModel.ResetPasswordState.CodeSent -> {
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
            .setTitle(getString(R.string.enter_verification_code))
            .setView(dialogBinding.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.submit), null)
            .setNegativeButton(getString(R.string.cancel)) { dlg, _ ->
                dlg.dismiss()
                resetDialog = null
                // Восстанавливаем доступность полей
                binding.etEmailInputLayout.isEnabled = true
                binding.btnResetPassword.isEnabled = true
            }
            .create()

        resetDialog?.setOnShowListener {
            val pinView = dialogBinding.pinView

            resetDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setOnClickListener {
                    val code = pinView.text.toString().trim()
                    if (code.isNotBlank()) {
                        viewModel.resetPassword(
                            binding.etEmail.text.toString().trim(),
                            code
                        )
                    } else {
                        pinView.error = getString(R.string.code_error)
                    }
                }
        }

        resetDialog?.show()
    }
}