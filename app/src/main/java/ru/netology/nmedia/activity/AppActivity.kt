package ru.netology.nmedia.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.fragment.FeedFragment
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity() {
    private val viewModel: AuthViewModel by viewModels()
    // Лаунчер для запроса разрешений
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.w("AppActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("ERROR_LOG", "APP STARTED!!! onCreate called")

        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем Toolbar как ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Применяем отступы для корневого view, а не для navController
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        // Запрос разрешения на уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Получаем NavController здесь
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_controller) as NavHostFragment
        val navController = navHostFragment.navController

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                menu.findItem(R.id.signin)?.isVisible = !viewModel.authenticated
                menu.findItem(R.id.signup)?.isVisible = !viewModel.authenticated
                menu.findItem(R.id.signout)?.isVisible = viewModel.authenticated
            }

//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                menuInflater.inflate(R.menu.menu_main, menu)
//
//                menu.let {
//                    it.setGroupVisible(R.id.unauthenticated, !viewModel.authenticated)
//                    it.setGroupVisible(R.id.authenticated, viewModel.authenticated)
//                }
//            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                Log.d("AppActivity", "Menu item selected: ${menuItem.itemId}")

                return when (menuItem.itemId) {
                    R.id.signin -> {
                        Log.d("AppActivity", "Sign in clicked")
                        try {
                            // Используем action из вашего nav_main
                            navController.navigate(R.id.action_feedFragment_to_signInFragment)
                            true
                        } catch (e: Exception) {
                            Log.e("AppActivity", "Navigation error", e)
                            false
                        }
                    }

                    R.id.signup -> {
                        Log.d("AppActivity", "Sign up clicked")
                        try {
                            navController.navigate (R.id.action_feedFragment_to_signInFragment)
                            true
                        } catch (e: Exception) {
                            Log.e("AppActivity", "Navigation error", e)
                            false
                        }
                    }

                    R.id.signout -> {
                        Log.d("AppActivity", "Sign out clicked")
                        // Показываем диалог подтверждения через ViewModel
                        val navHostFragment = supportFragmentManager
                            .findFragmentById(R.id.nav_controller) as NavHostFragment
                        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                        if (currentFragment is FeedFragment) {
                            currentFragment.viewModel.signOut()
                        } else {
                            // Если текущий фрагмент не FeedFragment, просто выходим
                            AppAuth.getInstance().removeAuth()
                        }
                        true
                    }

                    else -> false
                }
            }
        })

        lifecycleScope.launch {
            AppAuth.getInstance().authStateFlow.collect {
                invalidateOptionsMenu()
            }
        }

            viewModel.data.observe(this) {
                invalidateOptionsMenu()
            }

        handleIntent(binding)
    }

    private fun handleIntent(binding: ActivityAppBinding) {
        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)

            if (text.isNullOrBlank()) {
                Snackbar.make(
                    binding.root,
                    R.string.error_empty_content,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) { finish() }
                    .show()
                return
            }

            // Правильный способ получить NavController
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_controller) as NavHostFragment
            val navController = navHostFragment.navController

            // Навигация с аргументом
            val bundle = Bundle().apply {
                textArg = text
            }

            navController.navigate(
                R.id.action_feedFragment_to_newPostFragment2,
                bundle
            )
        }
    }
}