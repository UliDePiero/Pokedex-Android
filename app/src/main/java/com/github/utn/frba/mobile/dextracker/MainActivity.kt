package com.github.utn.frba.mobile.dextracker

//import com.google.android.material.snackbar.Snackbar

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
import com.github.utn.frba.mobile.dextracker.repository.InMemoryRepository
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
    /////////////////////////////////////////////////////////
    //SOLO PARA TESTING
    //val favPokesFragment = InfoPokeFragment("b2w2-national","eevee")
    //val favPokesFragment = ShareDexFragment()
    ////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {

        //Thread.sleep(2000)  //Solo lo use para testear el splash
        setTheme(R.style.Theme_DexTracker_NoActionBar)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            //Toast.makeText(this,"Aqui pondria el scanner...si tan solo tuviera uno",Toast.LENGTH_LONG).show()
            /*if(Permisos.checkForPermissions(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    cameraRequestCode,
                    "No se puede escanear un codigo si no tenemos acceso a tu camara"))
                    {*/
            val scanner = IntentIntegrator(this)
            scanner.setOrientationLocked(false)
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            scanner.setPrompt("Escanea el codigo QR de la PokeDEX deseada")
            scanner.initiateScan()
            //}

            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()*/
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.background = null
        bottomNavigationView.menu.getItem(2).isEnabled = false
        bottomNavigationView.selectedItemId = R.id.misdex

        makeCurrentFragment(redirect?.to() ?: myDexFragment)
//        makeCurrentFragment(DexDiffFragment.newInstance(
//            leftUserId = "U-2021-02-13-ddf9d418-d114-435b-b901-69f57223dca4",
//            leftUserDexId = "UD-2021-06-05-2ff23c99-7811-4e4a-a127-ebb4ab599af0",
//            rightUserId = "U-2021-04-18-36d52c56-4cbd-4e81-9885-80e095990bf8",
//            rightUserDexId = "UD-2021-04-18-a7263e1a-e894-4ffc-8a78-711ac7140e65"
//        ))
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
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
