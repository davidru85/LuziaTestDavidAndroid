# Luzia Voice Assistant — Android

> Cliente de chat por voz de alto rendimiento para un backend de IA autoalojado. Construido como escaparate de ingeniería Android de nivel sénior: Clean Architecture, TDD estricto, MVVM + flujo de datos unidireccional, Material 3 Expressive.

---

## Tabla de contenidos

1. [Visión general](#visión-general)
2. [Las Reglas de Oro](#las-reglas-de-oro)
3. [Stack tecnológico](#stack-tecnológico)
4. [Arquitectura](#arquitectura)
5. [Funcionalidades entregadas](#funcionalidades-entregadas)
6. [Estructura del proyecto](#estructura-del-proyecto)
7. [Configuración](#configuración)
8. [Variantes de compilación](#variantes-de-compilación)
9. [Pruebas](#pruebas)
10. [CI / CD](#ci--cd)
11. [Decisiones de diseño](#decisiones-de-diseño)
12. [Mejoras futuras / Siguientes pasos](#mejoras-futuras--siguientes-pasos)
13. [Documentación de referencia](#documentación-de-referencia)

---

## Visión general

Luzia es un cliente de chat *voice-first*. El usuario pulsa el micrófono, el audio crudo se captura y se sube a un backend de IA local que lo transcribe mediante Whisper, reenvía la conversación a un LLM y devuelve la respuesta token a token mediante Server-Sent Events. El cliente Android renderiza la respuesta en streaming como texto, con una opción bajo demanda de "leer en voz alta" que utiliza el motor TTS del sistema Android.

El proyecto está deliberadamente acotado como escaparate de ingeniería más que como una aplicación de consumo lista para publicar. Cada decisión arquitectónica se optimiza para la testabilidad, la pureza de capas y la coordinación *contract-first* con el equipo de backend — compromisos que se hacen visibles en la **suite de 497 pruebas unitarias**, en la separación de capas impuesta por `DomainPurityTest` y en el [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) versionado que actúa como fuente única de verdad entre ambos equipos.

Estado: **Fases 1 a 10 completadas** (chat + voz + streaming + personas + TTS + i18n + estrategia de errores en 3 niveles + R8 + baseline profile + grafo de Hilt para tests). La fase 11 (este README + backlog de mejoras futuras) es el último elemento pendiente. Consulta [ROADMAP.md](ROADMAP.md) para el historial fase por fase.

---

## Las Reglas de Oro

Seis invariantes innegociables, garantizadas por el código y las pruebas:

1. **Cero STT / ASR local para la entrada del usuario.** El audio se captura crudo y se sube; la transcripción se realiza en el servidor (Whisper). En el lado de salida, el motor `TextToSpeech` del sistema Android **puede** invocarse al pulsar el usuario para leer en voz alta una respuesta del asistente — sin motores de terceros, sin reproducción automática.
2. **Cero procesado de audio local.** El cliente es un agente de captura y transmisión. Sin reducción de ruido, sin recodificación. El recorder escribe `.m4a` / AAC directamente desde `MediaRecorder` y envía los bytes tal cual.
3. **Pureza del dominio.** La capa `domain/` tiene **cero** dependencias de Android o AndroidX. Kotlin / JVM puro, 100 % testeable en pruebas unitarias. Garantizado por [`DomainPurityTest`](app/src/test/java/com/ruizurraca/luziatestdavid/architecture/DomainPurityTest.kt).
4. **Sin fugas entre capas.** `presentation/` nunca importa de `data/`. Las capas se comunican únicamente a través de las interfaces de `domain/`. Garantizado por `PresentationPurityTest`.
5. **TDD estricto.** Red → Green → Refactor. Ningún código de producción se publica sin una prueba que falle previamente.
6. **UI orientada a texto.** La lista de mensajes es solo texto. Sin formas de onda, sin controles de reproducción del audio grabado por el usuario. La única opción de audio en el lado de salida es un único botón con icono de TTS sobre la **última** respuesta recibida del asistente.

El contexto completo y la narrativa histórica de las acotaciones (Fork 6, que delimitó la excepción del TTS) se encuentran en [CONTEXT.md](CONTEXT.md) y [MEMORY.md](MEMORY.md).

---

## Stack tecnológico

| Área | Elección | Versión |
| :--- | :--- | :--- |
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 Expressive | Compose BOM 2026.02.01 |
| Arquitectura | MVVM + Clean Architecture (3 capas, unidireccional) | — |
| DI | Hilt | 2.59.2 |
| Asincronía | Coroutines + Flow | 1.10.2 |
| Red | Ktor Client (motor OkHttp) | 3.3.0 |
| Serialización | kotlinx.serialization (JSON, `explicitNulls = false`) | 1.9.0 |
| Persistencia | Room | 2.8.4 |
| Build | Android Gradle Plugin + KSP | AGP 9.1.1 / KSP 2.2.10-2.0.2 |
| JDK | 17 (source, target, daemon de Gradle) | — |
| Min / Target SDK | 24 / 36 | — |
| Pruebas | JUnit 5 (unit) + JUnit 4 Vintage (Robolectric Compose) + MockK + Turbine + Ktor MockEngine + grafo de Hilt para tests | — |
| Cobertura | Jacoco (`enableUnitTestCoverage` nativo de AGP) | 0.8.12 |
| Rendimiento | Baseline profile (módulo productor) | androidx.benchmark 1.5.0-alpha05 |

Catálogo de versiones: [gradle/libs.versions.toml](gradle/libs.versions.toml). Los pines de compatibilidad y su justificación están documentados en la auto-memoria `agp9_compatibility_pins`.

---

## Arquitectura

Clean Architecture con flujo de datos unidireccional estricto:

```
UI Event → ViewModel → UseCase → Repository → DataSource (Remote / Local)
```

### Guarda de imports (innegociable)

| Paquete | Imports prohibidos | Razón |
| :--- | :--- | :--- |
| `domain/` | `android.*`, `androidx.*`, `...data.*` | Pruebas unitarias en JVM puro |
| `data/` | `...presentation.*` | Independiente de UI |
| `presentation/` | `...data.*` | Solo habla con interfaces de `domain/` |

Las violaciones rompen CI mediante las pruebas de pureza. Detalle completo en [TECHNICAL_SPEC.md §Architecture](TECHNICAL_SPEC.md).

### Modelo de threading

Todo el cambio de `Dispatchers` ocurre en la capa de repository / data source mediante `flowOn()`. Los dispatchers se inyectan vía Hilt (`@IoDispatcher`, `@MainDispatcher`) para que las pruebas puedan sustituirlos por `UnconfinedTestDispatcher` de forma determinista.

- `Dispatchers.IO` — red, Room, parseo de SSE.
- `Dispatchers.Main` — ciclo de vida de `MediaRecorder`, mutaciones del estado de UI.
- `Dispatchers.Default` — procesado de datos pesado (si lo hubiera).

**Sin `Thread.sleep()` en ninguna prueba.** Todas las pruebas asíncronas usan `runTest` + `StandardTestDispatcher`.

### Estrategia de errores (3 niveles)

| Nivel | Símbolo en código | Mecanismo | Disparador |
| :--- | :--- | :--- | :--- |
| **Nivel 1** | `ChatEvent.TransientSnackbar` | Snackbar, autodescarte | Validación / formato (VALIDATION_ERROR, BAD_REQUEST, FILE_TOO_LARGE, fallos del recorder local, TTS no disponible) |
| **Nivel 2** | viaja sobre `MessageStatus.FAILED` | Burbuja inline + botón de reintento debajo | Conectividad / red durante el streaming |
| **Nivel 3** | `ChatEvent.BlockingErrorDialog` | AlertDialog, modal | Errores críticos / de servidor (INTERNAL_ERROR, SERVICE_UNAVAILABLE) |

Jerarquía sealed `AppError` en `domain/`, clasificación por `ErrorMapper` en `data/`, enrutado mediante `AppError.toChatEvent()` en `presentation/`. Los mensajes de error provistos por el backend se preservan literalmente a lo largo del stack. Tabla completa en [TECHNICAL_SPEC.md §Error Handling](TECHNICAL_SPEC.md).

---

## Funcionalidades entregadas

Fases 1 a 10 completadas. Capacidades destacables:

- **Chat por voz a texto.** Pulsar para grabar → transcripción Whisper redondeada por `/transcribe` → transcript mostrado como mensaje de usuario → gestión de permisos + diálogo de justificación.
- **Respuestas de IA en streaming.** SSE sobre `/chat` parseado línea a línea; la UI transita LOADING (shimmer) → STREAMING (fade-in de tokens + LiveRegion.Polite) → RECEIVED.
- **Captura de persona por mensaje.** Tres personas (Estudiante / Científico / Artista) seleccionables desde una tira de `FilterChip` en la barra superior con iconos específicos de cada persona y animación bounce al seleccionar. La persona activa en el momento del envío se captura por mensaje de usuario y se reenvía como `role_prompt` en el cable (semántica del Fork 4).
- **TTS bajo demanda** (Fase 10.6.D). Icono de "leer en voz alta" únicamente sobre la última respuesta RECEIVED del asistente. Pulsar habla, volver a pulsar detiene, cambiar de mensaje cambia la reproducción, la reproducción se autodescarga al completar de forma natural. Conscienza de locale vía `LocalConfiguration.current.locales[0]`.
- **Estrategia de errores en 3 niveles.** Modelo de dominio sealed `AppError`, parser de envoltorio `ErrorMapper`, mensajes provistos por el backend preservados literalmente. `BlockingErrorDialog` reutiliza un composable compartido `LuziaAlertDialog` con slot de icono + tinte por severidad + detalles colapsables.
- **Reintento sobre respuestas del asistente fallidas.** `RetryAssistantReplyButton` dedicado debajo de la última burbuja FAILED (reubicado fuera de la burbuja en la fase 10.6.A). Copy específico por posición — *"Me he quedado en blanco. ¿Quieres reintentar?"* para el último fallo reintentable, *"Lo siento, mensaje vacío"* para vacíos antiguos no reintentables.
- **Internacionalización.** Tres locales totalmente traducidos (en / es-ES / pt-PT), incluyendo `role_prompts`, con cada string visible para el usuario fijado byte a byte mediante pruebas específicas de locale. Campo opcional `lang` en ambos endpoints POST (Fase 10.6.H, API_SPEC v1.4.0) que reenvía el locale del dispositivo al backend, de modo que los usuarios en locales que la propia app no traduce pueden seguir recibiendo respuestas en su idioma preferido.
- **Accesibilidad.** `LiveRegion.Polite` sobre los estados de streaming / loading del asistente (los mensajes históricos deliberadamente no se reanuncian al hacer scroll), `Role.RadioButton` en los chips de persona, content descriptions en cada elemento interactivo, verificado con TalkBack en dispositivo.
- **Material 3 Expressive.** Color dinámico en Android 12+, icono de launcher adaptativo con capa monocroma de themed-icons para Android 13+, rediseño expresivo de AlertDialog con slot de icono + tintes por severidad, selección de chip de persona conducida por motion.
- **R8 + reducción de recursos en release.** 21,6 MB → 1,79 MB (~92 % de reducción). Reglas keep manuales de kotlinx.serialization para los DTO `@Serializable` que solo se usan por reflection.
- **Baseline profile** que cubre arranque en frío → grafo de Hilt → render de ChatScreen → rotación de personas (Fase 10.3.B). Módulo productor en [`baseline-profile/`](baseline-profile/), consumido por `:app`.
- **Tooling de cobertura Jacoco** (Fase 10.2.A). Paquetes de lógica de negocio al 96–100 % de cobertura de líneas; informe en `app/build/reports/jacoco/`.
- **Smoke test a nivel de Activity** (`MainActivitySmokeTest`) usando `@HiltAndroidTest` + `ActivityScenario` y un `TestAppModule` que sustituye `NetworkModule` (MockEngine), `DatabaseModule` (Room en memoria) y `AudioModule` (recorder + speaker fake).

---

## Estructura del proyecto

```
com.ruizurraca.luziatestdavid/
├── domain/                  # Kotlin / JVM puro — sin imports de Android
│   ├── audio/               # Contratos AudioRecorder + TextSpeaker
│   ├── catalog/             # Contrato PersonaCatalog
│   ├── common/              # Resource<T>, jerarquía sealed AppError
│   ├── locale/              # LocaleProvider (Fase 10.6.H)
│   ├── model/               # ChatMessage, MessageRole, Persona, PersonaEntry, MessageStatus
│   ├── repository/          # Contrato ChatRepository
│   └── usecase/             # TranscribeAudioUseCase, StreamAssistantReplyUseCase, SendMessageUseCase
├── data/
│   ├── catalog/             # DefaultPersonaCatalog (impl respaldada por Android-strings)
│   ├── local/
│   │   ├── audio/           # Adaptadores MediaRecorderAudioRecorder + AndroidTextSpeaker
│   │   ├── dao/ + entity/   # DAO de Room + ChatMessageEntity
│   │   ├── locale/          # AndroidLocaleProvider (Fase 10.6.H)
│   │   └── mapper/          # ChatEntityMapper (entity ↔ domain)
│   ├── remote/
│   │   ├── api/             # L1ApiClient (transporte Ktor)
│   │   ├── dto/             # ChatMessageDto, ChatRequestDto, TranscribeResponseDto, ApiErrorEnvelope
│   │   ├── mapper/          # ChatMapper (domain → DTO de cable), ErrorMapper
│   │   └── sse/             # SseParser + SseEvent
│   └── repository/          # ChatRepositoryImpl (orquestador que cablea L1ApiClient + SseParser + mappers + DAO)
├── presentation/
│   ├── component/           # AssistantMessageBubble, UserMessageBubble, MessageBubble, ChatInputBar,
│   │                        # ChatTopAppBar, RoleSelectorChips, MorphingActionButton, ShimmerBox,
│   │                        # StreamingIndicator, RetryAssistantReplyButton, TtsPlayButton, LuziaAlertDialog
│   ├── model/               # ChatMessageUiModel + ChatMessageUiMapper (domain → UI)
│   ├── screen/              # ChatScreen (cableado a VM) + ChatScreenContent (sin estado)
│   ├── state/               # ChatUiState sealed interface, ChatEvent (TransientSnackbar / BlockingErrorDialog)
│   ├── theme/               # Color.kt, Type.kt, Theme.kt (consciente de color dinámico)
│   └── viewmodel/           # ChatViewModel
├── di/                      # Módulos de Hilt — Network / Database / Repository / Audio / Catalog / Locale / Dispatcher
├── LuziaApp.kt              # @HiltAndroidApp
└── MainActivity.kt          # Activity única, @AndroidEntryPoint, aloja ChatScreen + enableEdgeToEdge
```

---

## Configuración

Sigue estos cuatro pasos en orden. El flujo completo debería tomar menos de 5 minutos en una máquina que ya tenga Android Studio + JDK 17 instalados.

### 1. Requisitos previos

- Android Studio Koala (2024.1.1) o superior, o Gradle por línea de comandos ≥ 8.11
- JDK 17 (el plugin `foojay-resolver-convention` de Gradle descargará una toolchain compatible si es necesario)
- Un backend de IA accesible en la `BASE_URL_PRODUCTION` que configures más abajo — consulta [TECHNICAL_SPEC.md §API Contracts](TECHNICAL_SPEC.md) para ver la forma del cable que el backend debe respetar

### 2. Crea `local.properties` desde la plantilla

El repositorio incluye un [`local.properties.example`](local.properties.example) con valores de marcador. Cópialo como `local.properties` (que está en gitignore) y personalízalo para tu máquina:

```bash
cp local.properties.example local.properties
```

Después abre `local.properties` y edita estas tres variables:

| Variable | A qué establecerla |
| :--- | :--- |
| `sdk.dir` | Ruta absoluta al SDK de Android local. Valores típicos: `/Users/<tú>/Library/Android/sdk` (macOS), `C:\\Users\\<tú>\\AppData\\Local\\Android\\Sdk` (Windows), `/home/<tú>/Android/Sdk` (Linux). Android Studio normalmente autogenera `local.properties` al abrir el proyecto por primera vez con el `sdk.dir` correcto, así que si el archivo ya existe y apunta a una ruta válida del SDK, deja `sdk.dir` como está. |
| `BASE_URL_PRODUCTION` | IP o hostname + puerto de tu servidor backend en ejecución, p. ej. `http://192.168.1.42:8000/` en una LAN, o `http://10.0.2.2:8000/` cuando se ejecuta en el emulador de Android para alcanzar el loopback de la máquina anfitriona. |
| `BASE_URL_STAGING` | Se entrega preconfigurada con una URL pública de mockable.io para que la variante `stagingDebug` funcione *out of the box* en demos / runs de CI. Déjala como está salvo que tengas un backend de staging dedicado. |

El build falla ruidosamente (`IllegalStateException`) si falta cualquiera de las claves `BASE_URL_*` — sin defaults silenciosos. Si `sdk.dir` falta o es incorrecto, Gradle lo captura con un error convencional de SDK no encontrado.

### 3. Elige una variante de compilación

Dos flavours, cada uno apuntando a una `BASE_URL` distinta:

| Variante | Lee de | Úsala cuando… |
| :--- | :--- | :--- |
| **`stagingDebug`** | `BASE_URL_STAGING` (entregada como URL pública de mockable.io) | Quieres explorar la UI con respuestas predefinidas, sin necesidad de backend real. Útil para demos / CI. |
| **`productionDebug`** | `BASE_URL_PRODUCTION` (tu backend real) | Estás ejecutando la app contra tu propia instancia de backend (LAN local, cloud, etc.) — el camino que casi seguro quieres para pruebas end-to-end. |

Ambas variantes son depurables; la única diferencia es qué `BASE_URL` queda compilada en `BuildConfig`. Consulta [Variantes de compilación](#variantes-de-compilación) para la matriz completa 2 × 3 incluyendo los build types `release` / `benchmark`.

### 4. Compila, prueba, ejecuta

```bash
# Compila + instala un APK debug contra tu backend real (BASE_URL_PRODUCTION)
./gradlew :app:installProductionDebug

# …o instala contra el backend de demo de mockable.io (BASE_URL_STAGING)
./gradlew :app:installStagingDebug

# Ejecuta la suite completa de tests unitarios (~497 tests, ~30 s). La variante aquí es arbitraria —
# los tests no salen a la red; usan Ktor MockEngine.
./gradlew :app:testStagingDebugUnitTest

# Lint (filtra el baseline de compatibilidad de AGP-9)
./gradlew :app:lintStagingDebug

# Informe de cobertura → app/build/reports/jacoco/jacocoStagingDebugCoverageReport/html/
./gradlew :app:jacocoStagingDebugCoverageReport

# Build de release con R8 + reducción de recursos (~1,79 MB de APK)
./gradlew :app:assembleStagingRelease
```

Una vez instalada la app, lánzala desde el launcher, concede el permiso de micrófono cuando se solicite y pulsa el micro para grabar tu primera consulta. Si la respuesta no llega en streaming, la causa más probable es un backend inalcanzable en la `BASE_URL` compilada en la variante instalada — la app lo expone como un Snackbar de Nivel 1 con el mensaje de error del backend.

---

## Variantes de compilación

Dos product flavours × tres build types = **seis variantes**.

**Flavours** (dimensión de entorno):
- `staging` — `applicationIdSuffix = ".staging"`, instalable en paralelo a producción.
- `production` — sin sufijo.

Ambos flavours leen actualmente de la misma URL de backend (según [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) — la división de flavours del lado Android es una preocupación de organización de ingeniería, no un destino de despliegue distinto).

**Build types:**
- `debug` — build de desarrollo estándar. `enableUnitTestCoverage = true` activa la instrumentación de Jacoco.
- `release` — R8 + reducción de recursos. Requiere una configuración de firma de release para distribución externa; consulta [ROADMAP.md §Phase 11](ROADMAP.md) "Release-APK install validation" para el hueco pendiente.
- `benchmark` — replica `release` (R8 + resource-shrink) pero usa la configuración de firma de debug para que el APK sea instalable, y se marca `isDebuggable = false` para que ART pueda optimizar AOT el baseline profile. Lo usa exclusivamente el módulo productor `:baseline-profile`.

---

## Pruebas

La suite de pruebas es el artefacto de primera clase del proyecto. 497 pruebas unitarias, todas ejecutándose en JVM, todas en verde.

### Pruebas unitarias — JUnit 5 + MockK + Turbine

```bash
./gradlew :app:testStagingDebugUnitTest
```

Cubre:
- Domain en JVM puro (use cases, mappers, modelos, variantes de `AppError`)
- Integración repository / mapper usando `Ktor MockEngine` + Room en memoria
- Pruebas hoja de Compose (`createComposeRule` + Robolectric vía motor JUnit 4 Vintage)
- Pruebas del scaffold sin estado para `ChatScreenContent`
- Pruebas de fijado de locale (`A11yStringMigrationTest` / `EsTest` / `PtTest`) — cada string traducible fijado byte a byte
- Smoke test a nivel de Activity (`MainActivitySmokeTest`) vía `@HiltAndroidTest` + `ActivityScenario`

### Pruebas arquitectónicas

`DomainPurityTest` + `PresentationPurityTest` escanean los imports del código fuente en tiempo de pruebas y fallan si se viola la guarda de imports. Se ejecutan como parte de la tarea de pruebas unitarias — sin invocación separada.

### Cobertura

Jacoco cableado vía `enableUnitTestCoverage = true` nativo de AGP (Fase 10.2.A). La ruta del bytecode post-ASM en AGP 9 es `intermediates/classes/<variant>/transformClassesWithAsm/dirs`.

- Los paquetes de lógica de negocio (`domain/`, mappers + use cases + repository de `data/`) aterrizan en un 96–100 % de cobertura de líneas.
- Los paquetes de UI de Compose muestran 0 % porque Jacoco no puede trazar a través de la indirección de las lambdas de Compose — la UI SÍ se ejercita en los tests, pero la medición es ciega a ello.

Aún no hay puerta de cobertura en CI (deliberadamente diferida hasta que el baseline se estabilice). Consulta [ROADMAP.md §Phase 11](ROADMAP.md) "Jacoco CI coverage gate."

### Baseline profile

Módulo productor en [`baseline-profile/`](baseline-profile/). Profile generado en `app/src/stagingRelease/generated/baselineProfiles/baseline-prof.txt` (19.774 entradas, 692 líneas específicas de Luzia). Cubre arranque en frío → grafo de Hilt → render de ChatScreen → rotación de personas. La extensión para cubrir mic → transcribe → SSE → render es un follow-up de la fase 11 (el backend no era alcanzable durante la recolección inicial del profile).

### Convenciones y protocolos

- **Sin `Thread.sleep()`.** Usa `runTest` + `StandardTestDispatcher` y dispatchers inyectados por Hilt.
- **Runs de la suite completa después de cada transición a GREEN.** Los subconjuntos `--tests` dirigidos ocultan regresiones en pruebas distantes de fijado de locale o de jerarquías sealed. Capturado como auto-memoria `run_full_suite_after_green`.
- **Verificación en dispositivo obligatoria para tareas de UI / layout en Compose.** El paso de layout de Robolectric no simula fielmente el anclado de listas cortas, el comportamiento del IME, el clamping del contenedor padre o el wrapping del placeholder. Capturado como auto-memoria `ui_tasks_device_verification` con los antipatrones que afloraron en las fases 10.6.B / 10.6.C.

Protocolo completo en [TECHNICAL_SPEC.md §Testing Protocol](TECHNICAL_SPEC.md).

---

## CI / CD

Workflow de GitHub Actions en [.github/workflows/android-ci.yml](.github/workflows/android-ci.yml).

- **Disparadores:** `push` a `main`, `pull_request` contra `main`.
- **Runner:** `ubuntu-latest` + JDK 17 (Temurin) + `gradle/actions/setup-gradle@v4`.
- **Pipeline:** `Lint → Tests unitarios → Build` (`lintStagingDebug` → `testStagingDebugUnitTest` → `assembleStagingDebug`).
- **Artefactos:** informe de lint + informe de tests unitarios subidos en cada ejecución (incluyendo fallos) para inspección.
- **Concurrencia:** `cancel-in-progress` por rama para que las ejecuciones obsoletas no se acumulen en force-pushes.

Modo de fallo transitorio conocido: Maven Central ocasionalmente devuelve 403 a las IPs compartidas del runner de GitHub. Aflora como un error de config-cache porque la resolución de `jacocoAgent` falla antes de que Gradle pueda serializar la tarea de tests. Reintentar el job lo soluciona. Capturado durante el push del commit coordinado con el backend en la fase 10.6.H; consulta las notas de sesión si se repite.

---

## Decisiones de diseño

Las decisiones arquitectónicas duraderas se registran en [MEMORY.md](MEMORY.md) como **Forks** numerados. Lee el Fork relevante antes de reabrir un tema — cada Fork registra las opciones consideradas, lo que se eligió y lo que sobrevive o queda superado.

| Fork | Tema | Fase |
| :--- | :--- | :--- |
| 1 | Captura de persona por mensaje (sin turno `system` inyectado por el cliente) | 5.5 |
| 2 | Reintento inline de Nivel 2 (`onRetryLastFailure` + botón dedicado) | 5.5 |
| 3 | Gestión de permisos fusionada en `ChatScreen` (absorbe la fase 6) | 5.5 |
| 4 | Contrato de cable `/chat` revisado — tres campos `role` / `role_prompt` / `content` (API_SPEC v1.1.0) | 7.1 |
| 5 | `/transcribe` 400 para audio vacío / demasiado corto con mensaje literal (API_SPEC v1.2.0) | 7.2 |
| 6 | Acotación de las Reglas de Oro — excepción del TTS del lado de salida en las Reglas #1 y #6 | 10 |
| 7 | Campo `lang` opcional en `/chat` + `/transcribe` (API_SPEC v1.4.0) | 10.6.H |

Las reglas operativas (auto-comiteadas a un sistema de memoria de agentes) incluyen: contract-first al coordinar con el equipo de backend, división TDD en 3 commits (test / feat / docs) por sub-tarea, verificación en dispositivo obligatoria para trabajo de UI sensible a layout, ejecutar la suite completa de tests tras cada transición a GREEN. No son documentación del repo pero conforman cómo evolucionó el código.

---

## Mejoras futuras / Siguientes pasos

Backlog curado de elementos diferidos deliberadamente durante las fases 5 a 10 o aflorados como huecos de nivel sénior durante la construcción. Detalle completo con referencias al código, justificación e indicaciones sugeridas de implementación en [ROADMAP.md §Phase 11](ROADMAP.md).

**Visual / UX**
- Renderizado de Markdown en las burbujas de mensaje del asistente
- Auto-scroll del streaming a granularidad de token (el scroll actual se dispara por número de mensajes, no por crecimiento de contenido por token)
- Affordance de copiar al portapapeles en las respuestas del asistente

**Arquitectura / ingeniería**
- **Migración Room 2.x → Room 3.x.** Cambiar `androidx.room` → `androidx.room3` cuando 3.x alcance estable. APIs *coroutine-first*, nuevo `SQLiteDriver`, soporte KMP, `@DaoReturnTypeConverters`. Diferido desde la fase 4.3 porque 3.x estaba en alpha.
- Mejor soporte multiidioma (más locales, variantes regionales, override de idioma in-app, auditoría RTL, `<plurals>`).
- Personas / roles gestionados desde el backend (`GET /personas`) en vez de arrays de strings del lado del cliente.
- Suite de pruebas instrumentadas (`app/src/androidTest/`) en dispositivo real / emulador.
- Pruebas de captura de pantalla (Paparazzi o Roborazzi) para cobertura de regresión de tema.
- Validación de instalación del APK de release (R8 está activado pero el APK de release nunca se ha instalado en un dispositivo).

**Observabilidad y *production-readiness***
- Reporte de crashes (Firebase Crashlytics / Sentry) con redacción de PII.
- Watchdog de stream SSE — `withTimeout` alrededor de las lecturas de tokens para que el cliente no se cuelgue si el backend nunca emite `[DONE]`.
- Revisión de redacción del logging de Ktor para builds debug.

**Contenido y moderación**
- Filtrado de términos prohibidos / lenguaje malsonante — autoritativo en backend, consciente de locale, en dos capas (entrada de usuario + salida del asistente).

**Pruebas y tooling**
- Puerta de cobertura Jacoco en CI.
- Extensión del baseline profile cubriendo mic → `/transcribe` → SSE → render.
- Dependabot / Renovate para actualizaciones automatizadas de dependencias.
- Guarda de duración de audio previa a la subida (rechazo en cliente para audios cortos antes del upload multipart).

**Limpieza menor**
- Renombrado de la clave de string `cd_retry_reply` (la clave tiene prefijo `cd_` pero se usa como label de botón, no como contentDescription).
- Colapso editorial en el ROADMAP de las sub-tareas §10.6 dentro del resumen del padre.

---

## Documentación de referencia

Documentos en capas, cada uno con un propósito distinto. Empieza aquí y sigue los enlaces hacia abajo.

| Archivo | Propósito |
| :--- | :--- |
| **[CONTEXT.md](CONTEXT.md)** | Identidad del proyecto, Reglas de Oro, resumen del stack tecnológico, mapa de referencias. El "qué y por qué a 30.000 pies." |
| **[TECHNICAL_SPEC.md](TECHNICAL_SPEC.md)** | Contratos de API (a nivel de cable, strings de error fijadas en bytes), modelo de threading, estrategia de errores en 3 niveles, guarda de imports, protocolo de pruebas. El contrato entre el equipo Android y el equipo de backend. |
| **[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)** | Arquitectura de pantalla, especificaciones de componentes, interacción y motion, checklist de A11y. El contrato entre el equipo Android y UX. |
| **[ROADMAP.md](ROADMAP.md)** | Plan de ejecución fase por fase con notas de finalización por sub-tarea. La historia de ingeniería del proyecto. |
| **[MEMORY.md](MEMORY.md)** | Bitácora de decisiones — el *porqué* detrás de las elecciones no obvias. Forks numerados, anexados según se asientan. Léelo antes de reabrir una pregunta zanjada. |
| **[AGENTS.md](AGENTS.md)** | Protocolo operativo para desarrollo asistido por agentes de IA en este repo. Relevante si el repo lo retoma un flujo de trabajo de ingeniería aumentado por IA. |

---

*Última actualización como parte de la fase 11 (2026-04). Para cualquier sub-tarea más profunda que "qué archivo renderiza el botón de enviar", los cinco markdowns de arriba son más autoritativos que este README — describen el sistema en los términos en los que pensaban los ingenieros mientras lo construían.*
