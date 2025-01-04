package ru.iuturakulov.mybudget.ui.register

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentRegisterBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class RegisterFragment : BaseFragment<FragmentRegisterBinding>(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels()

    override fun getViewBinding(view: View) = FragmentRegisterBinding.bind(view)

    override fun setupViews() {
        binding.apply {
            tvAlreadyRegistered.setOnClickListener {
                findNavController().navigateUp()
            }

            binding.toolbar.setNavigationOnClickListener {
                findNavController().navigateUp() // Возврат назад
            }

            btnRegister.setOnClickListener {
                val name = etName.text?.toString().orEmpty().trim()
                val email = etEmail.text?.toString().orEmpty().trim()
                val password = etPassword.text?.toString().orEmpty().trim()
                val confirmPassword = etConfirmPassword.text?.toString().orEmpty().trim()

                if (validateInputs(name, email, password, confirmPassword)) {
                    viewModel.register(name, email, password)
                }
            }
        }
    }

    override fun setupObservers() {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is RegisterViewModel.RegisterState.Loading

            when (state) {
                is RegisterViewModel.RegisterState.Success -> {
                    // TODO: показать экран кода подтверждения
                    Toast.makeText(
                        requireContext(),
                        "Регистрация успешна",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_register_to_login)
                }

                is RegisterViewModel.RegisterState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> Unit
            }
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        fun showError(inputLayout: TextInputLayout, error: String?): Boolean {
            inputLayout.error = error
            inputLayout.isErrorEnabled = error != null
            if (error != null) inputLayout.requestFocus()
            return error == null
        }

        if (!showError(
                inputLayout = binding.etNameInputLayout,
                error = "Имя не может быть пустым".takeIf { name.isBlank() }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etEmailInputLayout,
                error = when {
                    email.isBlank() -> "Email не может быть пустым"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches() -> "Некорректный Email"

                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etPasswordInputLayout,
                error = when {
                    password.isBlank() -> "Пароль не может быть пустым"
                    password.length < 6 -> "Пароль должен содержать минимум 6 символов"
                    else -> null
                }
            )
        ) {
            return false
        }

        if (!showError(
                inputLayout = binding.etConfirmPasswordInputLayout,
                error = when {
                    confirmPassword.isBlank() || confirmPassword != password -> "Пароли не совпадают"
                    else -> null
                }
            )
        ) {
            return false
        }

        if (!binding.cbPolicy.isChecked) {
            Toast.makeText(
                requireContext(),
                "Необходимо принять политику конфиденциальности",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }
}