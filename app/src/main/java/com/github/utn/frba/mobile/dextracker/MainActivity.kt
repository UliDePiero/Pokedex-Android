package com.github.utn.frba.mobile.dextracker


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.github.utn.frba.mobile.dextracker.async.AsyncCoroutineExecutor
import com.github.utn.frba.mobile.dextracker.db.storage.SessionStorage
import com.github.utn.frba.mobile.dextracker.extensions.replaceWithAnimWith
import com.github.utn.frba.mobile.dextracker.firebase.redirect
import com.github.utn.frba.mobile.dextracker.repository.inMemoryRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator

const val cameraRequestCode = 9999

class MainActivity : AppCompatActivity() {
    private val perfilFragment = PerfilFragment()
    private val myDexFragment = MyDexFragment()
    private val favDexFragment = FavDEX_Fragment()
    private val favPokesFragment = FavPokes_Fragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DexTracker_NoActionBar)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            val scanner = IntentIntegrator(this)
            scanner.setOrientationLocked(false)
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            scanner.setPrompt("Escanea el codigo QR de la PokeDEX deseada")
            scanner.initiateScan()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.background = null
        bottomNavigationView.menu.getItem(2).isEnabled = false
        bottomNavigationView.selectedItemId = R.id.misdex

        makeCurrentFragment(redirect?.to() ?: myDexFragment)

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.perfil -> makeCurrentFragment(perfilFragment)
                R.id.misdex -> makeCurrentFragment(myDexFragment)
                R.id.favdex -> makeCurrentFragment(favDexFragment)
                R.id.favpokes -> makeCurrentFragment(favPokesFragment)
            }
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(this, "Cancelado :(", Toast.LENGTH_LONG).show()
                } else {
                    //Aca iria la redireccion hacia la DEX
                    val userId = result.contents.substringBefore("@")
                    val dexId = result.contents.substringAfter("@")
                    makeCurrentFragment(favDexFragment)
                    favDexFragment.replaceWithAnimWith(
                        resourceId  = R.id.fl_wrapper,
                        other       = PokedexFragment.newInstance(
                            userId = userId,
                            dexId = dexId,
                        ),
                        enter   = R.anim.fragment_open_enter,
                        exit    = R.anim.fragment_fade_exit,
                        popEnter= R.anim.fragment_open_enter,
                        popExit = R.anim.fragment_open_exit,
                    )
                    Toast.makeText(this, "Escaneado:  \nUserID: $userId \nDexID: $dexId", Toast.LENGTH_LONG).show()
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            cameraRequestCode -> {
                if (grantResults.count() > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val scanner = IntentIntegrator(this)
                    scanner.setOrientationLocked(false)
                    scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    scanner.setPrompt("Escanea el codigo QR de la PokeDEX deseada")
                    scanner.initiateScan()
                } else {
                    Toast.makeText(this, "No me diste permiso!", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.commit {
                setCustomAnimations(
                        R.anim.fragment_open_enter,
                        R.anim.fragment_fade_exit,
                        R.anim.fragment_fade_enter,
                        R.anim.fragment_open_exit,
                )
            replace(R.id.fl_wrapper, fragment)
            }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        val sessionStorage = SessionStorage(this)
        AsyncCoroutineExecutor.dispatch { sessionStorage.store(inMemoryRepository.session) }
    }
}
