package com.vinz.fpsroyale

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Entry point Android. Menjalankan FPSGame (libGDX ApplicationListener) di dalam
 * sebuah Activity full-screen landscape.
 */
class MainActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = true
            numSamples = 0 // MSAA diatur manual lewat GameSettings (hemat baterai di HP low-end)
        }

        initialize(FPSGame(), config)
    }
}
