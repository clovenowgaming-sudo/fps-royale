package com.vinz.fpsroyale

import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entry point Android. Menjalankan FPSGame (libGDX ApplicationListener) di dalam
 * sebuah Activity full-screen landscape.
 */
class MainActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashLogger()

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = true
            numSamples = 0 // MSAA diatur manual lewat GameSettings (hemat baterai di HP low-end)
        }

        initialize(FPSGame(), config)
    }

    /**
     * Nangkep SEMUA uncaught exception (termasuk yang terjadi di thread render libGDX)
     * dan nulis stacktrace lengkapnya ke file teks di penyimpanan khusus app ini.
     * Device tanpa root/ADB gak bisa baca logcat app lain, tapi folder ini BISA dibaca
     * dari Termux (lewat ~/storage/shared/Android/data/com.vinz.fpsroyale/files/) tanpa root,
     * karena ini masih di bawah external-storage milik app sendiri.
     */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = File(dir, "crash_log.txt")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                file.appendText("\n\n===== CRASH @ $time (thread: ${thread.name}) =====\n$sw")
                Log.e("FPSRoyaleCrash", "Fatal error, log ditulis ke ${file.absolutePath}", throwable)
            } catch (loggingError: Throwable) {
                Log.e("FPSRoyaleCrash", "Gagal nulis crash log", loggingError)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
