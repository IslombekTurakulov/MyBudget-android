package ru.iuturakulov.mybudget.ui.login

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentLoginBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun getViewBinding(view: View): FragmentLoginBinding {
        return FragmentLoginBinding.bind(view)
    }

    override fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is LoginViewModel.LoginState.Loading
            when (state) {
                is LoginViewModel.LoginState.Success -> {
                    findNavController().navigate(R.id.action_login_to_projects)
                }
                is LoginViewModel.LoginState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit // Ничего не делаем для Loading
            }
        }
    }
}