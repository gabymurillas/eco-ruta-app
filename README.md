# EcoRuta – Aplicación del dispositivo de ruta

App Android nativa (Kotlin + Jetpack Compose) que corre en un teléfono dedicado dentro del vehículo y
transmite su posición mientras la ruta está en servicio. Ver la especificación funcional en
`EcoRuta_App_Dispositivo_de_Ruta.pdf`.

> Estado: **funcional y con su interfaz definitiva**. Falta la lectura por QR (ver Pendiente).

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

MVVM por capas (Clean Architecture): las dependencias apuntan hacia el dominio,
`presentation → domain ← data`. El dominio es Kotlin puro, sin `android.*`, por eso sus reglas se
prueban en la JVM sin emulador.

```
app/src/main/java/com/ecoruta/dispositivo/
├── EcoRutaApplication.kt   Crea el AppContainer y configura WorkManager.
├── core/                   Outcome y AppError: resultado tipado de toda operación que puede fallar.
├── domain/                 Reglas del negocio. No conoce Android ni la red.
│   ├── model/              Modelos inmutables (DeviceSession, Trip, Position, TripStatus, ThemeMode…).
│   ├── policy/             TransmissionPolicy: 5 s en movimiento / 30 s detenido, calidad del GPS.
│   ├── repository/         Contratos de datos (sesión, viajes, telemetría, parámetros, ajustes…).
│   ├── system/             Contratos de la plataforma (GPS, batería, red, ubicación, servicio, cola).
│   └── usecase/            Una acción de negocio por clase (PairDevice, StartTrip, FinishTrip…).
├── data/                   Implementa los contratos del dominio.
│   ├── local/db/           Room: la cola offline de posiciones.
│   ├── local/prefs/        DataStore: vinculación (tokens cifrados con Keystore), viaje, ajustes.
│   ├── mapper/             Dominio ↔ DTO ↔ entidad.
│   ├── remote/             Retrofit + OkHttp contra Supabase: API, DTOs, renovación de token.
│   ├── repository/         *RepositoryImpl (patrón outbox en TelemetryRepositoryImpl).
│   └── system/             Adaptadores Android (FusedLocation, BatteryManager, ConnectivityManager…).
├── presentation/           Interfaz: Compose + ViewModel (StateFlow).
│   ├── <pantalla>/         welcome, pairing, idle, trip, incident, permission: XxxScreen + XxxViewModel.
│   ├── components/         Piezas compartidas, una por archivo (cristal, botones, avisos, andamio…).
│   ├── theme/              Sistema de diseño EcoRuta: Color, Type, Theme.
│   ├── navigation/         Rutas tipadas; la pantalla la decide el DeviceState, no los botones.
│   └── common/             Fábrica de ViewModels, mensajes de error, tiempo relativo.
├── service/                Foreground service de ubicación y su notificación.
├── work/                   SyncWorker: entrega posiciones y cierres pendientes al volver la red.
├── receiver/               Arranque automático y administrador de dispositivo.
├── platform/               Android que comparten servicio e interfaz: permisos y modo quiosco.
└── di/                     AppContainer: raíz de composición (inyección manual).

app/src/debug/              Solo en debug: catálogo de pantallas (ver Interfaz).
app/src/test/               Pruebas JVM, en el mismo orden: data/, domain/, presentation/, fakes/.
```

### Reglas de dependencia

| Capa | Puede usar | Nunca usa |
|---|---|---|
| `domain` | `core`, Kotlin y coroutines | `android.*`, `data`, `presentation` |
| `data` | `domain`, `core`, Android, Retrofit, Room | `presentation` |
| `presentation` | `domain` (casos de uso y modelos), `core`, `platform` | `data`: la vista nunca habla con un repositorio ni con la red |
| `service`, `work`, `receiver` | `domain` vía el `AppContainer` | lógica de negocio propia |
| `di` | todo | — es el único lugar que conoce las implementaciones |

Un ViewModel recibe **casos de uso**, no repositorios. Así cada acción que la interfaz puede pedir
tiene nombre propio y se prueba sola.

### Convenciones

- **Idiomas:** identificadores y comentarios en inglés; textos de interfaz en español
  (`res/values/strings.xml`, nunca en el código); nombres del backend en español con `@SerialName`.
- **Comentarios:** dicen el *porqué*, no el *qué*. Toda clase pública lleva KDoc.
- **Un tipo por archivo** (salvo un `UiState` junto a su ViewModel) y archivos cortos: el mayor ronda
  las 250 líneas.
- **Errores esperados** como `Outcome.Failure(AppError)`, nunca excepciones; el ViewModel los traduce
  con `toMessageRes()`.
- **Nada calibrable en el código:** intervalos y umbrales salen de la tabla `parametros`.
- **Compilación estricta:** los avisos de Kotlin son errores (`allWarningsAsErrors`).

### Cómo agregar una pantalla

1. Estado y acciones: `presentation/<pantalla>/XxxViewModel.kt` con un `XxxUiState` inmutable.
2. Si necesita algo nuevo del negocio: un caso de uso en `domain/usecase/` (y su contrato en
   `domain/repository/` o `domain/system/`), implementado en `data/`.
3. Interfaz: `XxxScreen.kt` sin estado (recibe `UiState` y lambdas) más un `XxxRoute` que conecta el
   ViewModel. Construirla con `ScreenScaffold` y los componentes de `components/`.
4. Registrar el ViewModel en `AppViewModelFactory` y sus dependencias en `AppContainer`.
5. Ruta en `navigation/Route.kt`; si depende del estado del equipo, en `DeviceState.toRoute()`.
6. Agregarla al catálogo de debug con sus estados, y probar su lógica en `app/src/test/`.

### Flujo de telemetría

1. Al vincular (y al abrir la app ya vinculada) arranca `TelemetryService`, un foreground service de
   tipo `location` que vive mientras el equipo esté vinculado. Ejecuta `RunTelemetrySessionUseCase`.
   Nunca arranca sin permiso de ubicación precisa: en Android 14+ eso cierra la app.
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

## Interfaz

Sigue el sistema de diseño de EcoRuta (CLAUDE.md del backend): fondo crema, terracota solo para la
identidad y la acción principal, verde / ámbar / rojo solo para estados, titulares en Merriweather y
texto en Source Sans 3. Las fuentes van dentro del APK (licencia OFL en `assets/licenses/`): un equipo
puede arrancar por primera vez sin red. Tiene tema oscuro con los mismos tonos.

- **Pensada para el asiento del conductor:** controles de 72 dp o más, etiquetas de 16 sp, valores de
  32 sp. En "En viaje" un banner sólido resume el peor estado (ubicación apagada > sin GPS > sin red >
  GPS débil > transmitiendo); color, punto y texto dicen lo mismo, nunca solo el color.
- **Cristal ("liquid glass"), excepción consciente al sistema**, que pide superficies planas: lo
  decidió la autora para esta app. Es translúcido con borde y brillo, sin desenfoque real (solo existe
  desde Android 12 y cuesta fotogramas en teléfonos baratos). Los estados críticos nunca van sobre
  cristal: usan relleno sólido para mantener el contraste al sol.
- **Bienvenida (solo la primera vez):** con `DeviceState.Unpaired(firstTime = true)`, es decir, si el
  equipo nunca se vinculó. Foto de los Morros a pantalla completa y el texto al pie, en blanco y sin
  recuadro; el único degradado funde la foto con el fondo oscuro detrás del texto. Se ve igual en
  ambos temas. "Comenzar" lleva a la vinculación. Si la sesión se revocó, va directo a vinculación
  con su aviso. Foto en `drawable-nodpi`, JPEG 1080×2400 (~130 KB).
- **Vinculación sobre la misma foto** (`ScreenScaffold(photo = …)`): título e instrucciones en blanco
  con una sombra suave, sin recuadro ni degradado; el código justo debajo, en cristal. En oscuro la
  foto se atenúa un 40 %. La foto no se mueve al abrir el teclado (`adjustResize`): solo el contenido.
- **Tema:** selector en la esquina superior derecha: igual que el teléfono (por defecto), automático
  por hora (oscuro de 18:30 a 06:00, sin red ni ubicación), claro u oscuro.

**Catálogo de diseño (solo debug).** Cada pantalla en cada estado, con datos de ejemplo, sin vincular
el equipo; sirve para revisar el diseño y para las capturas de la tesis:

```bash
adb shell am start -f 0x10008000 -n com.ecoruta.dispositivo/.catalog.DesignCatalogActivity --es screen trip_offline --es theme dark
```

Pantallas: `welcome`, `permission`, `pairing`, `pairing_error`, `idle`, `idle_offline`, `trip`, `trip_offline`,
`trip_nogps`, `trip_sent`, `incident`. Sin `-S`: en Android 15+ forzar la detención hace que el sistema
envíe `BOOT_COMPLETED` al reabrir, y `BootReceiver` abre la app real encima.

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
| Incidencias        | `POST rest/v1/incidencias` `{ruta_id, unidad_id, tipo, ...}`     | `tipo`: `calle_cerrada, falla_mecanica, desvio, congestion` u `otro` (solo observación; exige `descripcion`, `sql/08_incidencia_otro.sql`). |
| Latido             | `POST rest/v1/rpc/registrar_salud_del_dispositivo` `{bateria}`   | RPC, no PATCH: la unidad sale del JWT y `ultima_senal_at` la pone el servidor. Batería ilegible: se omite y el servidor conserva la última. Anon recibe 401. |

## Pendiente

- Lectura de QR en la vinculación (CameraX + ML Kit). **Bloqueado por el backend:** el panel admin
  hoy solo muestra el código de 5 caracteres, no genera QR; hace falta definir su contenido en
  `docs/contrato-vinculacion.md` y que la web lo dibuje.
- Fallback a `LocationManager` cuando no haya Google Play Services (limitación §6 del PDF).
- Viaje cerrado desde el panel: el equipo no se entera y sigue en "En viaje"; sus posiciones se
  guardan pero no evalúan geocercas. Hace falta que la app consulte el viaje o que el backend avise.
- Modo quiosco: salida de mantenimiento protegida y pantalla siempre encendida durante el viaje.
- Firma de release y `applicationId` definitivo.
