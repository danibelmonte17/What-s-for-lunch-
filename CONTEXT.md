# Contexto del proyecto

Contexto persistente para el agente: se carga antes de cada tarea. El detalle está en `docs/`. Lo previsto o no confirmado lleva `⚠`.

## Qué es

**What's for Lunch?** es una app **Android (Kotlin + Jetpack Compose)** que resuelve el «¿qué cocino hoy?» con los alimentos que ya hay en casa: se escanea la **Despensa**, la IA propone **Recetas** compatibles y se cocina en **Modo Cocina** manos libres. MVP de uso personal (1-5 usuarios), con datos locales, sin cuentas ni backend. Especificación completa en `docs/build-brief.md`.

## Términos

Lenguaje común: cada término significa lo mismo para toda persona o agente que trabaje aquí. Los documentos y el código usan estos términos tal cual.

### Producto

**Borrador**: Receta guardada que aún no está lista para cocinar; al publicarla queda disponible en Mis Recetas.
**Ingrediente**: componente de una Receta, con cantidad, unidad y si es obligatorio u opcional.
**Lista de la Compra**: ítems que faltan por comprar, agrupables por Pasillo o por Receta.
**Mis Recetas**: Recetas del usuario (creadas a mano o aceptadas desde una Sugerencia) y pantalla de entrada de la app.
_Evitar_: colección, catálogo.
**Modo Cocina**: guía de una Receta paso a paso, manos libres, con temporizadores y voz.
**Momento culinario**: clasificación de una Receta en desayuno, almuerzo, cena o postre.
**Paso**: instrucción secuencial de una Receta, con temporizador opcional.
**Pinche**: asistente conversacional de cocina con IA.
_Evitar_: Pinche IA, chatbot.
**Raciones**: número de comensales de una Receta; ajusta las cantidades (entre 1 y 12).
**Receta**: plato con ingredientes, pasos y metadatos; se crea a mano o se acepta desde una Sugerencia.
**What's for Lunch?**: la app.
_Evitar_: ChefEnCasa.

### Despensa y compra

**Alimento**: cada producto detectado o añadido a la Despensa (p. ej. «Tomate fresco»).
**Coincidencia**: porcentaje de ingredientes obligatorios de una Receta presentes en la Despensa.
_Evitar_: match. _No confundir con_: el badge «85% Match», que es el umbral mínimo de sugerencia.
**Despensa**: todos los alimentos del hogar en un único listado. No hay desglose por ubicaciones físicas.
_Evitar_: nevera (como inventario), inventario. _No confundir con_: Escaneo, que es la forma de llenarla con la cámara.
**Escaneo**: captura con la cámara que detecta Alimentos y los propone para la Despensa.
**Escáner**: pantalla y flujo donde se escanea la Despensa para obtener Sugerencias.
_No confundir con_: Escaneo, la captura concreta.
**Origen**: trazabilidad de un ítem de la Lista de la Compra («Para: Receta X» o «Despensa baja»).
**Pasillo**: categoría de supermercado para agrupar la Lista de la Compra (Verduras & Frescos, Carnes & Pescados, Lácteos, Despensa & Aceites).
**Sugerencia**: Receta propuesta por la IA a partir de la Despensa; al aceptarla se guarda en Mis Recetas.

### Asistencia y comportamiento

**Modo de degradación**: comportamiento ante un fallo de la IA o de la red: el flujo continúa con entrada manual (sin bloquear).

## Arquitectura

Estructura del scaffold Android ya creado (S06 confirmado en Q-arrancar-app-en-mis-recetas-1):

- `app/src/main/java/.../ui/`: pantallas Compose (Mis Recetas, Escáner, Detalle, Modo Cocina, Pinche, Formulario, Lista de la Compra, Ajustes).
- `app/src/main/java/.../domain/`: modelos y reglas de negocio (Coincidencia, Raciones, estados de Receta); no depende de la UI.
- `app/src/main/java/.../data/`: Room (Despensa, Recetas, Lista de la Compra), cliente de OpenRouter y detección MediaPipe.
- `app/src/main/res/values/` y `values-en/`: textos `strings.xml` (es/en).
- `docs/`: especificación (`build-brief.md`, `domain-model.md`, `technical-discovery.md`, `risks-and-open-questions.md`).
- `uxdocs/`: material de referencia UX (no se modifica).

Detalle y flujos: `docs/technical-discovery.md`.

## Dependencias y patrones

- Kotlin + Jetpack Compose (Confirmado) con Material 3 adaptado a los tokens de `uxdocs/DESIGN.md`.
- Room para datos locales; Retrofit/OkHttp para OpenRouter (API compatible con OpenAI); MediaPipe (Google AI Edge) para detección on-device.
- Voz con `SpeechRecognizer`/`TextToSpeech` nativos (`⚠ S04`).
- Patrón: MVVM + StateFlow con capas `ui`/`domain`/`data` (S06 confirmado en Q-arrancar-app-en-mis-recetas-1).
- Inyección de dependencias: Koin desde el scaffold (Q-arrancar-app-en-mis-recetas-4).

## Convenciones

- Documentación y specs en español; código, identificadores y mensajes técnicos en inglés.
- Commits en inglés con **Conventional Commits** (`feat:`, `fix:`, `docs:`…).
- La UI no contiene reglas de negocio: viven en `domain/` y en `docs/domain-model.md` (con IDs `RD##`).

## Comandos

Con Gradle Kotlin DSL y el wrapper del repo (los scripts `init`/`lint`/`unit`/`e2e` ya existen):

- Arrancar (compilar e instalar en dispositivo/emulador): `./gradlew installDebug`
- Compilar: `./gradlew assembleDebug`
- Tests unitarios: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Instrumentados/E2E: `./gradlew connectedDebugAndroidTest`

## Validación antes de dar algo por terminado

1. `lintDebug` y `testDebugUnitTest` en verde (y `assembleDebug` sin errores).
2. E2E en verde: pruebas instrumentadas Compose (`connectedDebugAndroidTest`) sobre emulador, con fixtures locales y cliente de IA simulado.
3. Si existe `feature_list.json`, actualizar el `status` de la feature solo después de lo anterior.

## Reglas de trabajo

- Prohibido: comandos destructivos fuera del directorio del proyecto; escribir credenciales, tokens o PII en el repositorio, logs o documentos (nunca la API key de OpenRouter); commits sin Conventional Commits; modificar `uxdocs/`.
- Pedir confirmación humana antes de: instalar en un dispositivo físico, distribuir la APK, o ejecutar llamadas de pago a la API de IA.
- Si la tarea toca algo marcado `⚠` en `docs/risks-and-open-questions.md`, preguntar en vez de asumir.
- Si un cambio altera estructura, comandos, convenciones o términos, actualizar este archivo en el mismo cambio.

## Qué consultar y qué evitar

- Consultar según la tarea: `docs/build-brief.md` (qué sí y qué no se construye), `docs/domain-model.md` (reglas de negocio `RD##`), `DESIGN.md` (interfaz), `docs/technical-discovery.md` (stack y su estado), `docs/risks-and-open-questions.md` (supuestos `S##` y riesgos `RK##`).
- Evitar: `uxdocs/` como destino de cambios (solo lectura), `.opencode/`, cualquier fichero con secretos.
