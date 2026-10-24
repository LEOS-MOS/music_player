package com.example.muzzio.service

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.muzzio.R
import com.example.muzzio.player.PlayerController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var controller: PlayerController

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.nav_host_fragment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tracks -> {
                    navController.navigate(R.id.trackListFragment)
                    true
                }
                R.id.nav_playlists -> {
                    navController.navigate(R.id.playlistListFragment)
                    true
                }
                else -> false
            }
        }

        // приложение запущено нажатием на уведомление
        if (savedInstanceState == null) {
            findViewById<View>(R.id.nav_host_fragment).post { openPlayerIfRequested(intent) }
        }
    }

    // приложение уже было открыто, и пользователь нажал на уведомление
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPlayerIfRequested(intent)
    }

    private fun openPlayerIfRequested(intent: Intent?) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) return
        // чтобы не сработало повторно, например при повороте экрана
        intent.removeExtra(EXTRA_OPEN_PLAYER)

        val player = controller.getPlayer()
        if (player.mediaItemCount == 0) return
        if (navController.currentDestination?.id == R.id.playerFragment) return

        navController.navigate(
            R.id.playerFragment,
            bundleOf("index" to player.currentMediaItemIndex)
        )
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}
