package ru.iuturakulov.mybudget

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.core.worker.ProjectSyncWorker
import ru.iuturakulov.mybudget.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val connectivityManager by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val workRequest = PeriodicWorkRequestBuilder<ProjectSyncWorker>(
            5, TimeUnit.MINUTES
        ).build()

        observeNetworkChanges()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ProjectSync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // Инициализация NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Настройка BottomNavigationView
        val bottomNavigationView = binding.bottomNavigation
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // Скрываем BottomNavigationView на SplashScreen и авторизации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationView.visibility = when (destination.id) {
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.resetPasswordFragment -> View.GONE

                else -> View.VISIBLE
            }

            when (destination.id) {

            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun observeNetworkChanges() {
        lifecycleScope.launch {
            connectivityManager.observeNetworkStatus().collect { isConnected ->
                updateNetworkStatus(isConnected)
            }
        }
    }

    private fun updateNetworkStatus(isConnected: Boolean) {
        val networkStatusOverlay = binding.networkStatusOverlay
        val tvStatus = networkStatusOverlay.tvNetworkStatus
        val ivStatusIcon = networkStatusOverlay.ivStatusIcon

        if (isConnected) {
            networkStatusOverlay.root.setBackgroundColor(ContextCompat.getColor(this, R.color.networkStatusBackgroundOn))
            tvStatus.text = getString(R.string.connected)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.networkStatusIconTintOn))
            ivStatusIcon.setImageResource(R.drawable.baseline_wifi_24)
            ivStatusIcon.setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.networkStatusIconTintOn
                )
            )
            showOverlay(networkStatusOverlay.root)
            lifecycleScope.launch {
                delay(2000)
                hideOverlay(networkStatusOverlay.root)
            }
        } else {
            networkStatusOverlay.root.setBackgroundColor(ContextCompat.getColor(this, R.color.networkStatusBackgroundOff))
            tvStatus.text = getString(R.string.no_internet_connection)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.networkStatusIconTintOff))
            ivStatusIcon.setImageResource(R.drawable.baseline_wifi_off_24)
            ivStatusIcon.setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.networkStatusIconTintOff
                )
            )
            showOverlay(networkStatusOverlay.root)
        }
    }

    private fun showOverlay(view: View) {
        view.visibility = View.VISIBLE
        view.animate()
            .translationY(0f)
            .setDuration(500)
            .start()
    }

    private fun hideOverlay(view: View) {
        view.animate()
            .translationY(-view.height.toFloat())
            .setDuration(500)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    // Extension to observe network status
    private fun ConnectivityManager.observeNetworkStatus(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        registerDefaultNetworkCallback(callback)
        awaitClose { unregisterNetworkCallback(callback) }
    }
}