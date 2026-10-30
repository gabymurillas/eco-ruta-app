package com.ecoruta.dispositivo

import android.app.Application
import androidx.work.Configuration
import com.ecoruta.dispositivo.di.AppContainer
import com.ecoruta.dispositivo.work.AppWorkerFactory

/**
 * Owns the [AppContainer] and gives WorkManager its worker factory. Allow-listing the app for
 * kiosk mode happens here, before any activity can try to enter it.
 */
class EcoRutaApplication : Application(), Configuration.Provider {

    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(container))
            .build()

    override fun onCreate() {
        super.onCreate()
        container.kiosk.prepare()
    }
}
