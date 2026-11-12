# Evidencia — mejorar-prompts-y-contratos-de-ia

Autoverificación del implementador (2026-11-12, rama `feature/mejorar-prompts-y-contratos-de-ia`). Spec: `docs/specs/mejorar-prompts-y-contratos-de-ia.md`.

## 1. Gates de build (dispositivo físico V2124, Android 13)

- `init.ps1`: **exit 0 «Entorno listo»** (app instalada y en ejecución en V2124).
- `testDebugUnitTest`: **52 suites / 405 tests / 0 fallos / 0 omitidos** (línea base 51/392 → +1 suite `AiContractPayloadTest`, +13 tests).
- `lintDebug`: **0 errores / 7 warnings**, todos preexistentes y fuera del diff (`UnusedResources` ×3 de los iconos de develop, `ExifInterface` ×2 + `ModifierParameter` del PR #39, `ObsoleteSdkInt` de `mipmap-anydpi-v26`).
- `assembleDebug` + `compileDebugAndroidTestKotlin`: **BUILD SUCCESSFUL**.

## 2. E2E focalizada de regresión (V2124, autorización del usuario 2026-11-12)

`connectedDebugAndroidTest` con filtro de 4 suites: **67 tests / 0 fallos / 0 errores / 0 omitidos** (log en los XML de `app/build/outputs/androidTest-results/connected/debug/`):

- `AiSuggestionsE2ETest` 17/17
- `ScanPantryE2ETest` 25/25
- `LanguageSwitchE2ETest` 8/8 (S5 toca idioma de prompts)
- `PincheE2ETest` 17/17

La E2E usa clientes simulados: verifica que la separación system/user, los delimitadores y los textos nuevos no rompen los consumidores. La calidad real de los prompts se verifica con el smoke test.

## 3. Smoke test de pago (autorización expresa del usuario, 2026-11-12)

Tres intentos contra `https://inference.devexpert.io/v1` con `AI_API_KEY` de `local.properties` (usada en proceso, **nunca impresa ni guardada**). Logs: `smoke-test-intento1.log` (intento 1) y `smoke-test.log` (intento 3, definitivo); **el log crudo del intento 2 se sobrescribió con el intento 3** (patrón N1 de la sesión 115) —sus resultados quedan citados íntegros en este apartado y el intento 1 conserva el rastro del primer rechazo de `json_schema`—.

### Intento 1 (13:26, `Invoke-WebRequest` de PS 5.1)

- Sugerencias `json_schema` → **HTTP 400 en 727 ms** (rechazo del proveedor) → fallback a `json_object` **disparado correctamente** (RF-MPC3), pero el reintento y la imagen fallaron con `NullReferenceException` de `Invoke-WebRequest` (bug de PS 5.1 al leer la respuesta), HTTP 0. Herramienta defectuosa, no del proyecto.

### Intento 2 (13:29, `HttpClient` de .NET — fallback verificado de extremo a extremo)

- Sugerencias `json_schema` (forma plana de la guía) → **HTTP 400 en 550 ms**, cuerpo:
  `litellm.BadRequestError: OpenrouterException - {"error":{"message":"response_format.json_schema is required when response_format.type is \"json_schema\".","code":400}}`
  → **fallback automático a `json_object` (RF-MPC3) → HTTP 200 en 16.444 ms** con JSON de receta válido (`mm=LUNCH`, `df=EASY`, ingredientes: los del usuario más despensa básica — aceite, sal, pimienta —; respeta cantidades).
- Escáner `json_schema` (forma plana) → **HTTP 400 en 296 ms** (mismo motivo) → **fallback → HTTP 200 en 3.278 ms** con `{"plátano":5}` sobre `banana.jpg` (un solo nombre, singular, sin no-alimentos).
- Imagen `model=prompt=size` (1024×1024) → **HTTP 200 en 4.568 ms**, `b64_json` guardado en **`smoke-imagen-generada.jpg`** (1024×1024; el proveedor devuelve bytes JPEG en `b64_json` —hallazgo L2 del validador, cerrado renombrando—): tortilla de patatas con cebolla, luz natural, primer plano, sin texto, sin marcas de agua, sin personas ni manos. **Los delimitadores `<<< >>>` no contaminan la imagen** (RF-MPC9 verificado visualmente).

### Hallazgo: forma del sobre `json_schema`

El alias `chat` (LiteLLM → OpenRouter) **sí soporta `json_schema`**, pero exige la **forma estándar de OpenAI** `{"type":"json_schema","json_schema":{"name":…,"schema":…}}`; la forma plana de la guía (`{"type":"json_schema","schema":…}`) devuelve 400. Spec D2 enmendada y código actualizado (`responseFormatJsonSchema(name, schema)`).

### Intento 3 (13:33, forma OpenAI — verificación definitiva)

- Sugerencias: **`json_schema` ACEPTADO (sin fallback) → HTTP 200 en 14.904 ms** con JSON válido: `mm=LUNCH`, `df=EASY`, ingredientes arroz/tomate/queso + aceite de oliva/agua/sal/pimienta/orégano (despensa básica) — RF-MPC2 y RF-MPC5 cumplidos en la respuesta real.
- Escáner: **`json_schema` ACEPTADO (sin fallback) → HTTP 200 en 5.020 ms** con `{"plátano":5}` — RF-MPC2 y RF-MPC7 cumplidos.

### Cobertura del smoke test

| Superficie | json_schema aceptado | Fallback verificado | Respuesta válida |
|---|---|---|---|
| Sugerencias | ✅ intento 3 | ✅ intento 2 (400 → json_object → 200) | ✅ contrato completo |
| Escáner | ✅ intento 3 | ✅ intento 2 (400 → json_object → 200) | ✅ mapa de cantidades |
| Imagen | n/a (guía 1 ya cumplida) | n/a | ✅ 200 + JPEG válido |

Total: 3 llamadas generativas (2 intentos de chat + 1 imagen) + 2 rechazos baratos de `json_schema` + 1 reintento de cada chat con `json_object`; sin PII (datos sintéticos), sin secretos en los logs.

## 4. Cambios de código verificados

- 12 assets `app/src/main/assets/prompts/` reescritos (pares es/en juntos, sin BOM, paridad intacta).
- `RecipeContract.kt`: `recipeJsonSchema()`, `scanJsonSchema()`, `responseFormatJsonSchema(name, schema)`, `responseFormatJsonObject()`, `shouldFallbackToJsonObject()`.
- `OpenAiCompatibleRecipeSuggester.kt` / `OpenAiCompatibleScanImageAnalyzer.kt`: mensajes `system`/`user`, builders puros `buildBody`, reintento único con `json_schema` → `json_object` en 4xx.
- `PromptSource.kt`: `splitSystemUser`, `wrapUserData`, delimitadores en `renderProfilePrompt`/`renderDishPhotoPrompt`.
- `PincheChatProtocol.kt` + `OpenAiCompatiblePincheChatClient.kt`: `toolResultMessage` (rol `tool` + `tool_call_id`).
- Tests: `AiContractPayloadTest` (nueva, 7 tests), `PromptAssetsTest` (+paridad de marcadores/secciones y delimitadores), `SuggestionPromptTest` (system/user), `PinchePromptsTest` (+secciones y reglas de perfil), `PincheChatProtocolTest` (+rol `tool`).

Sin dependencias ni permisos nuevos, sin cambios en dominio/Room/UI, sin secretos en el diff.
