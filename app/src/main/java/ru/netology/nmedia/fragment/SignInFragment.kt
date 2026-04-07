package ru.netology.nmedia.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInViewModel

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SignInFragment", "Fragment created")

        setupClickListeners()
        observeAuthState()

        // Для отладки - предзаполняем поля (можно убрать в релизе)
        binding.loginEdit.setText("student")
        binding.passwordEdit.setText("secret")
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            Log.d("SignInFragment", "Sign in button clicked")
            val login = binding.loginEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()
            Log.d("SignInFragment", "Login: $login, Password length: ${password.length}")
            viewModel.authenticate(login, password)
        }

        binding.cancelButton.setOnClickListener {
            Log.d("SignInFragment", "Cancel button clicked")
            findNavController().navigateUp()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                Log.d("SignInFragment", "Auth state: $state")
                when (state) {
                    is SignInViewModel.AuthUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.signInButton.isEnabled = false
                        binding.errorText.visibility = View.GONE
                    }

                    is SignInViewModel.AuthUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Log.d("SignInFragment", "Success, navigating up")
                        // Возвращаемся на предыдущий фрагмент
                        findNavController().navigateUp()
                    }

                    is SignInViewModel.AuthUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.signInButton.isEnabled = true
                        binding.errorText.text = state.message
                        binding.errorText.visibility = View.VISIBLE
                        Log.e("SignInFragment", "Error: ${state.message}")
                    }

                    is SignInViewModel.AuthUiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.signInButton.isEnabled = true
                        binding.errorText.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

