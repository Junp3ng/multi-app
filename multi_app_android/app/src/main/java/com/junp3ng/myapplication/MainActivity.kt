package com.junp3ng.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import io.flutter.FlutterInjector
import io.flutter.embedding.android.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().commit()
        findViewById<MaterialToolbar>(R.id.topAppBar).setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_get_flutter_app_bundle_path -> {
                    val findAppBundlePath =
                        FlutterInjector.instance().flutterLoader().findAppBundlePath()
                    Toast.makeText(
                        this,
                        findAppBundlePath,
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d("MainActivity", "menuClick: $it $findAppBundlePath ")
                }
                R.id.menu_open_preload -> {
                    val newIntent =
                        FlutterActivity.withCachedEngine(MainApplication.PRELOAD_FLUTTER_ENGINE_ID)
                            .build(this)

                    startActivity(newIntent)
                }
            }
            true
        }
    }
}