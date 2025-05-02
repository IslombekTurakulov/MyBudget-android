package ru.iuturakulov.mybudget

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.auth.AuthEventBus
import ru.iuturakulov.mybudget.auth.CodeTokenStorage
import ru.iuturakulov.mybudget.auth.TokenStorage
import ru.iuturakulov.mybudget.core.worker.ProjectSyncWorker
import ru.iuturakulov.mybudget.databinding.ActivityMainBinding
import ru.iuturakulov.mybudget.di.PreferencesEntryPoint
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var authEventBus: AuthEventBus

    @Inject
    lateinit var codeTokenStorage: CodeTokenStorage

    private var hideJob: Job? = null
    private val connectivity by lazy { getSystemService<ConnectivityManager>()!! }

    override fun attachBaseContext(newBase: Context) {
        val prefs = EntryPointAccessors.fromApplication(
            newBase.applicationContext,
            PreferencesEntryPoint::class.java
        ).encryptedPrefs()

        val languageCode = prefs.getString(
            "locale",
            Locale.getDefault().language
        ) ?: Locale.getDefault().language

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
        }
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleDeepLinkIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectivity.observeNetworkStatus()
                    .distinctUntilChanged()
                    .collectLatest { isConnected ->
                        updateNetworkStatus(isConnected)
                    }
            }
        }

        // Инициализация NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        lifecycleScope.launchWhenStarted {
            authEventBus.unauthorized.collect {
                // Сначала строим NavOptions: очищаем бэк‑стек до старта графа, и ставим singleTop
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, inclusive = true)
                    .setLaunchSingleTop(true)
                    .build()

                // Навигируем на экран логина с нашими опциями
                navController.navigate(
                    R.id.loginFragment,
                    /* args = */ null,
                    /* navOptions = */ navOptions
                )
            }
        }

        val bottomNavigationView = binding.bottomNavigation
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavigationView.visibility = when (destination.id) {
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.resetPasswordFragment -> View.GONE

                else -> View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                if (uri.scheme == "mybudget" && uri.host == "invite") {
                    val code = uri.getQueryParameter("code")
                    if (!code.isNullOrBlank()) {
                        codeTokenStorage.saveCodeToken(code)
                    }
                }
            }
        }
    }

    private fun updateNetworkStatus(isConnected: Boolean) {
        val overlay = binding.networkStatusOverlay.root
        val statusView = binding.networkStatusOverlay.tvNetworkStatus
        val iconView = binding.networkStatusOverlay.ivStatusIcon

        hideJob?.cancel()

        val (bgColorRes, iconRes, tintRes, textRes) = if (isConnected) {
            Quad(
                R.color.networkStatusBackgroundOn,
                R.drawable.baseline_wifi_24,
                R.color.networkStatusIconTintOn,
                R.string.connected
            )
        } else {
            Quad(
                R.color.networkStatusBackgroundOff,
                R.drawable.baseline_wifi_off_24,
                R.color.networkStatusIconTintOff,
                R.string.no_internet_connection
            )
        }

        overlay.setBackgroundColor(ContextCompat.getColor(this, bgColorRes))
        statusView.setText(textRes)
        statusView.setTextColor(ContextCompat.getColor(this, tintRes))
        iconView.setImageResource(iconRes)
        iconView.setColorFilter(ContextCompat.getColor(this, tintRes))

        showOverlay(overlay)

        if (isConnected) {
            hideJob = lifecycleScope.launch {
                delay(2_000)
                hideOverlay(overlay)
            }
        }
    }

    private fun showOverlay(view: View) {
        view.apply {
            visibility = View.VISIBLE
            animate()
                .translationY(0f)
                .setDuration(500)
                .start()
        }
    }

    private fun hideOverlay(view: View) {
        view.animate()
            .translationY(-view.height.toFloat())
            .setDuration(500)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    private fun ConnectivityManager.observeNetworkStatus(): Flow<Boolean> = callbackFlow {
        trySend(isCurrentlyConnected())

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

    private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
        val nw = activeNetwork ?: return false
        val caps = getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private data class Quad<A, B, C, D>(
        val first: A, val second: B, val third: C, val fourth: D
    )
}