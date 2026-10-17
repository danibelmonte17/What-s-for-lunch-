---
feature: crear-receta
category: funcional
depends_on: [arrancar-app-en-mis-recetas]
status: draft
source: feature_list.json
---
# Spec: Crear una receta propia desde el formulario de alta
# PARTE A · CONTRATO
## Contexto y objetivo
Mis Recetas nace vacía: hoy la app no tiene forma de dar de alta los platos del usuario. Esta feature añade el formulario de alta completo (metadatos, ingredientes, pasos y foto) y el guardado dual Borrador/Publicada según `⚠ S07`, de modo que las recetas creadas a mano empiecen a poblar Mis Recetas. Es la base de la que dependerán edición, borrado, detalle y Modo Cocina.
## Actores y permisos
- **Usuario** (único rol del MVP, propietario del dispositivo): crea Recetas propias, elige o genera su foto, la marca como favorita y guarda como Borrador o Publicada. Sin cuentas ni roles adicionales (`P31`).
- «Publicar» solo cambia el estado local: no hay publicación en red ni catálogo compartido (`P32`).
## Historia
Cuando tengo platos propios que quiero conservar y cocinar,
quiero darlos de alta con sus ingredientes, sus pasos y una foto,
para poder verlos en Mis Recetas y más adelante cocinarlos paso a paso.
## Requisitos funcionales
- RF-1: CUANDO el usuario abre el formulario de alta desde Mis Recetas, EL SISTEMA muestra los campos de título, historia, momento culinario, tiempo de preparación, tiempo de cocción, dificultad, raciones base, calorías por porción (opcional), foto (opcional) y favorito.
- RF-2: EL SISTEMA ofrece como momento culinario exactamente Desayuno, Almuerzo, Cena y Postre.
- RF-3: EL SISTEMA limita las raciones base a enteros entre 1 y 12 (RD03).
- RF-4: EL SISTEMA acepta tiempo de preparación y de cocción en minutos enteros ≥ 0.
- RF-5: CUANDO el usuario pulsa «Añadir Ingrediente», EL SISTEMA añade una fila editable con nombre, cantidad, unidad y marca de opcional.
- RF-6: EL SISTEMA permite modificar cualquier fila de Ingrediente existente.
- RF-7: EL SISTEMA permite eliminar cualquier fila de Ingrediente.
- RF-8: SI una fila de Ingrediente tiene el nombre vacío o una cantidad menor o igual que 0, ENTONCES EL SISTEMA la marca como inválida y no guarda con ninguna de las dos acciones.
- RF-9: CUANDO el usuario pulsa «Añadir Nuevo Paso», EL SISTEMA añade un Paso con texto y temporizador opcional en minutos.
- RF-10: EL SISTEMA permite modificar cualquier Paso existente.
- RF-11: EL SISTEMA permite eliminar cualquier Paso.
- RF-12: SI un Paso no tiene texto, ENTONCES EL SISTEMA lo marca como inválido y no guarda con ninguna de las dos acciones.
- RF-13: CUANDO el usuario pulsa «Guardar Borrador» con título, EL SISTEMA guarda la Receta con estado Borrador, visible en Mis Recetas con ese distintivo.
- RF-14: SI el usuario pulsa «Guardar Borrador» sin título, ENTONCES EL SISTEMA no guarda y señala el título como obligatorio.
- RF-15: SI el usuario pulsa «Publicar Receta» sin al menos un Ingrediente o sin al menos un Paso, ENTONCES EL SISTEMA no publica e indica el requisito incumplido (`⚠ S07`).
- RF-16: SI el usuario pulsa «Publicar Receta» sin título o sin foto, ENTONCES EL SISTEMA no publica e indica el requisito incumplido (Q-crear-receta-1, que revisa `⚠ S07`).
- RF-17: CUANDO el usuario pulsa «Publicar Receta» con título, foto, al menos un Ingrediente válido y un Paso válido, EL SISTEMA guarda la Receta con estado Publicada, visible en Mis Recetas.
- RF-18: EL SISTEMA conserva las Recetas guardadas en el dispositivo tras reiniciar la app.
- RF-19: CUANDO el usuario elige la foto, EL SISTEMA permite cargar una imagen de la galería o capturar una nueva con la cámara.
- RF-20: DONDE el usuario pulsa «Generar con IA», EL SISTEMA ofrece la imagen generada con un prompt construido con la información del plato (título e historia) para aceptarla o descartarla.
- RF-21: SI no hay API key de IA configurada, ENTONCES EL SISTEMA muestra un aviso sin bloquear el resto del formulario (RD05).
- RF-22: SI la generación de la imagen falla, ENTONCES EL SISTEMA muestra un aviso sin bloquear la elección manual de foto.
- RF-23: SI el permiso de cámara es denegado, ENTONCES EL SISTEMA muestra un aviso sin bloquear los demás orígenes de foto.
- RF-24: CUANDO el usuario pulsa el control de favorito, EL SISTEMA alterna el estado de favorito de la Receta.
- RF-25: CUANDO existen Recetas guardadas, EL SISTEMA muestra en Mis Recetas una tarjeta por Receta con su título, su momento culinario y su estado (Borrador/Publicada).
## Requisitos no funcionales
- NFR-1: Interfaz en español e inglés vía `strings.xml` (`P19`); los textos del formulario y de las tarjetas se afirman en ambas lenguas.
- NFR-2: Objetivos táctiles ≥ 48×48 px y contraste WCAG 2.1 AA (`DESIGN.md`).
- NFR-3: Android 11 (API 30) o superior, solo móvil (`P20`); el alta funciona sin red salvo «Generar con IA» y todo reside en el dispositivo (`P33`).
## Casos límite
- Guardado incompleto: sin título → RF-14; publicar sin ingredientes o sin pasos → RF-15; publicar sin título o sin foto → RF-16.
- Filas incompletas: Ingrediente sin nombre o con cantidad ≤ 0 → RF-8; Paso sin texto → RF-12.
- Límites: raciones fuera de 1-12 → RF-3; tiempos que no sean minutos ≥ 0 → RF-4.
- Foto: permiso de cámara denegado → RF-23; sin API key → RF-21; fallo de generación → RF-22.
- Dos Recetas con el mismo título se permiten (el dominio no define unicidad); un Borrador con historia, calorías o foto sin rellenar es válido → RF-13.
## Fuera de alcance
- Editar y eliminar Recetas (`editar-receta`, `eliminar-receta`).
- Buscador, filtro por momento culinario y badge de Coincidencia en Mis Recetas (`buscar-y-filtrar-recetas`, `mostrar-badge-de-coincidencia`).
- Detalle de Receta, escalado de Raciones y Modo Cocina.
- Etiquetas y dieta del prototipo («Sin Gluten», «Vegetariana»), la entrada de ingredientes o pasos con IA («Escanear con IA», «Reconocimiento Activo» del prototipo) y el reordenar filas (drag & drop): el orden de los Pasos es el de inserción.
- Alta de la API key y pantalla de Ajustes (`configurar-acceso-a-ia`): el acceso a la key queda condicionado (Q-crear-receta-6).
- Subida de fotos a la nube y sincronización entre dispositivos.
## Criterios de finalización
- Los 25 RF pasan su verificación (E2E en emulador API 30+, unit y comprobaciones puntuales) y `lint` + `unit` están en verde.
- Evidencia capturada dentro del presupuesto (≤ 4 artefactos, solo de esta feature).
- `docs/domain-model.md` y el supuesto `⚠ S07` reflejan la resolución de Q-crear-receta-1 (foto obligatoria para Publicar).
## Dudas abiertas
- Ninguna. Las dudas detectadas se resolvieron con el usuario antes de redactar: Q-crear-receta-1 a Q-crear-receta-7 en `docs/risks-and-open-questions.md` (todas `resuelta`). Quedan fijadas en sus RF las decisiones asumidas sin consulta: unidad de Ingrediente en texto libre, dificultad Fácil/Media/Difícil por defecto Media, tiempos en minutos, los cuatro momentos culinarios del dominio y raciones base por defecto 4.
## Trazabilidad
| RF | Origen | Verificación | Evidencia |
|---|---|---|---|
| RF-1 | `steps`: formulario con título, historia, momento, tiempos… | verify 1 (formulario completo) | informe E2E |
| RF-2 | `steps`: momento culinario | escenario E2E: las cuatro opciones | informe E2E |
| RF-3 | `steps`: raciones base | unit (límite 1-12) | informe `unit` |
| RF-4 | `steps`: tiempos | escenario unit (minutos ≥ 0) | informe `unit` |
| RF-5 | `steps`: gestor de ingredientes | verify 1 (dos ingredientes) | informe E2E |
| RF-6, RF-7 | `steps`: gestor de ingredientes | escenario E2E: editar y eliminar fila | informe E2E |
| RF-8 | derivado: caso límite | unit (validación de fila) | informe `unit` |
| RF-9 | `steps`: pasos con temporizador | verify 1 (dos pasos) | informe E2E |
| RF-10, RF-11 | `steps`: definición de pasos | escenario E2E: editar y eliminar paso | informe E2E |
| RF-12 | derivado: caso límite | unit (paso sin texto) | informe `unit` |
| RF-13 | `steps`: guardar como Borrador | verify 2 | informe E2E + captura |
| RF-14 | derivado: caso límite | escenario E2E: error de título | informe E2E |
| RF-15 | `steps`: regla S07 | verify 3 (unit) + E2E de bloqueo | informe `unit` + E2E |
| RF-16 | `steps` S07 + Q-crear-receta-1 | escenario E2E: publicar sin foto | informe E2E |
| RF-17 | `description`; `steps`: guardar como Publicada | verify 1 | informe E2E + captura |
| RF-18 | `steps`: guardar la receta | escenario E2E: recrear actividad | informe E2E |
| RF-19 | `steps`: foto + Q-crear-receta-3 | escenario manual con `adb`/uiautomator | informe de validación |
| RF-20 | Q-crear-receta-3, Q-crear-receta-7 | escenario E2E con generador simulado | informe E2E |
| RF-21 | Q-crear-receta-6 (RD05) | escenario E2E sin API key | informe E2E |
| RF-22 | derivado: caso límite (RD05) | escenario E2E con generador en fallo | informe E2E |
| RF-23 | derivado: caso límite | escenario manual: permiso denegado | informe de validación |
| RF-24 | Q-crear-receta-5 | escenario E2E: marcar favorito | informe E2E |
| RF-25 | verify 1 y 2 (aparece en Mis Recetas) | verify 1 y 2 | informe E2E + captura |
# PARTE B · PLAN
## Investigación del repositorio
### Archivos inspeccionados
- `domain/model/Recipe.kt`, `domain/repository/RecipeRepository.kt` — modelo mínimo (`id`, `title`) y contrato con solo `observeRecipes`.
- `data/repository/InMemoryRecipeRepository.kt` — sin persistencia; anuncia que la real llega aquí.
- `ui/misrecetas/MisRecetasScreen.kt` — la rama `WithRecipes` está sin implementar.
- `ui/nav/AppNavHost.kt`, `ui/nav/ProvisionalScreen.kt`, `di/AppModule.kt` — ruta `CREAR_RECETA` provisional y Koin (Q-arrancar-app-en-mis-recetas-4).
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — sin Room, KSP ni cliente HTTP.
- `androidTest/.../AppLaunchE2ETest.kt`, `MisRecetasLocalizationE2ETest.kt` — patrón E2E con testTags.
- `uxdocs/chefencasa_registrar_y_editar_receta/code.html`, `uxdocs/chefencasa_mis_recetas_y_crud/code.html`, `docs/domain-model.md`, `docs/technical-discovery.md` — formulario, tarjetas, reglas y stack.
### Patrones existentes a seguir
- MVVM + StateFlow: `Route` (`koinViewModel`) → `ViewModel` → reglas en `domain/`, sin dependencias de UI.
- testTags estables en un object `*TestTags` para E2E independiente del idioma.
- Textos en `values/strings.xml` (es) y `values-en/strings.xml`; Koin en `di/AppModule.kts`; rutas en `ui/nav/AppNavHost.kt`.
- Scripts `init`/`lint`/`unit`/`e2e` (.sh y .ps1) y `lint.xml` en la raíz.
### Carencias actuales
- Sin Room ni plugin KSP en el build; sin cliente HTTP (OkHttp).
- Sin permiso `CAMERA` en el manifest.
- Sin almacenamiento de API key (lo trae `configurar-acceso-a-ia`): «Generar con IA» queda detrás de un puerto (Q-crear-receta-6).
- El selector del sistema no se automatiza desde las pruebas Compose: hace falta un puerto de foto sustituible.
## Enfoque técnico
- **Dominio** (puro, sin Android): ampliar `Recipe` con historia, `MealMoment` (Breakfast/Lunch/Dinner/Dessert), minutos de preparación y cocción, `Difficulty` (Easy/Medium/Hard), raciones base, calorías por porción nullable, ruta de foto nullable, favorito, `RecipeStatus` (Draft/Published), origen (User) y las listas `RecipeIngredient` (nombre, cantidad, unidad en texto libre, opcional) y `RecipeStep` (orden por inserción, texto, temporizador opcional en minutos).
- **Validaciones** en `domain/crearreceta/RecipeValidation.kt`, funciones puras que devuelven los requisitos incumplidos: `validateForDraft` (título) y `validateForPublish` (título, foto, ≥ 1 ingrediente válido, ≥ 1 paso válido); los límites (raciones 1-12, cantidades > 0, tiempos ≥ 0) también en dominio.
- **Datos**: Room (candidato de `docs/technical-discovery.md`) con KSP: `RecipeEntity`, `IngredientEntity` y `StepEntity` (clave foránea con borrado en cascada) y `RecipeDao`; `RoomRecipeRepository` implementa `RecipeRepository`, ampliado con `saveRecipe`. La foto se copia a `filesDir/recipes/<recipeId>.jpg` y la Receta guarda su ruta (RD06 aplica solo a fotos de Escaneo). Sustituye a `InMemoryRecipeRepository` en Koin.
- **UI**: `ui/crearreceta/` con `CrearRecetaRoute` (lanzadores de ActivityResult), `CrearRecetaViewModel` (UiState con campos, filas y errores en línea) y `CrearRecetaScreen` (secciones foto, metadatos, ingredientes, pasos y acciones). La ruta `CREAR_RECETA` sustituye `ProvisionalScreen`; tras guardar, `popBackStack` a Mis Recetas.
- **Foto**: puerto `PhotoAcquirer` con `pickFromGallery()`, `takePhoto()` y `generate(prompt)`; implementación con `PickVisualMedia`, `TakePicture` (permiso `CAMERA` en manifest + solicitud en tiempo de ejecución) y generador IA. En `androidTest` se sustituye por un fake que inyecta una imagen de prueba.
- **Generación IA** (provisional: depende de Q-crear-receta-6): `ImageGenerator` sobre la API compatible con OpenAI (OkHttp) con `AiKeyProvider` sobre el almacenamiento cifrado previsto por `⚠ S05` que `configurar-acceso-a-ia` reutilizará; el prompt se construye con título e historia (Q-crear-receta-7). Sin key → aviso (RF-21); con fallo → aviso y alternativa manual (RD05).
- **Mis Recetas**: la rama `WithRecipes` muestra un `LazyColumn` de tarjetas (título, miniatura si existe, momento culinario y distintivo Borrador/Publicada), sin buscador, filtro ni badge de Coincidencia.
- **Tests**: unit (JVM) sobre validaciones de dominio; E2E Compose con testTags y fakes de foto e IA; la durabilidad se comprueba recreando la actividad.
## Archivos esperados
- `domain/model/Recipe.kt` — ampliar; `RecipeIngredient.kt`, `RecipeStep.kt`, `MealMoment.kt`, `Difficulty.kt`, `RecipeStatus.kt` — crear.
- `domain/crearreceta/RecipeValidation.kt` y `domain/repository/RecipeRepository.kt` — crear y ampliar (`saveRecipe`).
- `data/local/RecipeEntity.kt`, `IngredientEntity.kt`, `StepEntity.kt`, `RecipeDao.kt`, `RecipeDatabase.kt` y `data/repository/RoomRecipeRepository.kt` — crear.
- `data/photo/PhotoStore.kt`, `SystemPhotoAcquirer.kt` y `data/ai/ImageGenerator.kt`, `OpenAiCompatibleImageGenerator.kt`, `AiKeyProvider.kt` — crear.
- `ui/crearreceta/CrearRecetaRoute.kt`, `CrearRecetaScreen.kt`, `CrearRecetaViewModel.kt` — crear; `ui/misrecetas/MisRecetasScreen.kt`, `ui/nav/AppNavHost.kt`, `di/AppModule.kt` — ampliar.
- `AndroidManifest.xml` (permiso `CAMERA`), `gradle/libs.versions.toml` y `app/build.gradle.kts` (Room, KSP, OkHttp) — ampliar.
- `res/values/strings.xml`, `res/values-en/strings.xml` — ampliar.
- `test/.../domain/RecipeValidationTest.kt` y `androidTest/.../CrearRecetaE2ETest.kt` con fakes de foto/IA — crear.
## Impacto visual
- Fuente de diseño: `DESIGN.md` + prototipo `uxdocs/chefencasa_registrar_y_editar_receta/code.html` (orientativo) y `uxdocs/chefencasa_mis_recetas_y_crud/code.html` para las tarjetas.
- Pantallas o estados afectados: Formulario de Receta (nuevo, sustituye el destino provisional) y Mis Recetas (estado con Recetas).
- Fidelidad visual (NFR): misma jerarquía del prototipo (foto arriba, metadatos, ingredientes, pasos, «Guardar Borrador»/«Publicar Receta») con los tokens Warm Culinary Craft de `uxdocs/DESIGN.md` (botones ≥ 52 px, targets ≥ 48 px); las 2 capturas de emulador API 30+ se comparan con el prototipo y `DESIGN.md`.
- ¿Nuevo artefacto de diseño necesario?: no — el prototipo cubre el formulario y las tarjetas se apoyan en el de Mis Recetas.
## Impacto en documentación duradera
- `ARCHITECTURE.md` / `CONSTRAINTS.md`: no necesario — no existen; la arquitectura vive en `CONTEXT.md` y `docs/technical-discovery.md`.
- `AGENTS.md`: no necesario — no cambian estructura, comandos ni convenciones.
- Otros: `docs/domain-model.md` — actualizar la validación de Publicar (Q-crear-receta-1: foto obligatoria, revisando `⚠ S07`); `docs/risks-and-open-questions.md` — reflejar esa revisión de S07.
## Plan y tareas
- [ ] T1: modelo de dominio ampliado y validaciones puras. (cubre RF-1 a RF-4, RF-8, RF-12, RF-15, RF-16)
- [ ] T2: Room, `RoomRecipeRepository` y Koin. (cubre RF-18)
- [ ] T3: pantalla del formulario, ViewModel y navegación. (cubre RF-1)
- [ ] T4: gestor dinámico de ingredientes. (cubre RF-5, RF-6, RF-7, RF-8)
- [ ] T5: definición de pasos con temporizador. (cubre RF-9, RF-10, RF-11, RF-12)
- [ ] T6: foto por galería, cámara y «Generar con IA» con sus avisos. (cubre RF-19 a RF-23)
- [ ] T7: favorito y guardado Borrador/Publicar. (cubre RF-13 a RF-17, RF-24)
- [ ] T8: tarjetas de Receta en Mis Recetas. (cubre RF-25)
- [ ] T9: textos es/en de todo lo nuevo. (cubre NFR-1)
- [ ] T10: tests unit y E2E con fakes de foto e IA. (cubre el plan de verificación)
- [ ] T11: actualizar `docs/domain-model.md` y el supuesto `⚠ S07`. (cubre criterio de finalización)
## Plan de verificación
- `./lint.sh` → exit 0 sin errores; `./unit.sh` → exit 0.
- Unit (verify 3): «no se publica una receta sin al menos un ingrediente y un paso», más las validaciones de RF-3, RF-4, RF-8 y RF-12.
- `./e2e.sh` en emulador API 30+: crear receta con dos ingredientes y dos pasos, foto (fake) y publicar → aparece en Mis Recetas (verify 1); guardar el mismo formulario como Borrador → tarjeta con distintivo Borrador (verify 2).
- E2E propios: publicar sin ingrediente/paso o sin foto → error visible y sin guardar (RF-15, RF-16); «Generar con IA» sin API key → aviso y con generador en fallo → aviso (RF-21, RF-22); marcar favorito y recrear la actividad → la Receta sigue en Mis Recetas (RF-18, RF-24).
- UI (solo el validador): 2 capturas en emulador API 30+ (formulario completo y Mis Recetas con tarjetas) comparadas con `uxdocs/chefencasa_registrar_y_editar_receta/code.html` y `DESIGN.md`, guardadas según «Evidencia a capturar».
- Scripts de verificación: no se tocan `init`/`lint`/`unit`/`e2e`.
## Evidencia a capturar
- `docs/evidence/crear-receta/informe-validacion.md` con la salida de `lint`, `unit` y `e2e`, la verificación por RF y la comparación visual.
- UI: `docs/evidence/crear-receta/formulario-receta.png` y `docs/evidence/crear-receta/mis-recetas-con-recetas.png` (emulador API 30+, es).
## Checklist del validador
- [ ] La implementación se queda dentro del alcance de esta spec.
- [ ] Cada RF de la tabla de trazabilidad pasa su verificación.
- [ ] La evidencia está presente, dentro del presupuesto (≤ 4) y solo de esta feature.
- [ ] UI: 2 capturas de estados clave y fidelidad al prototipo y a `DESIGN.md`.
- [ ] Cobertura E2E añadida o actualizada para los flujos observables (hay harness E2E).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] No se añadió comportamiento ni trabajo de otras features.
