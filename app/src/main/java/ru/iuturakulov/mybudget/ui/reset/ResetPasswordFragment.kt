package ru.iuturakulov.mybudget.ui.reset

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentResetPasswordBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ResetPasswordFragment :
    BaseFragment<FragmentResetPasswordBinding>(R.layout.fragment_reset_password) {

    private val viewModel: ResetPasswordViewModel by viewModels()
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
                is ResetPasswordViewModel.ResetPasswordState.Success -> {
                    Snackbar.make(binding.root, "Успех! Проверьте почту", Snackbar.LENGTH_LONG)
                        .show()
                    findNavController().navigateUp()
                }

                is ResetPasswordViewModel.ResetPasswordState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                is ResetPasswordViewModel.ResetPasswordState.Loading -> {
                    // Отобразить прогресс
                }
            }
        }
    }
}