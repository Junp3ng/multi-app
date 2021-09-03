package com.junp3ng.myapplication

import android.app.Application
import android.util.Log
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel

class MainApplication : Application() {
    companion object{
        private const val TAG = "MainApplication"
        const val PRELOAD_FLUTTER_ENGINE_ID = "PRELOAD_FLUTTER_ENGINE_ID"
    }
    override fun onCreate() {
        super.onCreate()
        // 预热一个FlutterEngine
        preloadFlutterEngine();
    }

    private fun preloadFlutterEngine() {
        val flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint(
                FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "preload"
            )
        )

        FlutterEngineCache.getInstance().put(PRELOAD_FLUTTER_ENGINE_ID, flutterEngine)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

    }
}