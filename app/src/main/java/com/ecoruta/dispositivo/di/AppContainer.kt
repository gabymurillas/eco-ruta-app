package com.ecoruta.dispositivo.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ecoruta.dispositivo.BuildConfig
import com.ecoruta.dispositivo.data.local.db.AppDatabase
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.local.prefs.KeystoreTokenCipher
import com.ecoruta.dispositivo.data.remote.RemoteServices
import com.ecoruta.dispositivo.data.remote.SupabaseConfig
import com.ecoruta.dispositivo.data.remote.UnitRouteResolver
import com.ecoruta.dispositivo.data.repository.DeviceStatusRepositoryImpl
import com.ecoruta.dispositivo.data.repository.IncidentRepositoryImpl
import com.ecoruta.dispositivo.data.repository.ParametersRepositoryImpl
import com.ecoruta.dispositivo.data.repository.SessionRepositoryImpl
import com.ecoruta.dispositivo.data.repository.TelemetryRepositoryImpl
import com.ecoruta.dispositivo.data.repository.TripRepositoryImpl
import com.ecoruta.dispositivo.data.system.AndroidBatteryStatusSource
import com.ecoruta.dispositivo.data.system.AndroidConnectivityObserver
import com.ecoruta.dispositivo.data.system.FusedLocationSource
import com.ecoruta.dispositivo.data.system.InMemoryTelemetryStatusStore
import com.ecoruta.dispositivo.domain.model.TransmissionPolicy
import com.ecoruta.dispositivo.domain.repository.DeviceStatusRepository
import com.ecoruta.dispositivo.domain.repository.IncidentRepository
import com.ecoruta.dispositivo.domain.repository.ParametersRepository
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import com.ecoruta.dispositivo.domain.system.BatteryStatusSource
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.LocationSource
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import com.ecoruta.dispositivo.domain.system.TelemetryController
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import com.ecoruta.dispositivo.domain.usecase.CaptureTelemetryUseCase
import com.ecoruta.dispositivo.domain.usecase.FinishTripUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.domain.usecase.ObservePendingTripCloseUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveTripStatusUseCase
import com.ecoruta.dispositivo.domain.usecase.PairDeviceUseCase
import com.ecoruta.dispositivo.domain.usecase.ReportIncidentUseCase
import com.ecoruta.dispositivo.domain.usecase.ResumeTelemetryUseCase
import com.ecoruta.dispositivo.domain.usecase.RunTelemetrySessionUseCase
import com.ecoruta.dispositivo.domain.usecase.SendHeartbeatUseCase
import com.ecoruta.dispositivo.domain.usecase.StartTripUseCase
import com.ecoruta.dispositivo.domain.usecase.SyncPendingDataUseCase
import com.ecoruta.dispositivo.service.ForegroundTelemetryController
import com.ecoruta.dispositivo.service.KioskController
import com.ecoruta.dispositivo.work.WorkManagerSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Composition root. Owns every long-lived object and wires the layers together by hand.
 *
 * Manual injection keeps the build lean for an app this size; every dependency is already passed
 * through constructors, so migrating to Hilt/Koin later only means replacing this class.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock: Clock = Clock.systemUTC()

    // Storage
    private val devicePreferences = DevicePreferences(
        store = PreferenceDataStoreFactory.create(scope = appScope) { appContext.preferencesDataStoreFile("device") },
        cipher = KeystoreTokenCipher(),
    )
    private val database by lazy { AppDatabase.create(appContext) }

    // Network
    private val remote by lazy {
        RemoteServices(
            config = SupabaseConfig(
                baseUrl = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
                enableHttpLogging = BuildConfig.DEBUG,
            ),
            preferences = devicePreferences,
        )
    }

    private val unitRoutes by lazy { UnitRouteResolver(remote.restApi) }

    // Calibration owned by the backend `parametros` table
    private val parametersRepository: ParametersRepository by lazy {
        ParametersRepositoryImpl(remote.restApi, devicePreferences)
    }
    private val policy by lazy { TransmissionPolicy(parametersRepository) }

    // Android system adapters
    private val locationSource: LocationSource by lazy { FusedLocationSource(appContext) }
    private val batterySource: BatteryStatusSource by lazy { AndroidBatteryStatusSource(appContext) }
    private val connectivity: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext, appScope) }
    private val statusStore: TelemetryStatusStore = InMemoryTelemetryStatusStore()
    val syncScheduler: SyncScheduler by lazy { WorkManagerSyncScheduler(appContext) }
    val telemetryController: TelemetryController by lazy { ForegroundTelemetryController(appContext) }
    val kiosk by lazy { KioskController(appContext) }

    // Repositories
    private val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(devicePreferences, remote.authApi)
    }
    private val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(devicePreferences, remote.restApi, unitRoutes, syncScheduler, clock)
    }
    private val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepositoryImpl(
            dao = database.pendingPositionDao(),
            api = remote.restApi,
            connectivity = connectivity,
            scheduler = syncScheduler,
            statusStore = statusStore,
            paramsSource = parametersRepository,
            clock = clock,
            appScope = appScope,
        )
    }
    private val deviceStatusRepository: DeviceStatusRepository by lazy { DeviceStatusRepositoryImpl(remote.restApi) }
    private val incidentRepository: IncidentRepository by lazy {
        IncidentRepositoryImpl(remote.restApi, unitRoutes)
    }

    // Use cases
    val pairDevice by lazy {
        PairDeviceUseCase(sessionRepository, tripRepository, telemetryRepository, telemetryController)
    }
    val startTrip by lazy { StartTripUseCase(sessionRepository, tripRepository) }
    val finishTrip by lazy { FinishTripUseCase(tripRepository) }
    val syncPendingData by lazy { SyncPendingDataUseCase(telemetryRepository, tripRepository) }
    val reportIncident by lazy {
        ReportIncidentUseCase(
            sessions = sessionRepository,
            location = locationSource,
            statusStore = statusStore,
            params = parametersRepository,
            clock = clock,
            incidents = incidentRepository,
        )
    }
    val resumeTelemetry by lazy { ResumeTelemetryUseCase(sessionRepository, telemetryController) }
    val observeDeviceState by lazy { ObserveDeviceStateUseCase(sessionRepository, tripRepository) }
    val observePendingTripClose by lazy { ObservePendingTripCloseUseCase(tripRepository) }
    val observeTripStatus by lazy {
        ObserveTripStatusUseCase(statusStore, telemetryRepository, connectivity, batterySource, policy, clock)
    }
    private val sendHeartbeat by lazy {
        SendHeartbeatUseCase(sessionRepository, batterySource, deviceStatusRepository)
    }
    val runTelemetrySession by lazy {
        val capture = CaptureTelemetryUseCase(
            location = locationSource,
            sessions = sessionRepository,
            trips = tripRepository,
            telemetry = telemetryRepository,
            statusStore = statusStore,
            policy = policy,
        )
        RunTelemetrySessionUseCase(
            captureTelemetry = capture,
            sendHeartbeat = sendHeartbeat,
            trips = tripRepository,
            parameters = parametersRepository,
            reportFailure = { Log.e(TELEMETRY_TAG, "Telemetry step failed; it will be retried", it) },
        )
    }

    private companion object {
        const val TELEMETRY_TAG = "TelemetrySession"
    }
}
