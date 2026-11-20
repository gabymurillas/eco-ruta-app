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
| APK demo (backend simulado)   | `.\gradlew.bat assembleDemo`                           |
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

El diseño completo está en [DESIGN.md](DESIGN.md): principios, tokens, componentes y pantallas. Usa la
identidad del **panel admin de la web**: barra superior azul petróleo, turquesa como acento, fondo gris
claro y tarjetas blancas planas con borde, sin cristal ni sombras. La fuente es Figtree, que va dentro
del APK (licencia OFL en `assets/licenses/`) para que un equipo pueda arrancar por primera vez sin red.
Tiene tema oscuro, el mismo del panel.

- **Todo sale de tokens.** `presentation/theme/` define colores, tipografía, espaciado, radios,
  tamaños, bordes y movimiento; pantallas y componentes los leen con `EcoTheme.*` y nunca escriben un
  color, un `dp` o un `sp` propio.
- **Pensada para el asiento del conductor:** acción principal de 84 dp, controles de 56 dp o más,
  etiquetas de 16 sp, valores de 28 sp. En "En recorrido" un banner sólido resume el peor estado
  (ubicación apagada > sin GPS > sin red > GPS débil > transmitiendo); color, punto y texto dicen lo
  mismo, nunca solo el color. Los avisos van sobre relleno sólido suave con ícono.
- **Carrusel de bienvenida (una sola vez por teléfono):** con `DeviceState.Unpaired(firstTime = true)`,
  es decir, si nunca se vio el carrusel y el equipo nunca se vinculó. Cuatro páginas sobre fondo
  petróleo: la foto de los Morros, una ruta animada con el bus transmitiendo, la cola de posiciones
  sin señal y el código de vinculación escribiéndose solo. Las ilustraciones se dibujan con `Canvas`
  y se desplazan con paralaje. "Saltar" o "Vincular equipo" lo marcan como visto
  (`CompleteOnboardingUseCase`) y la navegación pasa sola a la vinculación. Si la sesión se revocó,
  va directo a vinculación con su aviso.
- **Vinculación y espera sobre la misma foto** (`ScreenScaffold(photo = …)`), bajo la barra petróleo.
  En oscuro la foto se atenúa. La foto no se mueve al abrir el teclado (`adjustResize`): solo el contenido.
- **Tema:** selector en la barra superior de Vinculación y En espera (no en "En recorrido", donde el
  conductor maneja): igual que el teléfono (por defecto), automático por hora (oscuro de 18:30 a 06:00,
  sin red ni ubicación), claro u oscuro.

**APK demo.** La variante `demo` (`assembleDemo`, se instala junto a la app real como
`com.ecoruta.dispositivo.demo`) responde todas las llamadas al backend con datos de ejemplo
(`MockBackendInterceptor`), para recorrer todas las pantallas sin código ni red: acepta cualquier
código de 5 caracteres y `ERROR` simula un código vencido. El resto de la app corre su lógica real
(GPS incluido). Trae también el catálogo de diseño.

**Catálogo de diseño (solo debug y demo).** Cada pantalla en cada estado, con datos de ejemplo, sin vincular
el equipo; sirve para revisar el diseño y para las capturas de la tesis:

```bash
adb shell am start -f 0x10008000 -n com.ecoruta.dispositivo/.catalog.DesignCatalogActivity --es screen trip_offline --es theme dark
```

Pantallas: `welcome`, `permission`, `pairing`, `pairing_error`, `idle`, `idle_offline`, `trip`, `trip_offline`,
`trip_nogps`, `incident`. Sin `-S`: en Android 15+ forzar la detención hace que el sistema
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
