package ru.iuturakulov.mybudget.ui.register

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
            // Валидация ввода в реальном времени
            etName.addTextChangedListener { updateRegisterButtonState() }
            etEmail.addTextChangedListener { updateRegisterButtonState() }
            etPassword.addTextChangedListener { updateRegisterButtonState() }
            etConfirmPassword.addTextChangedListener { updateRegisterButtonState() }
            cbPolicy.setOnCheckedChangeListener { _, _ -> updateRegisterButtonState() }

            btnRegister.setOnClickListener {
                val name = etName.text.toString()
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

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
                    Toast.makeText(requireContext(), "Регистрация успешна", Toast.LENGTH_SHORT)
                        .show()
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
        var isValid = true

        if (name.isBlank()) {
            binding.etName.error = "Имя не может быть пустым"
            isValid = false
        }

        if (email.isBlank()) {
            binding.etEmail.error = "Email не может быть пустым"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Некорректный Email"
            isValid = false
        }

        if (password.isBlank()) {
            binding.etPassword.error = "Пароль не может быть пустым"
            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        }

        if (confirmPassword.isBlank() || confirmPassword != password) {
            binding.etConfirmPassword.error = "Пароли не совпадают"
            isValid = false
        }

        if (!binding.cbPolicy.isChecked) {
            Toast.makeText(
                requireContext(),
                "Необходимо принять политику конфиденциальности",
                Toast.LENGTH_SHORT
            ).show()
            isValid = false
        }

        return isValid
    }

    private fun updateRegisterButtonState() {
        binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                binding.etEmail.text.toString().isNotBlank() &&
                binding.etPassword.text.toString().isNotBlank() &&
                binding.etConfirmPassword.text.toString().isNotBlank() &&
                binding.cbPolicy.isChecked
    }
}