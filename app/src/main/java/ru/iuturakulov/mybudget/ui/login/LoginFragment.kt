package ru.iuturakulov.mybudget.ui.login

import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentLoginBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels<LoginViewModel>()

    override fun getViewBinding(view: View): FragmentLoginBinding {
        return FragmentLoginBinding.bind(view)
    }

    override fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()
            val password = binding.etPassword.text?.toString()
            val emailPattern = Patterns.EMAIL_ADDRESS
            if (email.isNullOrBlank() || !emailPattern.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (password.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is LoginViewModel.LoginState.Loading
            binding.btnLogin.isEnabled = state !is LoginViewModel.LoginState.Loading
            when (state) {
                is LoginViewModel.LoginState.Success -> {
                    findNavController().navigate(R.id.action_login_to_projects)
                }

                is LoginViewModel.LoginState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }

                else -> Unit // Ничего не делаем для Loading
            }
        }
    }
}