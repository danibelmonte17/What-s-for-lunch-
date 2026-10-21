# Riesgos y preguntas abiertas

Todo lo que falta por saber o que hemos asumido sin confirmar. Si otro documento depende de algo de aquí, lo marca con `⚠` y el ID. La numeración es estable: no se reutiliza aunque se responda o se descarte algo.

## Resumen

Preguntas abiertas: 0 · Supuestos: 7 · Riesgos: 6 · Bloquean la siguiente fase: 0 · Supuestos sin confirmar: 0 (las tres Q del Escáner, `Q-escanear-despensa-con-camara-1..3`, se resolvieron el 2026-10-21 con respuesta del usuario; ver el apéndice)

El banco de descubrimiento (37 preguntas, `P1`-`P37`) quedó **resuelto**: 36 respondidas por el usuario y 1 asumida (`P29` → `S01`); sus respuestas viven ya dobladas en los documentos de producto y siguen siendo citas válidas (`P12`, `P31`…). Las preguntas `Q-…` de las specs se registran aquí mientras están abiertas y pasan al apéndice al resolverse.

## Bloquean la siguiente fase

Ninguna. **RK02 aceptado por el usuario el 2026-10-21**: implementar EfficientDet-Lite2 con umbral mínimo 0.60, chips editables y modo manual RD05 como mitigaciones explícitas. La comparación de modelos está en `docs/technical-discovery.md`.

## Preguntas de tiempo de implementación

No hay preguntas pendientes para esta feature; la decisión de RK02 se registró como aceptada. Las preguntas `Q-…` que surjan al redactar o implementar una spec se añaden aquí con su origen y estado, y se mueven al apéndice al resolverse.

## Más adelante / No es MVP

- **«Sin precios» (`P12`, `S02`)** — el mockup de Detalle de Receta dibuja «coste casero estimado»; decidimos no mostrar precios en ninguna vista. El material de `uxdocs/` y el MVP quedan en desacuerdo explícito; se revisa solo si el usuario lo pide.
- **Entrada en «Mis Recetas» (`P18`)** — Mis Recetas arranca vacía; la alternativa (abrir en el Escáner) da una primera impresión más rica. El estado vacío empuja hacia el Escáner mientras se mantenga.
- **Sin ubicaciones físicas (`P37`)** — elimina el checklist por ubicaciones (nevera/despensa/vinagrera/especiero) del PRD; posible ampliación futura de la Despensa.
- **Redefinir criterios de éxito para uso personal (`S03`)** — los KPIs del PRD (WAU, sesiones) son poco medibles con 1-5 usuarios (`RK06`).
- **Telemetría y métricas (`S01`)** — si algún día se quiere, hay que planear recolección desde el inicio.
- **Modelo alternativo a MediaPipe (YOLOv8 ONNX, `P23`)** — solo si el spike de `RK02` falla.

## Supuestos

Decisiones tomadas sin confirmación explícita, incluidas las tomadas para desbloquear preguntas pendientes.

| ID | Asumimos que… | Por qué | Si es falso… | Cómo confirmarlo |
|---|---|---|---|---|
| S01 | La observabilidad del MVP es mínima (logs locales); métricas y alertas se añaden después | Responde a `P29` (observabilidad) sin necesidad de infraestructura en uso personal | Habría que planear recolección de métricas desde el inicio | Preguntar al usuario si quiere telemetría desde la primera versión |
| S02 | La app no muestra precios ni costes estimados en ninguna vista | El usuario respondió «sin precios» a `P12`; el mockup de Detalle que los dibuja es orientativo (`P16`) | Habría que recuperar el campo «coste casero estimado» del PRD y definir moneda y origen del precio | Confirmar que «sin precios» aplica a todas las vistas, no solo al Detalle |
| S03 | Los KPIs del PRD aplican como criterios de éxito aunque el MVP sea de uso personal (1-5 usuarios) | El usuario validó los criterios de éxito del PRD sin matizar (`P15`) | Los criterios de éxito deberían redefinirse para uso personal | Revisar `docs/build-brief.md` con el usuario |
| S04 | La voz (locución de pasos, comandos, dictado) usa las APIs nativas de Android (`SpeechRecognizer`/`TextToSpeech`) | Sustituye la Web Speech API del PRD al pasar de PWA a Android nativo (`P35`, `P20`) | Habría que integrar otro motor de voz (p. ej. Whisper on-device) | Probar precisión de reconocimiento en español con las APIs nativas |
| S05 | La API key de OpenRouter la introduce el usuario en Ajustes, se guarda cifrada en el dispositivo y la app llama directamente al proveedor (sin backend) | Deriva de «sin backend» (`P33`) + IA real en el MVP (`P4`) | Si se requiere ocultar la key, haría falta el proxy FastAPI previsto como fase posterior (`P34`) | Decidir si una key en el dispositivo es aceptable para el usuario |
| S06 | La estructura de carpetas, la arquitectura MVVM por capas y los comandos Gradle descritos en `CONTEXT.md` son los previstos | El repo estaba vacío: no había código que inspeccionar | Se ajusta la documentación cuando exista el proyecto real | Confirmado con el scaffold real (Q-arrancar-app-en-mis-recetas-1); la estructura y los comandos viven hoy en `docs/technical-discovery.md` |
| S07 | Para publicar una Receta hace falta título y foto, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila de Ingrediente ni Paso inválidos | Ninguna pregunta del banco cubrió las validaciones mínimas del formulario (`uxdocs/chefencasa_registrar_y_editar_receta/` no las define); revisado por Q-crear-receta-1 y Q-crear-receta-2 | Cambian las validaciones del formulario y los casos límite de `docs/domain-model.md` | Preguntar al usuario qué campos son obligatorios para publicar |

## Riesgos

| ID | Riesgo | Probabilidad | Impacto | Mitigación o siguiente paso |
|---|---|---|---|---|
| RK01 | Sin backend, la API key vive en el dispositivo: posible extracción y coste no controlado de las llamadas a OpenRouter | Media | Medio | Almacenamiento cifrado (Keystore), aviso en Ajustes, límites de gasto en el panel de OpenRouter (`S05`) |
| RK02 | MediaPipe no alcanza el objetivo inicial >88% con modelos genéricos COCO | Alta | Alto | **Riesgo aceptado por el usuario (2026-10-21):** EfficientDet-Lite2 con umbral de propuesta 0.60; los chips se editan antes de persistir y RD05 mantiene entrada manual ante permiso denegado, detecciones vacías o fallo de inferencia. Modelo/hash/comparativa en `docs/technical-discovery.md`. |
| RK03 | Envío de datos a OpenRouter pese al compromiso «sin retención» (RGPD) | Baja | Alto | Verificar y configurar la política de retención del proveedor; las fotos del Escaneo nunca salen del dispositivo (detección on-device) |
| RK04 | El material UX es orientativo y no cubre Ajustes ni estados vacío/carga/error: riesgo de divergencia visual | Media | Bajo | Seguir los tokens YAML de `uxdocs/DESIGN.md` (`P17`) y las pautas de `DESIGN.md` para las lagunas |
| RK05 | El pivote de PWA (PRD) a Android nativo deja sin equivalente claro funcionalidades del PRD (WebRTC, Web Speech, Service Workers) | Media | Medio | Equivalencias previstas en `docs/technical-discovery.md` (CameraX/MediaPipe, APIs nativas de voz, Room offline); validar en el spike de `RK02` |
| RK06 | Los KPIs del PRD (WAU, sesiones) son poco medibles con 1-5 usuarios | Alta | Bajo | Medición manual en la beta personal o redefinición de criterios (`S03`) |

## Tareas de investigación

Validaciones pendientes antes o durante la construcción de las features que dependen de ellas (detalle en `docs/technical-discovery.md`):

| Tarea | Para cuándo | Riesgo que cubre |
|---|---|---|
| Spike MediaPipe y reintento con otros modelos (2026-10-21): cuatro detectores en API 30; decisión del usuario: EfficientDet-Lite2, umbral 0.60 | **completado; riesgo aceptado con mitigaciones RD05** | `RK02`, `RK05` |
| Latencia y coste reales de OpenRouter para chat y sugerencias | antes de `configurar-acceso-a-ia` | `RK01` |
| Configuración efectiva de «sin retención» de datos en OpenRouter | antes de `sugerir-recetas-con-ia` | `RK03` |
| Precisión de `SpeechRecognizer`/`TextToSpeech` en español y flujo de permisos de cámara/micrófono | antes de `controlar-cocina-con-voz` | `S04` |
| Almacenamiento cifrado de la API key (Keystore vs EncryptedSharedPreferences) | al implementar `configurar-acceso-a-ia` | `RK01`, `S05` |

## Apéndice · Registro de preguntas resueltas

Historial de trazabilidad. Las specs citan estos IDs como origen de requisitos; no se borran ni se renumeran. Todas las preguntas están resueltas; las tres Q del Escáner (`Q-escanear-despensa-con-camara-1..3`) se respondieron el 2026-10-21 (ver su estado).

| ID | Pregunta | Origen | Bloqueante | Estado |
|---|---|---|---|---|
| Q-arrancar-app-en-mis-recetas-1 | ¿Confirmamos la estructura prevista en `CONTEXT.md` (módulo único `app` con capas `ui`/`domain`/`data`, MVVM + StateFlow, Gradle Kotlin DSL y comandos `./gradlew`)? | `docs/specs/arrancar-app-en-mis-recetas.md` (⚠ S06) | no | resuelta (confirmada la estructura y los comandos de `CONTEXT.md`, hoy documentados en `docs/technical-discovery.md`) |
| Q-arrancar-app-en-mis-recetas-2 | ¿Qué versión mínima de Android (minSdk) fijamos para la app? | `docs/specs/arrancar-app-en-mis-recetas.md` | no | resuelta (API 30 / Android 11) |
| Q-arrancar-app-en-mis-recetas-3 | Las acciones del estado vacío apuntan al formulario y al Escáner, que llegan en features posteriores: ¿qué hacen ahora al pulsarlas? | `docs/specs/arrancar-app-en-mis-recetas.md` | no | resuelta (pantallas provisionales con título y vuelta atrás) |
| Q-arrancar-app-en-mis-recetas-4 | ¿El scaffold incluye framework de inyección de dependencias y cuál? | `docs/specs/arrancar-app-en-mis-recetas.md` | no | resuelta (Koin desde el scaffold, en lugar de Hilt) |
| Q-arrancar-app-en-mis-recetas-5 | ¿Quién fija el copy del estado vacío (laguna RK04)? | `docs/specs/arrancar-app-en-mis-recetas.md` | no | resuelta (lo propone la spec y se valida en la revisión visual) |
| Q-crear-receta-1 | Para «Publicar Receta», además de ≥1 Ingrediente y ≥1 Paso (`⚠ S07`), ¿qué campos son obligatorios? | `docs/specs/crear-receta.md` | no | resuelta (título y foto obligatorios; revisa `⚠ S07` en su punto «la foto es opcional») |
| Q-crear-receta-2 | ¿Qué exige «Guardar Borrador»? | `docs/specs/crear-receta.md` | no | resuelta (solo título; el resto del formulario puede quedar incompleto) |
| Q-crear-receta-3 | ¿Desde dónde puede elegir el usuario la foto del formulario? | `docs/specs/crear-receta.md` | no | resuelta (galería, cámara o «Generar con IA») |
| Q-crear-receta-4 | ¿El formulario incluye el campo de calorías por porción que mostrará el Detalle? | `docs/specs/crear-receta.md` | no | resuelta (sí, como campo opcional) |
| Q-crear-receta-5 | ¿Se incluye el toggle de favorito del prototipo pese a no figurar en los pasos? | `docs/specs/crear-receta.md` | no | resuelta (sí, toggle simple) |
| Q-crear-receta-6 | «Generar con IA» necesita API key, que se registra en `configurar-acceso-a-ia`: ¿cómo se resuelve aquí? | `docs/specs/crear-receta.md` | sí | resuelta (condicionada a la API key del dispositivo: sin key, aviso y el formulario sigue usable; la pantalla de Ajustes llega en `configurar-acceso-a-ia`) |
| Q-crear-receta-7 | ¿Cómo se pide la imagen generada por IA? | `docs/specs/crear-receta.md` | no | resuelta (prompt construido automáticamente con la información del plato: título e historia) |
| Q-afinar-fidelidad-visual-del-formulario-1 | ¿Cómo se replica el asa de arrastre de las filas de Ingrediente del prototipo si el reordenado sigue fuera de alcance? | `docs/specs/afinar-fidelidad-visual-del-formulario.md` | no | resuelta (asa decorativa no interactiva; el reordenado sigue fuera de alcance) |
| Q-afinar-fidelidad-visual-del-formulario-2 | ¿Se implementa el botón «Guardar» de la cabecera del prototipo? | `docs/specs/afinar-fidelidad-visual-del-formulario.md` | no | resuelta (se implementó como guardado de Borrador, igual que «Guardar Borrador» del dock) y **revisada el 2026-10-19** por Q-editar-receta-5: se retira la acción de la cabecera en todos los modos |
| Q-editar-receta-1 | Al editar una Receta Publicada (sin vuelta a Borrador), ¿qué muestra el dock del formulario? | `docs/specs/editar-receta.md` | sí | resuelta (única acción primaria «Guardar cambios», que guarda manteniendo Publicada; RF-5) |
| Q-editar-receta-2 | ¿Está «Publicar Receta» disponible al editar un Borrador (transición Borrador → Publicada con `⚠ S07`)? | `docs/specs/editar-receta.md` | sí | resuelta (sí, con las mismas reglas que el alta; RF-8 y RF-10) |
| Q-editar-receta-3 | Si al editar una Publicada deja de cumplir `⚠ S07`, ¿qué pasa al guardar los cambios? | `docs/specs/editar-receta.md` | sí | resuelta (se bloquea el guardado: «Guardar cambios» deshabilitado, lista de requisitos junto al dock y nada se guarda; RF-7) |
| Q-editar-receta-4 | El pie de la tarjeta dibuja «Editar», «Eliminar» y «Cocinar Ahora»: ¿qué toca esta feature? | `docs/specs/editar-receta.md` | no | resuelta (solo «Editar receta»; «Eliminar» es `eliminar-receta` y «Cocinar Ahora» de otras features; RF-4) |
| Q-editar-receta-5 | El «Guardar» de la cabecera es redundante (mismo guardado y mismo habilitado que el dock): ¿se mantiene? | `docs/specs/editar-receta.md`, `docs/specs/afinar-fidelidad-visual-del-formulario.md` | no | resuelta (se elimina de la cabecera en todos los modos; el guardado queda solo en el dock; RF-13/RF-14 y RF-3/RF-21) |
| Q-editar-receta-6 | El icono «Editar receta» del pie de la tarjeta apenas se ve en el dispositivo: ¿se destaca? | `docs/specs/editar-receta.md` | no | resuelta (excepción de diseño aprobada por el usuario el 2026-10-19: botón primario compacto con lápiz y etiqueta «Editar» al estilo de «Crear receta», nombre accesible «Editar receta»; **tamaño final acordado el 2026-10-19: icono 16 dp y altura visual 36 dp con área táctil ≥ 48 dp**; RF-4) |
| Q-eliminar-receta-1 | ¿Qué copy exacto usa el diálogo de confirmación (título, mensaje con el nombre, botones)? | `docs/specs/eliminar-receta.md` | no | resuelta (confirmado por el usuario el 2026-10-19: título «Eliminar receta», mensaje «¿Eliminar «{nombre}»? Esta acción es definitiva y no se puede deshacer.», botones «Cancelar» y «Eliminar» con tinte error) |
| Q-eliminar-receta-2 | ¿Descartar el diálogo con atrás o toque fuera cuenta como cancelación? | `docs/specs/eliminar-receta.md` | no | resuelta (confirmado por el usuario el 2026-10-19: sí, el descarte cierra sin borrar, igual que «Cancelar») |
| Q-eliminar-receta-3 | ¿Se borra también el fichero de la foto asociada a la Receta? | `docs/specs/eliminar-receta.md` | no | resuelta (confirmado por el usuario el 2026-10-19: sí, al confirmar se borra el fichero de la foto) |
| Q-eliminar-receta-4 | ¿Qué estilo tiene la acción «Eliminar» del pie de la tarjeta tras la excepción de Q-editar-receta-6? | `docs/specs/eliminar-receta.md` | no | resuelta (confirmado por el usuario el 2026-10-19; valor alineado el 2026-10-19 con el tamaño compacto final de «Editar»: `Button` de relleno `#A83212` con texto blanco, icono `Icons.Outlined.Delete` de **16 dp** + etiqueta «Eliminar», **altura visual 36 dp** y ancho al contenido con **área táctil ≥ 48 dp**, nombre accesible «Eliminar receta», `testTag(RECIPE_DELETE)`, a la derecha de «Editar»; misma estructura, color y tamaño que «Editar» por la excepción de `Q-editar-receta-6`, en lugar del icono discreto tonal error de 36 dp) |
| Q-sincronizar-spec-de-crear-receta-1 | Al actualizar RF-15/RF-16 de `crear-receta.md`: ¿RF-15 queda el botón deshabilitado y RF-16 la lista junto al dock, o al revés? | `docs/specs/sincronizar-spec-de-crear-receta.md` | no | resuelta (RF-15 deshabilitado y RF-16 lista; confirmado por el usuario el 2026-10-19, manteniendo el orden de la `description`, `editar-receta` RF-7/RF-8 y `afinar-fidelidad-visual-del-formulario` RF-14/RF-15) |
| Q-sincronizar-spec-de-crear-receta-2 | ¿Se añade una línea en «Registro de iteraciones» de `crear-receta.md` que registre esta sincronización? | `docs/specs/sincronizar-spec-de-crear-receta.md` | no | resuelta (no se añade nada en «Registro de iteraciones» de `crear-receta.md`; alcance = las cinco secciones de `steps` + T6, y el rastro vive en la spec de esta feature y en el Historial de `docs/progress-history.md`) |
| Q-buscar-y-filtrar-recetas-1 | ¿El filtro por momento culinario es de selección única con «Todas» por defecto (como el prototipo) o de selección múltiple? | `docs/specs/buscar-y-filtrar-recetas.md` | no | resuelta (confirmada por el humano el 2026-10-19: selección única con opción «Todas») |
| Q-buscar-y-filtrar-recetas-2 | ¿La comparación del buscador ignora tildes y espacios exteriores además de mayúsculas? | `docs/specs/buscar-y-filtrar-recetas.md` | no | resuelta (confirmada por el humano el 2026-10-19: comparación normalizada completa, insensible a mayúsculas y tildes y con espacios recortados, en título e ingredientes) |
| Q-buscar-y-filtrar-recetas-3 | ¿Qué retardo de debounce usa el buscador? | `docs/specs/buscar-y-filtrar-recetas.md` | no | resuelta (confirmada por el humano el 2026-10-19: debounce de 300 ms) |
| Q-buscar-y-filtrar-recetas-4 | ¿Se muestran el buscador y el filtro cuando la colección de Recetas está vacía? | `docs/specs/buscar-y-filtrar-recetas.md` | no | resuelta (confirmada por el humano el 2026-10-19: se ocultan; solo queda el estado vacío de Mis Recetas) |
| Q-buscar-y-filtrar-recetas-5 | ¿La búsqueda y el filtro se conservan al navegar y volver a Mis Recetas y tras cerrar la app? | `docs/specs/buscar-y-filtrar-recetas.md` | no | resuelta (confirmada por el humano el 2026-10-19: estado en memoria, se conserva al navegar dentro de la sesión y se restablece al cerrar la app) |
| Q-gestionar-alimentos-de-la-despensa-1 | ¿Dónde vive el listado de la Despensa y desde dónde se accede? (los documentos definen 8 pantallas, ninguna «Despensa») | `docs/specs/gestionar-alimentos-de-la-despensa.md` | no | resuelta (confirmado por el usuario el 2026-10-20: pantalla «Despensa» nueva con ruta propia, accesible desde una acción en la barra superior de Mis Recetas, visible en lista y estado vacío; el Escáner queda intacto para `escanear-despensa-con-camara`) |
| Q-gestionar-alimentos-de-la-despensa-2 | ¿El borrado de un Alimento pide confirmación? (RD04 exige confirmación solo para Recetas; el prototipo del Escáner borra chips de un toque) | `docs/specs/gestionar-alimentos-de-la-despensa.md` | no | resuelta (confirmado por el usuario el 2026-10-20: diálogo de confirmación con el nombre del Alimento, mismo patrón que `eliminar-receta`, con «Cancelar»/«Eliminar» y descarte = cancelar) |
| Q-gestionar-alimentos-de-la-despensa-3 | ¿Qué pasa al añadir o renombrar un Alimento cuyo nombre normalizado coincide con otro existente? (el dominio fija «se fusionan» para el Escaneo) | `docs/specs/gestionar-alimentos-de-la-despensa.md` | no | resuelta (confirmado por el usuario el 2026-10-20: aviso «Este alimento ya está en la Despensa» y no se crea fila nueva; se conserva un solo Alimento por nombre normalizado) |
| Q-gestionar-alimentos-de-la-despensa-4 | ¿Qué representa la «cantidad (opcional)» del Alimento en el formulario? (sin unidad en `docs/domain-model.md`) | `docs/specs/gestionar-alimentos-de-la-despensa.md` | no | resuelta (revisado el 2026-10-20: se sustituyó el decimal por texto por un stepper entero, sin unidad; 0 = sin cantidad. Revisado el 2026-10-21: el rango pasa a 0..99 con máximo 99, ver Q-escanear-despensa-con-camara-4) |
| Q-escanear-despensa-con-camara-1 | ¿Qué pasa cuando un Alimento escaneado coincide (por nombre normalizado) con uno ya presente en la Despensa? | `docs/specs/escanear-despensa-con-camara.md` (D2) | no | resuelta (2026-10-21, respuesta del usuario: **«Preguntar en pantalla»** — el chip se marca «Ya está en la Despensa» y la coincidencia se resuelve con un diálogo por coincidencia: «Mantener cantidad» (el existente queda intacto, el chip no crea fila) o «Actualizar cantidad» (el existente adopta la del chip, incluido 0 = sin cantidad); varias coincidencias se resuelven una a una; se conserva «un Alimento por nombre normalizado») |
| Q-escanear-despensa-con-camara-2 | Al salir del Escáner con chips sin guardar (atrás o cierre de pantalla), ¿se descarta el Escaneo sin confirmación o se pide confirmación? | `docs/specs/escanear-despensa-con-camara.md` (D3) | no | resuelta (2026-10-21, respuesta del usuario: **«Pedir confirmación»** — al salir con chips sin guardar se muestra un diálogo con el patrón de `eliminar-receta`; «Cancelar» conserva los chips) |
| Q-escanear-despensa-con-camara-3 | ¿Los chips del Escáner editan la cantidad de los Alimentos detectados? | `docs/specs/escanear-despensa-con-camara.md` (D5) | no | resuelta (2026-10-21, respuesta del usuario: **«Con stepper en los chips»** — stepper entero 0..99 («−» deshabilitado en 0, «+» en 99, sin campo de texto; 0 = sin cantidad), igual que la pantalla Despensa, y se guarda la cantidad elegida en el chip) |
| Q-escanear-despensa-con-camara-4 | ¿Cuál es la cantidad máxima de un Alimento? (el dominio y `PantryItemValidation` fijaban 999) | `docs/specs/escanear-despensa-con-camara.md` (D6) | no | resuelta (2026-10-21, respuesta del usuario: **máximo 99** en toda la app — `MAX_QUANTITY` pasa de 999 a 99, rango entero 0..99 con 0 = sin cantidad; afecta a Despensa, Escáner, repositorios y tests, sin migración Room) |
| Q-ver-detalle-de-receta-1 | Los utensilios del step 1 («hero … con los utensilios») no existen en el modelo (`Recipe.kt`, Room, `docs/domain-model.md`) ni en el formulario aceptado: ¿se amplía el modelo (feature aparte con migración) o se omiten? | `docs/specs/ver-detalle-de-receta.md` (U1) | no | resuelta (decidida por el usuario el 2026-10-20: **omisión en el MVP**; la sección «Utensilios necesarios» no se implementa en el Detalle; si se quiere, una feature futura ampliará modelo + formulario con su propia spec) |
