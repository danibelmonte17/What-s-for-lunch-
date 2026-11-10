# Feature Implementation Spec: Mostrar badge de coincidencia

## Source Feature

- `id`: mostrar-badge-de-coincidencia
- `area`: funcional
- `depends_on`: crear-receta (`accepted`), gestionar-alimentos-de-la-despensa (`accepted`)
- `status`: `not_started`
- `source`: `feature_list.json`

## Goal

Cada tarjeta de Receta de Mis Recetas muestra su **Coincidencia** con la Despensa: un badge con el porcentaje de ingredientes obligatorios presentes en la Despensa, calculado según RD01 y RD10 (los opcionales no cuentan). Cuando todos los obligatorios están disponibles (100 %), el badge muestra «100% Match» (RD02). El cálculo vive en `domain/` (regla de negocio reutilizable por `mostrar-recetas-compatibles`) y se recalcula de forma reactiva cuando cambian la Despensa o las Recetas.

## Non-Goals

- El umbral de sugeribilidad ≥ 85 % de RD02 (qué Recetas se *sugieren*) y el carrusel del Escáner con los badges 100 %/85 %: son de `mostrar-recetas-compatibles`.
- Ordenar la lista por Coincidencia: se conserva el orden actual (el orden lo cubre `buscar-y-filtrar-recetas`).
- Matching semántico entre nombres de Ingrediente y Alimento (sinónimos, singular/plural, subcadenas): solo igualdad de nombre normalizado (D4).
- Desglose de ingredientes disponibles/faltantes en la tarjeta ni en el Detalle (el prototipo muestra el chip «N ingredientes esenciales disponibles en despensa»; fuera de alcance).
- Cantidades: la Coincidencia solo mira presencia en la Despensa (D1); no compara cantidades ni escalado de Raciones (RD03).
- Cambios en el formulario de Recetas, en la Despensa, en el Escáner o en la Lista de la Compra.
- Copiar pixel a pixel del prototipo: el badge no existe en `uxdocs/chefencasa_mis_recetas_y_crud/code.html` (ver «Visual Design Impact»); aquí se fija un estilo concreto con los tokens existentes.

## Job Story

Cuando miro Mis Recetas y quiero decidir qué cocinar hoy,
quiero ver de un vistazo qué porcentaje de los ingredientes obligatorios de cada Receta tengo en la Despensa,
para elegir una Receta que pueda cocinar ya o saber lo que me falta.

## Users And Permissions

- **Usuario** (único rol del MVP, propietario del dispositivo; sin cuentas ni roles, `P31`): ve el badge en sus propias Recetas (Borradores y Publicadas). Nada de esta feature exige permisos adicionales.

## Requisitos funcionales

Prefijo propio `RF-MB#` para no colisionar con los `RF-#` de `crear-receta.md` que ya se citan en comentarios de código.

- **RF-MB1** (RD01): `Coincidencia % = obligatorios disponibles / obligatorios totales × 100`. Redondeo al entero más próximo (D2). Ej.: 2 de 4 → 50 %; 2 de 3 → 67 %.
- **RF-MB2** (RD10): los ingredientes **opcionales** no entran ni en el numerador ni en el denominador.
- **RF-MB3**: un obligatorio está «disponible» si la Despensa contiene un Alimento con el mismo nombre normalizado (`normalizeName`, `domain/text/NameNormalization.kt`: trim, minúsculas, sin tildes, espacios colapsados). La cantidad del Alimento no influye (D1).
- **RF-MB4**: toda tarjeta de Receta de Mis Recetas muestra el badge con el porcentaje calculado (`«50%»`), en la lista filtrada y sin filtrar.
- **RF-MB5** (RD02): al llegar al 100 % el badge muestra el literal `«100% Match»` (D5).
- **RF-MB6**: Receta sin ingredientes obligatorios → el badge no se muestra (Coincidencia indefinida, D3).
- **RF-MB7**: el cálculo es reactivo: al cambiar la Despensa (alta/baja/edición de Alimentos) o los ingredientes de una Receta, el badge se actualiza sin acción manual del usuario.

## Acceptance Scenarios

### Scenario 1: Mitad de obligatorios en la Despensa (verify 1 de `feature_list.json`)

Given una Receta con 2 ingredientes obligatorios («Pollo», «Arroz») y solo «Pollo» en la Despensa
When el usuario abre Mis Recetas
Then la tarjeta de esa Receta muestra el badge «50%» (RF-MB1, RF-MB4)

### Scenario 2: Todos los obligatorios disponibles (RD02)

Given una Receta cuyos ingredientes obligatorios están todos en la Despensa
When el usuario abre Mis Recetas
Then la tarjeta muestra el badge «100% Match» (RF-MB5)

### Scenario 3: Opcionales que no alteran el porcentaje (verify 2 de `feature_list.json`)

Given una Receta con 2 obligatorios (uno disponible) y 2 opcionales, con uno de los opcionales también en la Despensa
When se calcula la Coincidencia
Then sigue siendo 50 %: quitar o poner el opcional en la Despensa no cambia el badge (RF-MB2)

### Scenario 4: Despensa vacía

Given una Receta con obligatorios y la Despensa vacía
When el usuario abre Mis Recetas
Then la tarjeta muestra «0%» (RF-MB1)

### Scenario 5: Actualización reactiva al cambiar la Despensa

Given una Receta con badge «50%»
When el usuario añade a la Despensa el Alimento que falta (nombre normalizado igual al del Ingrediente)
Then el badge de la tarjeta pasa a «100% Match» sin reiniciar la app (RF-MB7)

### Scenario 6: Receta sin obligatorios

Given una Receta cuyos ingredientes son todos opcionales
When se muestra su tarjeta
Then no aparece badge de Coincidencia (RF-MB6)

### Scenario 7: Coincidencia por nombre normalizado

Given un Ingrediente obligatorio «Tomate fresco » y un Alimento «tomate fresco» en la Despensa
When se calcula la Coincidencia
Then el obligatorio cuenta como disponible por igualdad de nombre normalizado (RF-MB3)

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Despensa vacía | Badge «0%» en toda Receta con obligatorios (S4) |
| Receta sin ingredientes obligatorios | Sin badge: el porcentaje no se puede calcular (RF-MB6, D3) |
| Receta sin ingredientes (inválida salvo borrador) | Sin badge: 0 obligatorios = mismo caso que el anterior |
| Alimento con cantidad 0 («sin cantidad») | Cuenta como disponible: la Coincidencia es de presencia (D1) |
| Nombres con tildes, mayúsculas o espacios distintos | Equivalentes tras `normalizeName` (RF-MB3) |
| Nombres distintos («Pollo» vs «Pechuga de pollo») | **No** coinciden: igualdad estricta normalizada (D4, limitación conocida) |
| Alimentos duplicados en la Despensa | Imposible por la fusión de duplicados (Q-gestionar-alimentos-de-la-despensa-3); si ocurriera, cuenta una vez |
| Porcentajes no enteros (2 de 3) | Redondeo al entero más próximo → «67%» (D2) |
| Borradores y Publicadas | Ambos muestran el badge: depende solo de ingredientes y Despensa |
| Búsqueda/filtro activos | El badge es por Receta; filtrar no lo altera ni lo oculta |

## Repository Research

### Files Inspected

- `feature_list.json` — `steps` y `verification` de la feature; dependencias en `accepted`.
- `docs/domain-model.md` — RD01, RD02, RD10 y la fórmula de Coincidencia (ej. 3/4 → 75 %).
- `CONTEXT.md` — definición de Coincidencia («obligatorios … **presentes** en la Despensa») y vocabulario («match» → Coincidencia; el badge literal «… Match» es la excepción de interfaz).
- `docs/build-brief.md` — Mis Recetas con «tarjetas con badge de Coincidencia»; Escáner con carrusel (100 %/85 %, fuera de alcance).
- `docs/technical-discovery.md` — comandos Gradle (`installDebug`, `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`) envueltos por los scripts del harness.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — `RecipeCard` (MomentBadge y StatusPill como píldoras `PillShape`, `MyRecipesTestTags`), `RecipeList` y estados Empty/WithRecipes/NoResults.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesViewModel.kt` — `MyRecipesUiState`, `combine` de 5 flows (límite de sobrecargas tipadas de `combine`), constructor `MyRecipesViewModel(RecipeRepository)`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesRoute.kt` — wiring con Koin.
- `app/src/main/java/com/whatsforlunch/app/domain/model/Recipe.kt`, `RecipeIngredient.kt` (`optional: Boolean`), `PantryItem.kt` (`quantity: Int?`).
- `app/src/main/java/com/whatsforlunch/app/domain/text/NameNormalization.kt` — `normalizeName` (ya usado para búsqueda y fusión de duplicados).
- `app/src/main/java/com/whatsforlunch/app/domain/repository/RecipeRepository.kt`, `PantryRepository.kt` — ambos exponen `Flow<List<…>>` observables (reactividad gratis).
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — `viewModel { MyRecipesViewModel(get()) }`.
- `app/src/main/res/values/strings.xml`, `values-en/strings.xml` — claves `my_recipes_card_*` y convención de textos es/en.
- `app/src/main/java/com/whatsforlunch/app/ui/theme/Color.kt` — tokens: `Secondary` #006C4A, `OnSecondary` #FFFFFF, `SecondaryContainer` #82F5C1, `OnSecondaryContainer` #00714E.
- `app/src/androidTest/java/com/whatsforlunch/app/SearchAndFilterRecipesE2ETest.kt`, `DeleteRecipeE2ETest.kt` y `app/src/test/.../MyRecipesSearchDebounceTest.kt` — construyen `MyRecipesViewModel(repository)` directamente (3 call sites; cambia la firma).
- `uxdocs/chefencasa_mis_recetas_y_crud/code.html` — la tarjeta del prototipo **no** tiene badge de porcentaje (tiene el chip «N ingredientes esenciales disponibles en despensa»).
- `uxdocs/chefencasa_esc_ner_de_nevera_ia/code.html` — píldoras de confianza `%` (`bg-secondary-container text-on-secondary-container rounded-full font-bold`) y «100% COINCIDENCIA» como referencia de estilo del badge.
- `uxdocs/DESIGN.md` — badges superpuestos arriba a la derecha sobre la imagen de la tarjeta; tokens YAML.

### Existing Patterns To Follow

- MVVM + StateFlow; reglas de negocio en `domain/` sin dependencias de UI (comentarios citan `RD##`); Koin inyecta los ViewModels.
- Funciones de dominio puras y testeadas en unit (`RecipeSearchFilterTest`, `PantryItemValidationTest`); repos in-memory para E2E (`InMemoryRecipeRepository`, `InMemoryPantryRepository`).
- Píldoras de tarjeta con `Surface(shape = PillShape)` + `labelSmall` bold (`StatusPill`, `MomentBadge`) y `testTag` en `MyRecipesTestTags`.
- Textos en `strings.xml` es/en; identificadores de código en inglés (`P19`, `P25`).
- Comandos: scripts `init`/`lint`/`unit`/`e2e` del harness sobre `./gradlew`.

### Current Gaps

- No existe ningún cálculo de Coincidencia en el código: se crea la regla en `domain/` desde cero.
- `MyRecipesViewModel` no observa la Despensa hoy: hay que añadir `PantryRepository` a su constructor (y a los 3 call sites de tests) y al módulo Koin.
- `combine` tipado llega hasta 5 flows; con la Despensa son 6 (ver «Technical Approach»).

## Technical Approach

- **Regla de dominio** — nuevo `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` (paquete `domain/match/` reutilizable por `mostrar-recetas-compatibles`):

  ```kotlin
  // Coincidencia de una Receta con la Despensa (RD01, RD10; badge según RD02).
  data class RecipeMatch(
      val percentage: Int,          // 0..100, redondeado (D2)
      val availableRequired: Int,
      val totalRequired: Int,
  ) { val isFullMatch: Boolean get() = percentage == 100 }

  // null cuando la Receta no tiene obligatorios (RF-MB6).
  fun calculateMatch(recipe: Recipe, pantryItems: List<PantryItem>): RecipeMatch?
  ```

  Implementación: filtrar `recipe.ingredients` por `!optional` (RD10); si la lista queda vacía → `null`; disponible = `pantryItems.any { normalizeName(it.name) == normalizeName(ingredient.name) }` (RD01, RF-MB3); `percentage = (availableRequired * 100 / totalRequired).roundToInt()` equivalente con redondeo half-up (D2).
- **ViewModel** — `MyRecipesViewModel(repository: RecipeRepository, pantryRepository: PantryRepository)`. Como `combine` tipado admite 5 flows, combinar primero `combine(repository.observeRecipes(), pantryRepository.observePantryItems()) { r, p -> r to p }` y ese flow con los 4 restantes (búsqueda aplicada, filtro, error y el flujo de debounce existente). `MyRecipesUiState` gana `matches: Map<String, RecipeMatch> = emptyMap()` (id de Receta → match; sin entrada ⇒ sin badge). `visibleRecipes: List<Recipe>` no cambia de tipo para no romper `MisRecetasState` ni el test de debounce.
- **UI** — nuevo composable `MatchBadge(match: RecipeMatch)` en `MyRecipesScreen.kt`, dentro de `RecipeCard` con doble ubicación espejo de `MomentBadge` (RF-MB4): superpuesto `Alignment.TopEnd` sobre la portada cuando hay foto (los badges superpuestos van arriba a la derecha, `uxdocs/DESIGN.md`); sin foto, inline en la fila del título antes de `StatusPill`. `RecipeCard` recibe `match: RecipeMatch?` y `RecipeList` el mapa `matches`. `MyRecipesTestTags.RECIPE_MATCH = "my_recipes_card_match"`.
- **Textos** — `values/strings.xml` y `values-en/strings.xml`: `my_recipes_card_match` = `%1$d%%` (ambos idiomas), `my_recipes_card_match_full` = `100% Match` (ambos idiomas, literal de RD02, D5) y `my_recipes_card_match_content_description` = «Coincidencia con la Despensa: %1$d%%» / «Pantry match: %1$d%%» (accesibilidad y `uiautomator`).
- **Tests** — unit sobre `calculateMatch` (fórmula RD01/RD10, normalización, redondeo, casos borde) y E2E Compose con repos in-memory sembrados (patrón de `SearchAndFilterRecipesE2ETest`): receta con 2 obligatorios + 1 opcional y Despensa con 1 obligatorio + 1 opcional → badge «50%» (verify 1); el opcional no altera (verify 2 también en unit).

### Decisiones (para no reabrir el debate en implementación)

- **D1 — presencia, no cantidad**: un Alimento cuenta aunque su cantidad sea 0 («sin cantidad»): CONTEXT define la Coincidencia sobre ingredientes «presentes en la Despensa».
- **D2 — redondeo** half-up al entero más próximo (`roundToInt`).
- **D3 — sin obligatorios**: badge oculto (0/0 indefinido; «100%» sería engañoso).
- **D4 — matching estricto**: igualdad de `normalizeName(nombre)`; sin sinónimos ni substrings (consistente con la fusión de duplicados de la Despensa). Limitación conocida y aceptada para el MVP.
- **D5 — «100% Match» literal** en es y en: RD02 y CONTEXT citan el badge con la palabra «Match».
- **D6 — ubicación**: portada arriba-derecha con foto; fila del título sin foto (mismo patrón dual que `MomentBadge`).

## Expected File Changes

- `app/src/main/java/com/whatsforlunch/app/domain/match/RecipeMatch.kt` — crear; regla RD01/RD10 + resultado `RecipeMatch`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesViewModel.kt` — modificar; `PantryRepository`, `matches: Map<String, RecipeMatch>` en `MyRecipesUiState`.
- `app/src/main/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesScreen.kt` — modificar; `MatchBadge`, doble ubicación en `RecipeCard`, `RECIPE_MATCH` en `MyRecipesTestTags`.
- `app/src/main/java/com/whatsforlunch/app/di/AppModule.kt` — modificar; `viewModel { MyRecipesViewModel(get(), get()) }`.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml` — modificar; 3 claves nuevas.
- `app/src/test/java/com/whatsforlunch/app/domain/match/RecipeMatchTest.kt` — crear; unit de la regla.
- `app/src/test/java/com/whatsforlunch/app/ui/myrecipes/MyRecipesSearchDebounceTest.kt` — modificar; firma del constructor (2 call sites).
- `app/src/androidTest/java/com/whatsforlunch/app/MatchBadgeE2ETest.kt` — crear; verify 1 y casos visibles (S1, S2, S3, S5).
- `app/src/androidTest/java/com/whatsforlunch/app/SearchAndFilterRecipesE2ETest.kt`, `DeleteRecipeE2ETest.kt` — modificar; firma del constructor (3 call sites).
- `docs/validations/mostrar-badge-de-coincidencia.md` y `docs/evidence/mostrar-badge-de-coincidencia/*.png` — crear al validar (evidencia).

## Visual Design Impact

- UI involved: yes
- Design source: tokens de `uxdocs/DESIGN.md` (mandan, `P17`) y patrones de píldora ya implementados (`MomentBadge`, `StatusPill`); el prototipo `uxdocs/chefencasa_mis_recetas_y_crud/code.html` **no** incluye badge de porcentaje → planning gap: no hay referencia 1:1 y este spec fija el estilo para que el implementer no lo invente. Referencia de sabor: píldoras de confianza del Escáner (`bg-secondary-container`, `font-label-sm`, `rounded-full`, bold) y «100% COINCIDENCIA».
- Screens or states affected: tarjetas de Mis Recetas (lista y resultados de búsqueda); Empty y NoResults no cambian.
- New design artifact required: no — el estilo queda definido aquí con los tokens existentes.

Especificación del badge (verificable por `uiautomator` y captura; sin exigir muestreo de píxeles):

- Píldora (`PillShape`, radio completo), padding horizontal `WarmSpacing.sm` y vertical 2 dp (idéntico a `StatusPill`), etiqueta `labelSmall` (11 sp) en negrita.
- **0–99 %**: fondo `secondaryContainer` (#82F5C1), texto `onSecondaryContainer` (#00714E), texto `«N%»` (p. ej. `«50%»`).
- **100 %**: fondo `secondary` (#006C4A), texto `onSecondary` (#FFFFFF), texto `«100% Match»` (destaca como en el prototipo del Escáner).
- Posición (D6): superpuesto arriba a la derecha de la portada con margen `WarmSpacing.md` (contrapunto de `MomentBadge`, que va arriba a la izquierda); sin foto, inline en la fila del título antes de `StatusPill`.
- `testTag` `my_recipes_card_match`; descripción accesible «Coincidencia con la Despensa: N%».

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed — no existe y las capas no cambian (la regla va a `domain/match/`, dentro de la convención existente de `domain/`).
- `CONSTRAINTS.md`: not needed — no existe y la feature no introduce reglas MUST/MUST NOT nuevas.
- `AGENTS.md`: not needed — sin cambios de flujo, comandos ni convenciones.
- Other docs: `docs/domain-model.md` — not needed (RD01/RD02/RD10 ya existen; D1-D6 son decisiones de esta spec). `docs/risks-and-open-questions.md` — not needed; no hay dudas `⚠` nuevas abiertas (D4 queda como limitación documentada aquí).

## Implementation Plan

1. Crear la regla en `domain/match/RecipeMatch.kt` (RF-MB1 a RF-MB3, D1-D4) con sus unit tests (`RecipeMatchTest`).
2. Ampliar `MyRecipesViewModel` con `PantryRepository` y el mapa `matches` (RF-MB7), y adaptar `AppModule` y los 3 call sites de tests existentes.
3. Implementar `MatchBadge` en `RecipeCard` con su `testTag`, textos es/en y las dos ubicaciones (RF-MB4 a RF-MB6, D5-D6).
4. Crear `MatchBadgeE2ETest` con repos in-memory sembrados (verify 1 y S2/S3/S5) y dejar la regresión completa en verde.
5. Ejecutar el harness completo, capturar la evidencia y dejar la feature en `passing` lista para `feature-validator`.

## Implementation Tasks

- [ ] T1: `RecipeMatch.kt` + `RecipeMatchTest` (fórmula RD01, RD10, normalización, redondeo, `null` sin obligatorios, cantidad 0). (S1-S4, S6, S7)
- [ ] T2: `MyRecipesViewModel` + `AppModule` + firma en tests existentes; `matches` reactivo al flujo de la Despensa. (S5, RF-MB7)
- [ ] T3: `MatchBadge` en `MyRecipesScreen` con textos es/en, `testTag` y doble ubicación. (S1, S2, S4, S6)
- [ ] T4: `MatchBadgeE2ETest` (verify 1 con «50%»; «100% Match»; opcional que no altera; actualización al cambiar la Despensa) sin romper la suite existente.
- [ ] T5: `init`/`lint`/`unit`/`e2e` en verde y evidencia capturada (el muestreo visual lo ejecuta el validador).

## Verification Plan

- `./init.ps1` / `./init.sh` → exit 0 (gate de arranque estándar no bloqueante; no arranca procesos duraderos y no se modifica; artefactos conocidos: carrera del `pidof` en `init.sh` y exit 1 falso de `init.ps1` por el stderr de `adb shell monkey` — ejecutar con `cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File init.ps1 > .harness\init.log 2>&1"`).
- `./lint.ps1` / `./unit.ps1` (o `.sh`) → exit 0; `unit` incluye `RecipeMatchTest` y los suites existentes (`MyRecipesSearchDebounceTest`, `RecipeSearchFilterTest`, `MisRecetasStateTest`, …).
- E2E persistente: `./e2e.ps1` / `./e2e.sh` (`connectedDebugAndroidTest`, emulador API 30+) con `MatchBadgeE2ETest`:
  - verify 1 (S1): Receta con 2 obligatorios («Pollo», «Arroz») y opcional («Limón»); Despensa con «Pollo» y «Limón» → la tarjeta muestra «50%» (el opcional en la Despensa no sube el porcentaje).
  - verify 2 (S3, también en unit): quitar/añadir «Limón» de la Despensa no cambia el badge.
  - S2: con «Arroz» también en la Despensa → «100% Match»; S5: el badge se actualiza al cambiar la Despensa; S7: nombre con distinta caja/acentos/espacios cuenta como disponible.
  - Unit: fórmula completa de RD01/RD10, redondeo (2 de 3 → 67), `null` sin obligatorios, cantidad 0 cuenta (D1), Despensa vacía → 0 %.
  - Regresión completa en verde (suite `connectedDebugAndroidTest`, hoy 68 tests) sin romper `crear-receta`, `editar-receta`, `eliminar-receta`, `buscar-y-filtrar-recetas` ni `gestionar-alimentos-de-la-despensa`.
- UI (solo el validador): captura de la tarjeta con badge parcial y de la de «100% Match» (es) + `uiautomator` (posición, textos, `testTag`); sin pipeline de screenshot testing en el repo (justificado por el precedente de `afinar-fidelidad-visual-del-formulario`).
- Cobertura E2E: sí se añade, porque la feature cambia comportamiento visible en Mis Recetas.
- Los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se tocan.

## Evidence To Capture

- `docs/validations/mostrar-badge-de-coincidencia.md` — informe con `lint`/`unit`/`e2e` (nombres de test) y verificación por escenario S1-S7; se resume en `evidence` de `feature_list.json` al aceptar.
- `docs/evidence/mostrar-badge-de-coincidencia/mis-recetas-badge-50.png` — tarjeta con el badge «50%» (es).
- `docs/evidence/mostrar-badge-de-coincidencia/mis-recetas-badge-100-match.png` — tarjeta con el badge «100% Match» (es).
- Presupuesto ≤ 3 artefactos y solo de esta feature (1 informe + 2 capturas).

## Trazabilidad

| `feature_list.json` | Requisito | Escenario | Tarea | Verificación |
|---|---|---|---|---|
| Step 1 (RD01/RD10) | RF-MB1, RF-MB2, RF-MB3 | S1, S3, S7 | T1 | unit `RecipeMatchTest` |
| Step 2 (badge según RD02) | RF-MB4, RF-MB5, RF-MB6 | S1, S2, S4, S6 | T3 | E2E + captura |
| Verify 1 (E2E: mitad → porcentaje) | RF-MB1, RF-MB4 | S1 | T4 | `MatchBadgeE2ETest` |
| Verify 2 (unit: opcionales) | RF-MB2 | S3 | T1 | `RecipeMatchTest` |
| — (reactividad) | RF-MB7 | S5 | T2, T4 | `MatchBadgeE2ETest` |

## Validator Checklist

- [ ] La implementación se queda dentro del alcance de esta feature.
- [ ] Los escenarios de aceptación pasan (S1-S7) y las decisiones D1-D6 se respetan.
- [ ] La evidencia de verificación está presente, dentro del presupuesto y solo de esta feature.
- [ ] Cobertura E2E persistente añadida y suite completa en verde.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento de producto ni trabajo de otras features (en particular: sin umbral 85 %, sin ordenación por Coincidencia, sin carrusel del Escáner).

---

## Enmienda — `redisenar-formulario-de-receta` (2026-11-09)

Decisión posterior del usuario (D5 de `docs/specs/redisenar-formulario-de-receta.md`): desaparece el concepto de ingrediente opcional, así que la **Coincidencia se calcula sobre todos los Ingredientes** (RD01/RD10 enmendadas: ya no hay obligatorios y opcionales) y una Receta **sin Ingredientes no tiene badge** (como antes las «solo opcionales»). El cálculo del porcentaje, el umbral 85 %, el badge «100% Match» y sus colores quedan intactos.

*Enmienda añadida al final por `redisenar-formulario-de-receta`, sin reescribir la historia ni las evidencias históricas de esta spec.*
