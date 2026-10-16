---
feature: arrancar-app-en-mis-recetas
category: funcional
depends_on: []
status: ready
source: feature_list.json
---

# Spec: Arrancar la app en Mis Recetas

# PARTE A · CONTRATO

> QUÉ y POR QUÉ. Sin stack, arquitectura, nombres de archivos, esquemas, algoritmos ni firmas.

## Contexto y objetivo

What's for Lunch? aún no tiene código: esta feature crea la base de la aplicación Android y su pantalla de entrada. El usuario debe abrir la app y encontrarse en **Mis Recetas** con un estado vacío que le indique cómo empezar: crear una Receta o escanear la Despensa (`P18`; `docs/domain-model.md`, caso «Colección de Recetas vacía»). La pantalla se presenta con la identidad visual «Warm Culinary Craft» y con textos en español e inglés.

Esta feature fija también las decisiones de base pendientes del ⚠ S06, confirmadas por el usuario antes de redactar (Q-arrancar-app-en-mis-recetas-1).

## Actores y permisos

- **Usuario** (uso personal, 1-5 usuarios; sin cuentas ni login — `P31`): abre la app, activa las acciones del estado vacío y vuelve atrás. No requiere permisos de sistema en esta feature (la cámara llega con el Escáner).

## Historia

Cuando abro la app para decidir qué cocino hoy,
quiero encontrarme en Mis Recetas con una guía clara de cómo empezar,
para poder crear una Receta o escanear la Despensa.

## Requisitos funcionales

- RF-1 (Ubicuo): EL SISTEMA se ejecutará como una aplicación Android instalable en un dispositivo móvil.
- RF-2 (Ubicuo): EL SISTEMA mantendrá sus reglas de negocio separadas de la interfaz y de la persistencia, de modo que puedan verificarse sin pantalla ni almacenamiento.
- RF-3 (Ubicuo): EL SISTEMA aplicará el sistema visual «Warm Culinary Craft» (color, tipografía, espaciado y formas) a todos sus elementos visibles.
- RF-4 (Evento): CUANDO el usuario abre la aplicación, EL SISTEMA mostrará la pantalla Mis Recetas.
- RF-5 (Estado): MIENTRAS no exista ninguna Receta, EL SISTEMA mostrará el estado vacío de Mis Recetas con una acción para crear una Receta y otra para escanear la Despensa.
- RF-6 (Evento): CUANDO el usuario activa la acción de crear una Receta del estado vacío, EL SISTEMA abrirá el formulario de Receta.
- RF-7 (Evento): CUANDO el usuario activa la acción de escanear la Despensa del estado vacío, EL SISTEMA abrirá el Escáner.
- RF-8 (Evento): CUANDO el usuario vuelve desde el formulario de Receta o desde el Escáner, EL SISTEMA mostrará de nuevo Mis Recetas.
- RF-9 (Ubicuo): EL SISTEMA obtendrá todos sus textos visibles de recursos localizables.
- RF-10 (Evento): CUANDO el idioma del dispositivo es inglés, EL SISTEMA mostrará sus textos visibles en inglés.
- RF-11 (No deseado): SI el idioma del dispositivo no es español ni inglés, ENTONCES EL SISTEMA mostrará sus textos visibles en español.

## Requisitos no funcionales

- RNF-1: La app funciona en Android 11 (API 30) o superior, solo en móviles (`P20`; versión mínima fijada por el usuario, Q-arrancar-app-en-mis-recetas-2).
- RNF-2: Todos los elementos visibles cumplen contraste WCAG 2.1 AA y sus áreas táctiles miden al menos 48×48 dp (`DESIGN.md`).
- RNF-3: Toda la feature funciona sin conexión de red.

## Casos límite

- La app nace sin ninguna Receta y el estado vacío es lo primero que se ve → cubierto por RF-5.
- El destino de las acciones aún no tiene contenido real (llega en features posteriores) → cubierto por RF-6 y RF-7 con destinos provisionales (Q-arrancar-app-en-mis-recetas-3).
- El usuario vuelve atrás desde un destino → cubierto por RF-8.
- El idioma del dispositivo no es ni español ni inglés → cubierto por RF-11.
- El usuario cambia el idioma del dispositivo con la app instalada → los textos visibles se muestran en el nuevo idioma → cubierto por RF-10.
- Cambio de configuración (rotación de pantalla) → Mis Recetas y su estado vacío siguen mostrándose → cubierto por RF-4 y RF-5.

## Fuera de alcance

- El contenido y comportamiento del formulario de Receta y del Escáner (`crear-receta`, `escanear-despensa-con-camara`): aquí sus destinos son pantallas provisionales (decisión Q-arrancar-app-en-mis-recetas-3).
- El listado de Recetas con tarjetas, buscador, filtro por Momento culinario, badge de Coincidencia y FAB de creación (`crear-receta`, `buscar-y-filtrar-recetas`, `mostrar-badge-de-coincidencia`).
- La persistencia de Recetas y de la Despensa (llega con `crear-receta` y `gestionar-alimentos-de-la-despensa`).
- El cambio de idioma dentro de la app (`cambiar-idioma-de-la-interfaz`), los Ajustes y la API key de IA (`configurar-acceso-a-ia`, ⚠ S05).
- El tema oscuro: los tokens definen un único esquema claro (`uxdocs/DESIGN.md`).
- La adaptación a tablet, escritorio o web (`P20`).

## Criterios de finalización

- Los dos `verify` de la feature pasan en un emulador API 30+ mediante la suite E2E instrumentada.
- RF-3 queda verificada con captura del estado vacío comparada contra los tokens de `uxdocs/DESIGN.md`.
- `lint` y `unit` en verde y evidencia capturada (≤4 artefactos, solo de esta feature).

## Dudas abiertas

- Ninguna. Las consultas previas a la redacción quedaron resueltas por el usuario: Q-arrancar-app-en-mis-recetas-1 a Q-arrancar-app-en-mis-recetas-5 en `docs/risks-and-open-questions.md`.

## Trazabilidad

| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `steps`: «Crear el proyecto Android con Gradle y el módulo app…» | verify 1 (instalar y abrir en emulador) | informe con la salida de `init` |
| RF-2 | `steps`: «…con las capas ui, domain y data» | escenario propio: estructura de capas + unit sin pantalla | salida de `unit` |
| RF-3 | `steps`: «Aplicar el tema Warm Culinary Craft…» | escenario propio: captura comparada con los tokens | captura `api30-mis-recetas-vacio.png` |
| RF-4 | `description` | verify 1 | informe E2E |
| RF-5 | `description`; `steps`: «…su estado vacío y sus acciones…» | verify 1 | informe E2E + captura |
| RF-6 | `steps`: «…sus acciones hacia el formulario…» | escenario propio E2E: al pulsar la acción se muestra el destino provisional del formulario con su título y la vuelta atrás | informe E2E |
| RF-7 | `steps`: «…y el Escáner» | escenario propio E2E: al pulsar la acción se muestra el destino provisional del Escáner con su título y la vuelta atrás | informe E2E |
| RF-8 | derivado: caso límite | escenario propio E2E: al volver desde un destino provisional se recupera Mis Recetas con su estado vacío | informe E2E |
| RF-9 | `steps`: «Configurar los recursos strings.xml…» | escenario propio E2E: todos los textos visibles (incluidos los destinos provisionales) se afirman en español y en inglés contra el copy esperado; una cadena hardcodeada falla al menos una lengua | informe E2E |
| RF-10 | `steps`: «…con los textos en español e inglés» | verify 2 | informe E2E |
| RF-11 | derivado: caso límite | escenario propio E2E con idioma no soportado | informe E2E |

## Registro de iteraciones

- Iteración 1 (resolución de la auditoría `A RETRABAJAR` del 2026-10-16): la verificación de RF-9 en «Trazabilidad» pasa de `lint`/`HardcodedText` (ineficaz en Compose) a aserciones E2E bilingües es/en, con el mismo ajuste en «Enfoque técnico» y «Plan de verificación»; se explicitan las aserciones de RF-6, RF-7 y RF-8; se explicita MVVM + StateFlow (Q-arrancar-app-en-mis-recetas-1) en «Enfoque técnico» y T4; y se añade el copy es/en de los destinos provisionales en «Impacto visual».

# PARTE B · PLAN

> CÓMO. No contradice la Parte A ni añade comportamiento nuevo.

## Investigación del repositorio

### Archivos inspeccionados

- `init.ps1`, `lint.ps1`, `unit.ps1`, `e2e.ps1` — definen los comandos reales de arranque y verificación: `gradlew installDebug` / `lintDebug` / `testDebugUnitTest` / `connectedDebugAndroidTest`; exigen `gradlew` en la raíz y el APK en `app/build/outputs/apk/debug`.
- `CONTEXT.md` — estructura por capas prevista (S06, confirmado en Q-arrancar-app-en-mis-recetas-1), términos, convenciones y comandos.
- `docs/technical-discovery.md` — stack (Kotlin + Compose, Material 3, MVVM, Gradle Kotlin DSL) y su estado.
- `docs/domain-model.md` — «Colección de Recetas vacía → estado vacío con CTA para crear una Receta o escanear la Despensa».
- `DESIGN.md` y `uxdocs/DESIGN.md` — tokens YAML (colores, tipografías, espaciado, formas) y pautas del estado vacío.
- `feature_list.json` — pasos y verificaciones de la feature.

### Patrones existentes a seguir

- Módulo único `app` con capas `ui`/`domain`/`data` y `res/values`/`values-en` (`CONTEXT.md`; S06 confirmado en Q-arrancar-app-en-mis-recetas-1).
- Comandos del harness sin modificar: `init` solo arranca (compila, instala y abre); las comprobaciones viven en `lint`/`unit`/`e2e`.
- Terminología de `CONTEXT.md` («Mis Recetas», «Despensa», «Receta») en los textos visibles.
- Documentación en español; código e identificadores en inglés.

### Carencias actuales

- El repo no tiene código Android ni `gradlew`: todo el scaffold se crea en esta feature (⚠ S06, cuya estructura y comandos confirma el usuario en Q-arrancar-app-en-mis-recetas-1).
- Versiones concretas del SDK/AGP disponibles en el entorno sin verificar (el `init` actual se detiene por falta de `gradlew`).
- Ficheros de fuente Plus Jakarta Sans e Inter sin incorporar (licencia OFL; se descargan de Google Fonts) — necesarios por el requisito de funcionar sin red.
- El prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` no dibuja el estado vacío (laguna `RK04`): el estilo se resuelve con los tokens y las pautas de `DESIGN.md`, sin inventar uno nuevo.

## Enfoque técnico

Scaffold Android estándar en la raíz del repo: Gradle Kotlin DSL con wrapper, JDK 17, AGP 8.x y Kotlin 2.x con el plugin de Compose; módulo único `app` con `namespace`/`applicationId` `com.whatsforlunch.app`, `minSdk 30` y `compileSdk`/`targetSdk` 35 (provisional: si el SDK instalado no dispone de la plataforma 35, se usa la más alta disponible sin bajar de 30 — depende de Q-arrancar-app-en-mis-recetas-2). La separación por capas es estructural —`ui/` (Compose), `domain/` (Kotlin puro) y `data/` (fuentes futuras)— con **MVVM + StateFlow** en las pantallas, tal como confirma Q-arrancar-app-en-mis-recetas-1. La inyección de dependencias usa **Koin desde el scaffold** (decisión Q-arrancar-app-en-mis-recetas-4): `Application` de Compose con los módulos que esta feature necesita.

Tema «Warm Culinary Craft» en Compose a partir de los tokens YAML de `uxdocs/DESIGN.md` (mandan sobre su prosa, `P17`): `ColorScheme` con `primary #a83212`, `secondary #006c4a`, `tertiary #8d4b00` y las superficies cálidas de los tokens; tipografías Plus Jakarta Sans (títulos y labels) e Inter (cuerpo) empaquetadas en `res/font/` (sin descarga en runtime, por el requisito de funcionar sin red); espaciado `space-xs`…`space-xl` y radios de la escala `rounded`.

Pantalla Mis Recetas como destino de arranque con su estado vacío (título, mensaje y dos acciones). Cada acción navega a una pantalla provisional con título y vuelta atrás (decisión Q-arrancar-app-en-mis-recetas-3), que `crear-receta` y `escanear-despensa-con-camara` sustituirán por las pantallas reales. La navegación usa Compose Navigation con Mis Recetas como destino inicial y pila que permite volver (RF-8). El estado visible se reduce al vacío: no existen Recetas todavía (la persistencia llega en features posteriores); el listado con Recetas queda fuera.

Todos los textos visibles viven en `strings.xml`: `values/` (español, por defecto) y `values-en/` (inglés). El idioma sigue el del dispositivo y, si no es es ni en, se muestra español (RF-11). Android Lint no detecta literales en composables Compose (`HardcodedText` solo cubre layouts XML), así que la procedencia de los textos se verifica con aserciones E2E bilingües: cada texto visible se afirma en español y en inglés contra el copy esperado, de modo que cualquier cadena hardcodeada falla al menos una de las dos lenguas. `lint` se mantiene como puerta de calidad general, sin ser evidencia de RF-9.

## Archivos esperados

> Rutas provisionales: el repo aún no tiene bootstrap Android; la estructura sigue S06/Q-arrancar-app-en-mis-recetas-1.

- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`/`gradlew.bat`, `gradle/wrapper/*` — crear; scaffold Gradle con wrapper.
- `app/build.gradle.kts` — crear; módulo `app` con Compose y Koin (`minSdk 30`).
- `app/src/main/java/com/whatsforlunch/app/MainActivity.kt`, `ui/theme/` — crear; tema y entrada Compose (RF-3, RF-4).
- `app/src/main/java/com/whatsforlunch/app/ui/misrecetas/` — crear; pantalla Mis Recetas con estado vacío (RF-5).
- `app/src/main/java/com/whatsforlunch/app/ui/nav/` — crear; rutas y destinos provisionales (RF-6, RF-7, RF-8).
- `app/src/main/java/com/whatsforlunch/app/domain/` — crear; reglas de negocio sin dependencias de UI (RF-2).
- `app/src/main/java/com/whatsforlunch/app/data/` — crear; estructura de persistencia sin contenido aún (RF-2).
- `app/src/main/java/com/whatsforlunch/app/di/` — crear; módulos Koin (Q-arrancar-app-en-mis-recetas-4).
- `app/src/main/res/values/strings.xml`, `res/values-en/strings.xml`, `res/font/` — crear; textos es/en y fuentes (RF-9, RF-10).
- `app/src/androidTest/java/com/whatsforlunch/app/` — crear; suite E2E Compose (verify 1, verify 2, RF-6/7/8/11).
- `app/src/test/java/com/whatsforlunch/app/` — crear; test unitario de humo de `domain/` (RF-2).

## Impacto visual

- Fuente de diseño: `DESIGN.md` + tokens YAML de `uxdocs/DESIGN.md`; el prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` orienta app bar y tono, pero no dibuja el estado vacío (laguna `RK04` declarada; se cubre con «Estados a cubrir» de `DESIGN.md`).
- Pantallas o estados afectados: Mis Recetas (estado vacío, es/en), pantalla provisional del formulario de Receta, pantalla provisional del Escáner.
- Copy propuesto (decisión Q-arrancar-app-en-mis-recetas-5; se valida en la revisión visual):
  - Título de pantalla: «Mis Recetas» / «My Recipes»
  - Estado vacío, título: «Todavía no tienes recetas» / «No recipes yet»
  - Estado vacío, mensaje: «Crea tu primera receta o escanea la Despensa para empezar.» / «Create your first recipe or scan your Pantry to get started.»
  - Acción primaria: «Crear receta» / «Create recipe»
  - Acción secundaria: «Escanear la Despensa» / «Scan your Pantry»
  - Destino provisional del formulario: título «Crear receta» / «Create recipe» y acción de vuelta con descripción «Volver» / «Back»
  - Destino provisional del Escáner: título «Escanear la Despensa» / «Scan your Pantry» y acción de vuelta con descripción «Volver» / «Back»
- Fidelidad visual (NFR): los elementos visibles reproducen los tokens YAML (colores, tipografías, espaciado, radios) y las pautas de `DESIGN.md` (calidez táctil, targets ≥ 48×48 dp); se verifica con hasta 2 capturas en emulador API 30+ comparadas con los tokens.
- ¿Nuevo artefacto de diseño necesario?: no — los tokens YAML y las pautas de `DESIGN.md` cubren el estado vacío.

## Impacto en documentación duradera

- `ARCHITECTURE.md`: no necesario — la arquitectura vive en `CONTEXT.md` y `docs/technical-discovery.md`, fuentes de verdad del repo.
- `CONSTRAINTS.md`: no necesario — sin reglas DEBE nuevas más allá de las ya documentadas.
- `AGENTS.md`: no necesario — el flujo de agentes no cambia y los scripts `init`/`lint`/`unit`/`e2e` ya existen sin tocarse.
- Otros: `CONTEXT.md` — actualizar al implementar: el ⚠ S06 queda confirmado (Q-arrancar-app-en-mis-recetas-1) y debe reflejar el scaffold real. `docs/technical-discovery.md` — actualizar al implementar: la fila DI pasa de Hilt a **Koin** (Q-arrancar-app-en-mis-recetas-4) y se fija la versión mínima de Android (Q-arrancar-app-en-mis-recetas-2).

## Plan y tareas

- [x] T1: Crear el scaffold Gradle (wrapper, `settings`/`build.gradle.kts`, JDK 17, AGP 8.x, Kotlin 2.x + Compose) con el módulo `app` (`applicationId` `com.whatsforlunch.app`, `minSdk 30`, `compileSdk`/`targetSdk` 35) y los paquetes `ui`/`domain`/`data` (RF-1, RF-2).
- [x] T2: Configurar Koin (módulos de DI y `Application` de Compose con su arranque) (RF-2; Q-arrancar-app-en-mis-recetas-4).
- [x] T3: Implementar el tema «Warm Culinary Craft» en Compose desde los tokens YAML de `uxdocs/DESIGN.md`, con las fuentes Plus Jakarta Sans e Inter empaquetadas en `res/font/` (RF-3).
- [x] T4: Implementar Mis Recetas con su estado vacío y sus acciones bajo MVVM + StateFlow, la navegación a los destinos provisionales (formulario de Receta y Escáner) y la vuelta atrás (RF-4, RF-5, RF-6, RF-7, RF-8).
- [x] T5: Configurar `strings.xml` (`values/` es, `values-en/` en) con el copy propuesto y sin textos hardcodeados en el código (RF-9, RF-10, RF-11).
- [x] T6: Añadir la suite E2E instrumentada Compose: verify 1, verify 2, aserciones de RF-9 (todos los textos visibles, incluidos los destinos provisionales, afirmados en español y en inglés), escenarios de RF-6, RF-7, RF-8 y RF-11; y el test unitario de humo de `domain/` (RF-2).
- [x] T7: Ejecutar `init`, `lint` y `unit` en verde y actualizar `CONTEXT.md` y `docs/technical-discovery.md` (Q-arrancar-app-en-mis-recetas-1, Q-arrancar-app-en-mis-recetas-2, Q-arrancar-app-en-mis-recetas-4).

## Plan de verificación

- `./init.ps1` (o `./init.sh`) → compila, instala y abre la app en el emulador sin errores (verify 1, RF-1, RF-4).
- `./lint.ps1` (o `./lint.sh`) → verde (puerta de calidad general; no es evidencia de RF-9, ver «Enfoque técnico»).
- `./unit.ps1` (o `./unit.sh`) → verde (test de humo de `domain/`, RF-2).
- `./e2e.ps1` (o `./e2e.sh`; `gradlew connectedDebugAndroidTest`) → verde con: apertura en Mis Recetas con estado vacío (verify 1, RF-4, RF-5); textos visibles en inglés al poner el dispositivo en inglés (verify 2, RF-10; el test fuerza la configuración con `createConfigurationContext` o el locale del emulador vía `adb`, según el AVD); navegación de cada acción a su destino provisional (con su título y vuelta atrás) y regreso a Mis Recetas (RF-6, RF-7, RF-8); textos en español con idioma no soportado (RF-11); y aserciones de RF-9: cada texto visible de Mis Recetas, su estado vacío y los destinos provisionales afirmado en español y en inglés contra el copy esperado (una cadena hardcodeada falla la aserción en inglés).
- UI *(solo el validador)*: hasta 2 capturas en emulador API 30+ del estado vacío en español y en inglés, comparadas con los tokens YAML de `uxdocs/DESIGN.md` y las pautas de `DESIGN.md`, guardadas en `docs/evidence/arrancar-app-en-mis-recetas/api30-mis-recetas-vacio.png` y `docs/evidence/arrancar-app-en-mis-recetas/api30-mis-recetas-vacio-en.png`.
- Scripts de verificación: no se tocan; `init` solo arranca (compila, instala y abre), sin tests ni lint.

## Evidencia a capturar

> Presupuesto: ≤4 artefactos y solo de esta feature.

- Informe de la feature que resume `lint`/`unit`/`e2e` y la fidelidad visual (1 artefacto).
- Salida de la suite E2E con los tests de verify 1 y verify 2 y las aserciones de RF-9 (1 artefacto).
- UI: hasta 2 capturas del estado vacío en español y en inglés (`docs/evidence/arrancar-app-en-mis-recetas/`), capturadas por el validador.

## Checklist del validador

- [x] La implementación se queda dentro del alcance de esta spec.
- [x] Cada RF de la tabla de trazabilidad pasa su verificación.
- [x] La evidencia está presente, dentro del presupuesto (≤4) y solo de esta feature.
- [x] UI: evidencia visual (hasta 2 capturas de estados clave) y fidelidad a los tokens YAML de `uxdocs/DESIGN.md` y a las pautas de `DESIGN.md` (capturas de emulador API 30+).
- [x] Cobertura E2E añadida: suite instrumentada Compose con los `verify` de la feature.
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] No se añadió comportamiento ni trabajo de otras features.

## Clarificación — 2026-10-16 — veredicto: A RETRABAJAR (intento 1 de 3)

### Hallazgos

1. [5 Formato/trazabilidad (verificabilidad), bloqueante] RF-9, secciones «Requisitos funcionales», «Trazabilidad», «Enfoque técnico» y «Plan de verificación»: la comprobación estática citada (Android Lint, regla `HardcodedText`) solo detecta textos literalmente fijados en layouts XML; con una UI 100 % Compose no detecta cadenas literales en composables, así que la «salida de lint» en verde no permite al validador juzgar RF-9 (pasaría aunque existan textos hardcodeados) y el implementador tendría que inventar la comprobación real → resolver en redacción: cambiar la verificación de RF-9 por una efectiva en Compose (p. ej., aserciones E2E del texto visible completo de cada pantalla y estado —incluidos los destinos provisionales— en español y en inglés, y/o una regla lint/detekt propia) y reflejarlo en la fila RF-9 de Trazabilidad, en la línea de `lint` del «Plan de verificación» y en T5/T6.
2. [5 Formato/trazabilidad (deriva respecto al backlog), menor] «Enfoque técnico» y «Plan y tareas» (T4): la estructura confirmada en Q-arrancar-app-en-mis-recetas-1 incluye «MVVM + StateFlow» (`CONTEXT.md`, ⚠ S06), pero el plan solo recoge las capas `ui`/`domain`/`data` y no menciona el patrón de estado de las pantallas → observación: hacer explícito MVVM + StateFlow en el enfoque y en T4 para no derivar de lo confirmado.
3. [1 Ambigüedades, menor] «Impacto visual» (copy propuesto) y Q-arrancar-app-en-mis-recetas-3: el copy solo cubre Mis Recetas y su estado vacío; los textos visibles de los destinos provisionales (título de cada pantalla provisional y la acción de volver) quedan sin proponer pese a estar cubiertos por RF-9/RF-10/RF-11 y validarse en la revisión visual (Q-arrancar-app-en-mis-recetas-5) → observación: añadir su copy es/en o citar explícitamente su origen (terminología de `CONTEXT.md`).
4. [5 Formato/trazabilidad (verificabilidad), menor] Trazabilidad de RF-6, RF-7 y RF-8: la verificación «escenario propio E2E: pulsar la acción» no indica qué se afirma, y los RF enuncian «abrirá el formulario de Receta» / «abrirá el Escáner» mientras el comportamiento de esta feature es el destino provisional (Q-arrancar-app-en-mis-recetas-3) → observación: explicitar la aserción (al pulsar se muestra el destino provisional con título y vuelta atrás, y al volver se recupera Mis Recetas) para que el validador juzgue estos RF sin consultar el registro.

### Siguiente acción

Retrabajar la spec (A RETRABAJAR): resolver en redacción el hallazgo 1 y, de paso, las observaciones 2-4.

## Clarificación — 2026-10-16 — veredicto: APTA (intento 2 de 3)

### Hallazgos

Sin hallazgos bloqueantes ni menores. Los cuatro hallazgos del intento 1 quedan resueltos:

1. Bloqueante (bloque 5 · RF-9): sustituida la verificación estática ineficaz por aserciones E2E bilingües es/en de todos los textos visibles, incluidos los destinos provisionales («Trazabilidad» RF-9, «Enfoque técnico», «Plan de verificación» y T5/T6). El validador ya puede juzgar RF-9.
2. Menor (bloque 5 · Q-arrancar-app-en-mis-recetas-1): MVVM + StateFlow explícito en «Enfoque técnico» y T4.
3. Menor (bloque 1 · Q-arrancar-app-en-mis-recetas-3): copy es/en de los destinos provisionales añadido en «Impacto visual».
4. Menor (bloque 5 · RF-6/7/8): aserciones explícitas (destino provisional con título, vuelta atrás y recuperación de Mis Recetas) en «Trazabilidad».

Gate A y Gate B revisados: RF EARS atómicos y verificables, comportamiento de `description`/`steps` cubierto, «Fuera de alcance» con contenido, sin contradicciones ni conflictos con las restricciones de `docs/build-brief.md`, sin dudas bloqueantes abiertas (Q-arrancar-app-en-mis-recetas-1 a -5 `resuelta`).

### Siguiente acción

Continuar (APTA): la feature pasa a `ready`; puede implementarse según el plan.
