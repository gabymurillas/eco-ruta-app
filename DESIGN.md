# EcoRuta · Diseño de la app del dispositivo

Fuente única del diseño de la app Android del dispositivo de ruta. Toma la **identidad del panel
admin de la web** (`ecoruta-web/src/app/globals.css`, `.tema-admin`) y la adapta al asiento del
conductor. Boceto aprobado: <https://claude.ai/artifact/FhaS1paZoD4hFHPBL7ubdQ> (2026-11-19).

Ninguna pantalla escribe un color, una medida, un radio o un tamaño de letra: todo sale de los
tokens de la sección 3. Si algo no existe como token, se agrega aquí y en `presentation/theme/`
antes de usarlo.

---

## 1. Principios

1. **Se usa desde el asiento.** Se lee de un vistazo, a un brazo de distancia y a pleno sol.
   Controles de 56 dp como mínimo, acciones principales de 84 dp, etiquetas de 16 sp y valores de 28 sp.
2. **Identidad del panel, plana.** Azul petróleo, turquesa de acento, fondo gris claro y tarjetas
   blancas con borde de 1 dp. Sin cristal, sin sombras, sin degradados; la profundidad va por tono.
3. **El estado nunca va solo en color.** Siempre punto o ícono más texto.
4. **Un botón apagado dice por qué.** Debajo, en texto legible: «Detén la unidad para cambiar la ruta».
5. **Lo crítico va sobre relleno sólido.** El banner de estado y los avisos de error nunca son translúcidos.
6. **Mismo diseño en claro y oscuro.** El oscuro es el del panel; el modo «automático» lo activa al anochecer.

## 2. Estructura MVVM de la interfaz

La interfaz vive en `presentation/` y sigue la arquitectura del README (`presentation → domain ← data`).
El diseño se reparte así:

```
presentation/
├── theme/                  TOKENS. Lo único que conoce valores concretos.
│   ├── Color.kt            EcoColors (claro / oscuro) — sección 3.1
│   ├── Type.kt             EcoType + fuente Figtree — sección 3.2
│   ├── Dimens.kt           EcoSpacing, EcoRadius, EcoSize, EcoBorder — secciones 3.3 a 3.6
│   ├── Motion.kt           EcoMotion — sección 3.7
│   └── Theme.kt            EcoRutaTheme: provee todo por CompositionLocal; acceso con `EcoTheme.x`
├── components/             PIEZAS. Sin estado; solo leen tokens. Una por archivo — sección 4.
├── <pantalla>/             XxxScreen (sin estado, recibe XxxUiState + lambdas)
│                           XxxRoute (conecta el ViewModel)
│                           XxxViewModel (StateFlow<XxxUiState>, llama casos de uso)
└── navigation/             La pantalla la decide DeviceState, no los botones.
```

Reglas:

| Capa | Puede | Nunca |
|---|---|---|
| `theme/` | Definir valores | Importar componentes o pantallas |
| `components/` | Leer `EcoTheme.*` | Escribir `Color(0x…)`, `12.dp`, `18.sp`; conocer un ViewModel |
| `XxxScreen` | Componer componentes; leer `EcoTheme.*` para el layout | Valores literales; lógica de negocio; textos fuera de `strings.xml` |
| `XxxViewModel` | Exponer `UiState` con datos y estados (cargando, error, deshabilitado y su motivo) | Conocer colores, `R.drawable` o Compose |

El **motivo** de un control deshabilitado es dato del `UiState` (por ejemplo
`changeRouteBlockedReason: BlockedReason?`), no una decisión de la pantalla: así se prueba en la JVM.

Acceso a los tokens en Compose:

```kotlin
val c = EcoTheme.colors       // EcoColors
val s = EcoTheme.spacing      // EcoSpacing
val r = EcoTheme.radius       // EcoRadius
val z = EcoTheme.size         // EcoSize
val t = EcoTheme.type         // EcoType
```

## 3. Tokens

### 3.1 Color

Nombres en Kotlin (`EcoColors`) y su equivalente en la web (`--color-*` de `.tema-admin`).

| Token | Web | Claro | Oscuro | Uso |
|---|---|---|---|---|
| `background` | fondo | `#F1F5F6` | `#0B1B21` | Fondo de pantalla |
| `surface` | superficie | `#FFFFFF` | `#11262D` | Tarjetas, filas, botón secundario |
| `border` | borde | `#DCE4E7` | `#24414A` | Borde de tarjetas (1 dp) |
| `borderField` | borde-campo | `#78898F` | `#6E878E` | Borde de botón secundario, campos, anillo de radio |
| `ink` | tinta | `#10232A` | `#E4EDEF` | Texto principal |
| `inkMuted` | tinta-media | `#4B5E65` | `#A9BCC2` | Etiquetas, texto secundario |
| `header` | menú | `#0E2A33` | `#06141A` | Barra superior |
| `onHeader` | — | `#FFFFFF` | `#FFFFFF` | Texto e íconos de la barra superior |
| `onHeaderMuted` | texto menú | `#CFE0E4` | `#CFE0E4` | Íconos secundarios y bordes en la barra |
| `brand` | marca | `#197878` | `#4FC1BE` | Acento: identidad y foco |
| `brandOnHeader` | — | `#4FC1BE` | `#4FC1BE` | La unidad (disco) de la marca Eco sobre la banda `header` |
| `brandStrong` | marca-profundo | `#0E2A33` | `#E4EDEF` | Relleno de la acción principal y de la opción elegida |
| `onBrandStrong` | — | `#FFFFFF` | `#0B1B21` | Texto sobre `brandStrong` |
| `brandTint` | marca-tenue | `#E8EFF1` | `#173A44` | Fondo de acciones secundarias («Cambiar») y avisos informativos |
| `brandText` | — | `#145F60` | `#4FC1BE` | Texto e íconos sobre `brandTint` |
| `success` | exito | `#1E8E5A` | `#3CC283` | Puntos de estado «bien» |
| `successStrong` | — | `#137049` | `#3CC283` | Relleno del banner «Transmitiendo» |
| `onSuccessStrong` | — | `#FFFFFF` | `#0B1B21` | Texto sobre `successStrong` |
| `warning` | advertencia | `#B4531A` | `#F08A4B` | Puntos e íconos de advertencia; relleno del banner de advertencia |
| `onWarning` | — | `#FFFFFF` | `#0B1B21` | Texto sobre `warning` |
| `warningSurface` | advertencia-fondo | `#F7F5F2` | `#152A31` | Fondo del aviso de advertencia |
| `warningBorder` | advertencia-borde | `#E2DDD6` | `#5A4130` | Borde del aviso de advertencia |
| `error` | error | `#B42318` | `#F28B82` | Ícono de error; relleno del banner de error |
| `onError` | — | `#FFFFFF` | `#0B1B21` | Texto sobre `error` |
| `errorSurface` | — | `#FBEAE8` | `#2A1B1E` | Fondo del aviso de error |
| `errorBorder` | — | `#F1C7C2` | `#5C2B2B` | Borde del aviso de error |
| `onPhoto` | — | `#FFFFFF` | `#FFFFFF` | Texto sobre la foto de los Morros |
| `photoScrim` | — | transparente | `#73000000` | Atenúa la foto en oscuro |

Contrastes verificados (WCAG): `ink`/`background` 14,8:1 · `inkMuted`/`surface` 6,8:1 (oscuro 8,0:1) ·
`onBrandStrong`/`brandStrong` 15,0:1 (oscuro 14,8:1) · `onSuccessStrong`/`successStrong` 6,1:1 (oscuro 7,7:1) ·
`brandText`/`brandTint` 6,4:1 (oscuro 5,6:1) · `borderField`/`surface` 3,6:1 (oscuro 4,1:1; mínimo no textual 3:1).

`successStrong` existe porque el verde del panel (`#1E8E5A`) con texto blanco da 4,1:1 y no pasa AA.
`errorSurface` y `errorBorder` no existen en la web; se derivan del mismo tono que `error`.

### 3.2 Tipografía

Familia única: **Figtree** (OFL), incluida en el APK (400, 500, 600, 700, 800): un equipo puede
arrancar sin red. Roles en `EcoType`:

| Token | Tamaño / interlínea | Peso | Tracking | Uso |
|---|---|---|---|---|
| `display` | 34 / 40 sp | 800 | −0,02 em | Título de pantalla («Listo para salir a ruta») |
| `status` | 32 / 38 sp | 800 | −0,02 em | Título del banner de estado |
| `screenTitle` | 26 / 32 sp | 800 | −0,01 em | Título en la barra superior («Elegir ruta») |
| `value` | 28 / 34 sp | 700 | 0 | Valor de una ficha («En línea») |
| `routeName` | 24 / 30 sp | 800 | −0,01 em | Nombre de la ruta actual (26 / 32 en «En espera») |
| `option` | 22 / 28 sp | 700 | 0 | Fila de una lista de opciones |
| `brand` | 22 / 28 sp | 800 | −0,01 em | Marca «EcoRuta» |
| `button` | 20 / 24 sp | 800 | 0 | Botones de 84 dp |
| `buttonSmall` | 18 / 22 sp | 700 | 0 | Botones de 56 dp («Cambiar») |
| `body` | 18 / 26 sp | 500 | 0 | Texto corrido, avisos, detalle del banner |
| `label` | 16 / 20 sp | 600 | 0 | Etiquetas de fichas, insignias, texto secundario |
| `code` | 34 sp | 700 | 0, cifras tabulares | Casillas del código de vinculación |

### 3.3 Espaciado (`EcoSpacing`, múltiplos de 4)

| Token | dp | Uso |
|---|---|---|
| `xxs` | 4 | Etiqueta → valor dentro de una ficha |
| `xs` | 8 | Ícono → texto; entre insignias |
| `sm` | 12 | Entre fichas de una cuadrícula; entre botones de la barra inferior |
| `md` | 14 | Entre bloques del cuerpo de una pantalla |
| `lg` | 16 | Relleno interno de tarjetas y avisos; margen inferior |
| `xl` | 20 | Margen lateral de pantalla |
| `xxl` | 24 | Relleno del banner de estado |

### 3.4 Radios (`EcoRadius`)

| Token | dp | Uso |
|---|---|---|
| `sm` | 12 | Avisos y motivos |
| `md` | 14 | Controles de 52–56 dp (Cambiar, Volver, tema) |
| `lg` | 16 | Tarjetas, fichas, filas de opción, botones de 84 dp |
| `xl` | 20 | Banner de estado |
| `full` | 50 % | Insignias, puntos |

### 3.5 Tamaños (`EcoSize`)

| Token | dp | Uso |
|---|---|---|
| `iconButton` | 52 | Botones de ícono en la barra superior |
| `controlSmall` | 56 | Acción secundaria dentro de una tarjeta («Cambiar») |
| `control` | 72 | Mínimo de cualquier botón de texto |
| `primaryAction` | 84 | Botones de la barra inferior (en todas las pantallas) |
| `optionRow` | 80 | Fila de un selector |
| `tileMin` | 104 | Alto mínimo de una ficha de estado |
| `routeCardMin` | 112 | Tarjeta de ruta en «En espera» |
| `badgeMin` | 36 | Insignia de estado |
| `icon` | 24 | Íconos |
| `dot` | 12 | Punto de estado |
| `brandMark` | 30 | Ancho de la marca Eco junto a «EcoRuta» (alto proporcional, 180 : 104) |
| `dotLarge` | 16 | Punto del banner |

### 3.6 Bordes (`EcoBorder`)

`hairline` 1 dp (tarjetas, avisos) · `field` 1,5 dp (botón secundario, campos, deshabilitado discontinuo) · `focus` 2 dp en `brand`.

### 3.7 Movimiento (`EcoMotion`)

`fast` 200 ms ease-out (cambios de estado, selección) · `pulse` 2 000 ms (halo «en vivo» del banner) ·
sin rebotes. Con «quitar animaciones» del sistema, el pulso se detiene y los cambios son instantáneos.

## 4. Componentes

Todos en `presentation/components/`, uno por archivo, sin estado propio.

| Componente | Anatomía y tokens | Estados |
|---|---|---|
| `TopBar` | Banda `header`, `BrandMark` (ecos `onHeader`, unidad `brandOnHeader`), «EcoRuta» en `type.brand` `onHeader`; a la derecha un `IconButton` opcional (tema, volver) de `iconButton`, radio `md`, borde `onHeaderMuted` | Con título (`screenTitle`) y «Volver» en pantallas secundarias |
| `StatusBadge` | Insignia `badgeMin`, radio `full`, `surface` + borde `border`, punto + texto `label` | Neutro (En espera) |
| `StatusBanner` | Relleno sólido (`successStrong` / `warning` / `error`), radio `xl`, relleno `xxl`; punto `dotLarge` con pulso + `status` + `body` | Transmitiendo · sin red · GPS débil · sin GPS · ubicación apagada |
| `InfoTile` | `surface`, borde `hairline` `border`, radio `lg`, relleno `lg`, alto `tileMin`; `label` `inkMuted` + punto, `value`, detalle `label` | Valor en `ink`; advertencia con punto `warning` |
| `RouteCard` *(nuevo)* | Como `InfoTile`; etiqueta + `routeName` + «Normal: …» + `SecondaryActionButton` «Cambiar» | Habilitado · deshabilitado con `DisabledReason` debajo · guardando |
| `PrimaryButton` | `brandStrong`, texto `button` `onBrandStrong`, radio `lg`, alto `primaryAction` | Normal · cargando (indicador) · deshabilitado (`brandStrong` al 45 % sobre `background`) |
| `SecondaryButton` | `surface`, borde `field` `borderField`, texto `button` `ink` | Normal · deshabilitado |
| `SecondaryActionButton` *(nuevo)* | `brandTint`, texto `buttonSmall` `brandText`, chevron, alto `controlSmall`, radio `md` | Deshabilitado: transparente, borde `field` discontinuo, candado, `inkMuted` |
| `ChoiceRow` *(nuevo)* | Fila `optionRow`, radio `lg`, anillo de radio 26 dp + `option` + subtítulo `label` | Elegida: `brandStrong` / `onBrandStrong` · no elegida: `surface` / borde `border` |
| `NoticeBanner` | Radio `sm`, relleno `lg`, ícono `icon` + texto `body` | Info: `brandTint` · advertencia: `warningSurface` + `warningBorder` · error: `errorSurface` + `errorBorder`, `role=alert` |
| `DisabledReason` *(nuevo)* | `NoticeBanner` informativo compacto con ícono «i» | — |
| `CodeField` | Una casilla por carácter, `surface`, borde `field`, radio `lg`, texto `code` | Siguiente casilla con borde `focus` · error con borde `error` |

Se eliminan: `GlassPanel`, `Modifier.glass`, `ContourBackground`, `HighlightTitle` (el panel es plano).
El **ícono de elegido** en el selector de tema es `Icons.Filled.Check`, no el carácter ✓.

### 4.1 Marca «Eco»

La unidad (un disco) y, detrás, dos ecos de las posiciones que va enviando, cada uno más atrás y
más fino. «Eco» es eco: la marca es el nombre. Originales vectoriales (solo rellenos, sin trazos)
en `docs/brand/`:

| Archivo | Uso |
|---|---|
| `ecoruta-mark.svg` | Color sobre claro: ecos `#0E2A33`, unidad `#197878` |
| `ecoruta-mark-reversed.svg` | Sobre petróleo: ecos blancos, unidad `#4FC1BE` |
| `ecoruta-mark-black.svg` / `-white.svg` | Una tinta |
| `ecoruta-mark-small.svg` | Corte para 24 px o menos: un solo eco, más grueso |
| `ecoruta-app-icon.svg` | Vista del ícono de la app |

En Android: `ic_launcher_foreground` (fondo `ic_launcher_background` = `#0E2A33`),
`ic_launcher_monochrome` (íconos temáticos), `ic_notification` (corte pequeño, blanco) y el componente
`BrandMark`, que dibuja la misma geometría con los tokens. Nunca se deforma, rota ni recolorea fuera
de estas combinaciones; el espacio libre alrededor es, como mínimo, el grosor del eco cercano × 2.

## 5. Pantallas

| Pantalla | Barra superior | Cuerpo | Barra inferior |
|---|---|---|---|
| Bienvenida (carrusel, una vez) | Marca + «Saltar», sobre `header` | 4 páginas con paralaje: foto de los Morros, ruta animada, cola sin señal, código que se escribe solo; título `display`, texto `body` | Indicador de páginas + `PrimaryButton` «Siguiente» / «Vincular equipo» (colores del tema oscuro: fondo siempre petróleo) |
| Vinculación | `TopBar` + tema | Foto, título, instrucciones, `CodeField`, avisos | `PrimaryButton` «Vincular» |
| En espera | `TopBar` + tema | Foto, `StatusBadge`, título, `RouteCard`, fichas Unidad y Conexión, avisos | `PrimaryButton` «Iniciar recorrido» |
| En recorrido | `TopBar` sin tema | `StatusBanner`, `RouteCard`, 4 `InfoTile` (GPS, Conexión, Alimentación, Envíos), avisos | `SecondaryButton` «Reportar incidencia» + `PrimaryButton` «Finalizar recorrido» |
| Elegir ruta *(nueva)* | `TopBar` con «Volver» y título | Texto `body` `inkMuted`, lista de `ChoiceRow` («Ruta normal» primero), avisos | `SecondaryButton` «Cancelar» + `PrimaryButton` «Confirmar» |
| Incidencia | `TopBar` con «Volver» y título | Lista de `ChoiceRow` con ícono, campo de observación | «Cancelar» + «Enviar» |
| Permiso | `TopBar` | Título, texto | «Abrir ajustes» + «Permitir» |

Estados de «Elegir ruta»: cargando la lista · lista (desde caché si no hay red) · sin red («Hace falta
conexión para cambiar la ruta», Confirmar deshabilitado) · guardando · opción rechazada (la lista se
recarga y avisa) · en movimiento (no se puede abrir: `RouteCard` muestra el motivo).

## 6. Migración desde el diseño anterior

| Antes | Ahora |
|---|---|
| Crema, terracota y cristal | Panel: petróleo, turquesa, superficies planas |
| Merriweather + Source Sans 3 | Figtree |
| Radios 10 / 16 / 18 / 22 / 26 / 28 escritos en cada archivo | `EcoRadius` (12 / 14 / 16 / 20) |
| Alturas 72 / 76 / 80 / 84 según la pantalla | `EcoSize.primaryAction` (84) en todas las barras |
| `NoticeBanner` sobre cristal con barra de color al costado | Fondo sólido suave + borde + ícono |
| Selector de tema en todas las pantallas | Solo en Vinculación y En espera |
| `34.sp` y `FontWeight` sueltos en `CodeField` | `EcoType.code` |
| Botón «Comenzar» con `LightEcoColors` fijo | `surface` / `ink`, siempre claro sobre la foto |

El README («Interfaz») describe el diseño anterior y se actualiza al aplicar estos tokens.
