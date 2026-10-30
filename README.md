# EcoRuta – Aplicación del dispositivo de ruta

App Android nativa (Kotlin + Jetpack Compose) que corre en un teléfono dedicado dentro del vehículo y
transmite su posición mientras la ruta está en servicio. Ver la especificación funcional en
`EcoRuta_App_Dispositivo_de_Ruta.pdf`.

> Estado: **base de arquitectura**. El diseño UI/UX está pendiente; las pantallas actuales son
> andamiaje funcional sin estilo.

## Requisitos

- JDK 17+ (probado con 21), Android SDK con plataforma 36.
- Gradle no hace falta instalarlo: se usa el wrapper (`gradlew.bat`).
- Variables del backend en `local.properties` (no se versiona):

  ```properties
  sdk.dir=C\:/Users/<usuario>/AppData/Local/Android/Sdk
  supabase.url=https://<proyecto>.supabase.co/
  supabase.anonKey=<anon public key>
  ```

## Comandos (también como tareas de VS Code: `Ctrl+Shift+B`, `Terminal > Run Task`)

| Acción                        | Comando                                                |
|-------------------------------|--------------------------------------------------------|
| Compilar APK debug            | `.\gradlew.bat assembleDebug`                          |
| Instalar en dispositivo/emu   | `.\gradlew.bat installDebug`                           |
| Pruebas unitarias             | `.\gradlew.bat testDebugUnitTest`                      |
| Lint                          | `.\gradlew.bat lintDebug`                              |
| Verificación completa         | `.\gradlew.bat testDebugUnitTest lintDebug assembleRelease` |

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Arquitectura

MVVM por capas con dependencias que apuntan hacia el dominio (`presentation → domain ← data`).
El dominio es Kotlin puro (sin `android.*`), por eso se prueba en JVM sin emulador.

```
presentation/   Compose + ViewModel (StateFlow). Una carpeta por pantalla.
  navigation/   Rutas tipadas; la pantalla la decide el DeviceState, no los botones.
domain/
  model/        Modelos y reglas puras (TransmissionPolicy: 5 s en movimiento / 30 s detenido).
  repository/   Contratos.
  system/       Contratos de la plataforma (GPS, batería, red, servicio, WorkManager).
  usecase/      Una acción de negocio por clase.
data/
  local/        Room (cola offline) y DataStore (vinculación, viaje activo).
  remote/       Retrofit + OkHttp (Supabase), DTOs, refresco automático de token.
  repository/   Implementaciones (patrón outbox en TelemetryRepositoryImpl).
  system/       Adaptadores Android (FusedLocation, BatteryManager, ConnectivityManager).
service/        Foreground service (location), notificación, modo quiosco.
work/           SyncWorker (reintenta la cola pendiente cuando vuelve la red).
receiver/       Arranque automático y administrador de dispositivo.
di/             AppContainer: raíz de composición manual.
```

### Flujo de telemetría

1. Al vincular (y al abrir la app ya vinculada) arranca `TelemetryService`, un foreground service de
   tipo `location` que vive mientras el equipo esté vinculado. Ejecuta `RunTelemetrySessionUseCase`.
2. **Latido**: envía la batería nada más arrancar y luego cada `emision_detenido_s`, haya viaje o no.
   El panel da la unidad por caída si pasa `sin_senal_umbral_s` sin latido, así que un equipo en
   espera no puede callarse (WorkManager no sirve: su mínimo periódico es 15 min).
3. **Captura**: solo corre mientras hay un viaje abierto. El GPS se pide siempre al intervalo rápido;
   `TransmissionPolicy` lo reduce al intervalo de detenido cuando la unidad no se mueve.
4. Cada posición se guarda **primero** en Room; nunca se pierde por falta de red.
5. Con red se envía de inmediato; sin red (o si falla) se agenda `SyncWorker` con reintentos.
6. La cola sale **completa en una sola llamada por viaje**: el servidor evalúa geocercas solo contra la
   lectura más reciente de cada llamada, y partir un respaldo atrasado haría que los trozos viejos
   evaluaran posiciones que la unidad ya dejó atrás. Las filas se borran solo tras el 200.
7. El buffer conserva máximo 2 h. Un 4xx no reintentable descarta el lote para no bloquear la cola.
8. Si el GPS o un latido fallan, se reintenta (GPS a los 10 s); ninguno detiene al otro ni al servicio.
9. Si el servidor rechaza el token de renovación, la sesión se borra y el equipo vuelve a la pantalla de
   vinculación. Revincular a la misma unidad conserva su viaje y su cola; a otra unidad, los descarta.

## Modo quiosco (una vez por equipo)

Requiere que la app sea *device owner*, con el equipo recién restablecido y sin cuentas:

```bash
adb shell dpm set-device-owner com.ecoruta.dispositivo/.receiver.KioskAdminReceiver
```

Con eso la app se auto-permite en lock-task (`KioskController.prepare()`) y al abrirse fija la pantalla.
Sin ser device owner, el modo quiosco se omite y la app funciona normal (útil para desarrollo).

## Contrato con el backend

Fuente de verdad: `docs/contrato-vinculacion.md` y `sql/01_schema.sql` del repositorio del backend.
La app usa **solo** la clave publicable (`apikey`) y el token de sesión del dispositivo; la clave
`service_role` nunca entra en el APK.

| Uso                | Llamada                                                          | Notas |
|--------------------|------------------------------------------------------------------|-------|
| Vincular           | `POST functions/v1/vincular-dispositivo` `{codigo}`              | Solo `apikey`, sin `Authorization`. Responde `{unidad:{id,nombre,ruta}, sesion:{access_token,refresh_token}}`. 401 = código inválido o vencido. |
| Renovar token      | `POST auth/v1/token?grant_type=refresh_token`                    | Automático ante un 401 (`SessionAuthenticator`). |
| Parámetros         | `GET rest/v1/parametros?select=clave,valor`                      | `emision_movimiento_s`, `emision_detenido_s`, `exactitud_maxima_m`, `sin_senal_umbral_s`. Se cachean para arrancar sin red. |
| Ruta de la unidad  | `GET rest/v1/unidades?id=eq.<id>&select=ruta_id`                 | `viajes` e `incidencias` exigen `ruta_id`. Se lee en vivo al abrir viaje o incidencia: el `ruta_id` del canje es el de *cuando se vinculó* y el admin puede reasignar la unidad. |
| Iniciar viaje      | `POST rest/v1/viajes` `{unidad_id, ruta_id}`                     | Antes envía el cierre pendiente del viaje anterior; sin red no abre otro. Un 409 (respuesta perdida) adopta el viaje abierto. |
| Viaje abierto      | `GET rest/v1/viajes?unidad_id=eq.<id>&finalizado_at=is.null`     | Solo tras un 409. Lectura pública. |
| Finalizar viaje    | `PATCH rest/v1/viajes?id=eq.<id>` `{finalizado_at}`              | Funciona sin red: se guarda con la hora real en que el conductor finalizó y `SyncWorker` lo envía al volver la conexión. |
| Posiciones         | `POST functions/v1/registrar-posicion` `{viaje_id, posiciones:[{lat,lng,exactitud_m,velocidad_kmh,capturado_at}]}` | Guarda **y evalúa geocercas**. El insert directo a `posiciones` no evalúa nada. Sin `unidad_id` (sale del JWT). Un 5xx = no se guardó: se reintenta. |
| Incidencias        | `POST rest/v1/incidencias` `{ruta_id, unidad_id, tipo, ...}`     | `tipo`: `calle_cerrada, falla_mecanica, desvio, congestion`. |
| Latido             | `POST rest/v1/rpc/registrar_salud_del_dispositivo` `{bateria}`   | RPC, no PATCH: la unidad sale del JWT y `ultima_senal_at` la pone el servidor. Anon recibe 401. |

## Pendiente

- Diseño UI/UX (pantallas grandes, legibles desde el puesto de conducción).
- Lectura de QR en la vinculación (CameraX + ML Kit) — hoy solo código alfanumérico.
- Fallback a `LocationManager` cuando no haya Google Play Services (limitación §6 del PDF).
- Firma de release y `applicationId` definitivo.
- Configurar `supabase.url` y `supabase.anonKey` (clave publicable) en `local.properties`.
