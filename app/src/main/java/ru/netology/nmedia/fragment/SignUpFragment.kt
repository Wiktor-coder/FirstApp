package ru.netology.nmedia.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.viewmodel.SignUpViewModel
import java.io.File
import java.io.FileOutputStream

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()

    private var avatarFile: File? = null

    // Лаунчер для разрешений
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Snackbar.make(binding.root, "Нужно разрешение для выбора фото", Snackbar.LENGTH_LONG).show()
        }
    }

    // Лаунчер для выбора изображения
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageUri = data?.data
            if (imageUri != null) {
                saveImageToFile(imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SignUpFragment", "Fragment created")

        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            Log.d("SignUpFragment", "Register button clicked")
            val name = binding.nameEdit.text.toString().trim()
            val login = binding.loginEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()
            val confirmPassword = binding.confirmPasswordEdit.text.toString()

            viewModel.register(name, login, password, confirmPassword, avatarFile)
        }

        binding.cancelButton.setOnClickListener {
            Log.d("SignUpFragment", "Cancel button clicked")
            findNavController().navigateUp()
        }

        binding.avatarImageView.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.removeAvatarButton.setOnClickListener {
            avatarFile = null
            binding.avatarImageView.setImageResource(R.drawable.ic_avatar_placeholder)
            binding.avatarHint.visibility = View.VISIBLE
            binding.removeAvatarButton.visibility = View.GONE
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun saveImageToFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            avatarFile = file
            binding.avatarImageView.setImageURI(uri)
            binding.avatarHint.visibility = View.GONE
            binding.removeAvatarButton.visibility = View.VISIBLE

            Log.d("SignUpFragment", "Avatar saved: ${file.absolutePath}, size: ${file.length()}")
        } catch (e: Exception) {
            Log.e("SignUpFragment", "Error saving avatar", e)
            Snackbar.make(binding.root, "Ошибка при сохранении аватарки", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                Log.d("SignUpFragment", "UI State: $state")
                when (state) {
                    is SignUpViewModel.SignUpUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.registerButton.isEnabled = false
                        binding.errorText.visibility = View.GONE
                    }

                    is SignUpViewModel.SignUpUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Log.d("SignUpFragment", "Success, navigating up")
                        findNavController().navigateUp()
                    }

                    is SignUpViewModel.SignUpUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
                        binding.errorText.text = state.message
                        binding.errorText.visibility = View.VISIBLE
                        Log.e("SignUpFragment", "Error: ${state.message}")
                    }

                    is SignUpViewModel.SignUpUiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
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