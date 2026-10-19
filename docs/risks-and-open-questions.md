# Riesgos y preguntas abiertas

Todo lo que falta por saber o que hemos asumido sin confirmar. Si otro documento depende de algo de aquí, lo marca con `⚠` y el ID.

## Resumen

Preguntas abiertas: 0 · Supuestos: 7 · Riesgos: 6 · Bloquean la construcción: 0

El banco de descubrimiento (37 preguntas, `P1`-`P37`) quedó **resuelto** en la Fase 2: 36 respondidas por el usuario y 1 asumida (`P29` → `S01`). La numeración es estable: no se reutiliza aunque se responda o se descarte algo.

## Preguntas abiertas

Ninguna.

| ID | Pregunta | Origen | Bloqueante | Estado |
|---|---|---|---|---|
| Q-arrancar-app-en-mis-recetas-1 | ¿Confirmamos la estructura prevista en `CONTEXT.md` (módulo único `app` con capas `ui`/`domain`/`data`, MVVM + StateFlow, Gradle Kotlin DSL y comandos `./gradlew`)? | `docs/specs/arrancar-app-en-mis-recetas.md` (⚠ S06) | no | resuelta (confirmada la estructura y los comandos de `CONTEXT.md`) |
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

## Supuestos

Decisiones tomadas sin confirmación explícita, incluidas las tomadas para desbloquear preguntas pendientes.

| ID | Asumimos que… | Por qué | Si es falso… | Cómo confirmarlo |
|---|---|---|---|---|
| S01 | La observabilidad del MVP es mínima (logs locales); métricas y alertas se añaden después | Responde a `P29` (observabilidad) sin necesidad de infraestructura en uso personal | Habría que planear recolección de métricas desde el inicio | Preguntar al usuario si quiere telemetría desde la primera versión |
| S02 | La app no muestra precios ni costes estimados en ninguna vista | El usuario respondió «sin precios» a `P12`; el mockup de Detalle que los dibuja es orientativo (`P16`) | Habría que recuperar el campo «coste casero estimado» del PRD y definir moneda y origen del precio | Confirmar que «sin precios» aplica a todas las vistas, no solo al Detalle |
| S03 | Los KPIs del PRD aplican como criterios de éxito aunque el MVP sea de uso personal (1-5 usuarios) | El usuario validó los criterios de éxito del PRD sin matizar (`P15`) | Los criterios de éxito deberían redefinirse para uso personal | Revisar `docs/build-brief.md` con el usuario |
| S04 | La voz (locución de pasos, comandos, dictado) usa las APIs nativas de Android (`SpeechRecognizer`/`TextToSpeech`) | Sustituye la Web Speech API del PRD al pasar de PWA a Android nativo (`P35`, `P20`) | Habría que integrar otro motor de voz (p. ej. Whisper on-device) | Probar precisión de reconocimiento en español con las APIs nativas |
| S05 | La API key de OpenRouter la introduce el usuario en Ajustes, se guarda cifrada en el dispositivo y la app llama directamente al proveedor (sin backend) | Deriva de «sin backend» (`P33`) + IA real en el MVP (`P4`) | Si se requiere ocultar la key, haría falta el proxy FastAPI previsto como fase posterior (`P34`) | Decidir si una key en el dispositivo es aceptable para el usuario |
| S06 | La estructura de carpetas, la arquitectura MVVM por capas y los comandos Gradle descritos en `CONTEXT.md` son los previstos | El repo está vacío: no hay código que inspeccionar | Se ajusta `CONTEXT.md` cuando exista el proyecto real | Al crear el proyecto Android (primer `build.gradle`) |
| S07 | Para publicar una Receta hace falta título y foto, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila de Ingrediente ni Paso inválidos | Ninguna pregunta del banco cubrió las validaciones mínimas del formulario (`uxdocs/chefencasa_registrar_y_editar_receta/` no las define); revisado por Q-crear-receta-1 y Q-crear-receta-2 | Cambian las validaciones del formulario y los casos límite de `docs/domain-model.md` | Preguntar al usuario qué campos son obligatorios para publicar |

## Riesgos

| ID | Riesgo | Probabilidad | Impacto | Mitigación o siguiente paso |
|---|---|---|---|---|
| RK01 | Sin backend, la API key vive en el dispositivo: posible extracción y coste no controlado de las llamadas a OpenRouter | Media | Medio | Almacenamiento cifrado (Keystore), aviso en Ajustes, límites de gasto en el panel de OpenRouter (`S05`) |
| RK02 | MediaPipe puede no detectar alimentos con la confianza objetivo (> 88%) sin un modelo específico entrenado | Alta | Alto | Spike de validación antes de construir el Escáner; el Modo de degradación manual (RD05) ya cubre el fallo |
| RK03 | Envío de datos a OpenRouter pese al compromiso «sin retención» (RGPD) | Baja | Alto | Verificar y configurar la política de retención del proveedor; las fotos del Escaneo nunca salen del dispositivo (detección on-device) |
| RK04 | El material UX es orientativo y no cubre Ajustes ni estados vacío/carga/error: riesgo de divergencia visual | Media | Bajo | Seguir los tokens YAML de `uxdocs/DESIGN.md` (`P17`) y las pautas de `DESIGN.md` para las lagunas |
| RK05 | El pivote de PWA (PRD) a Android nativo deja sin equivalente claro funcionalidades del PRD (WebRTC, Web Speech, Service Workers) | Media | Medio | Equivalencias previstas en `docs/technical-discovery.md` (CameraX/MediaPipe, APIs nativas de voz, Room offline); validar en el spike de `RK02` |
| RK06 | Los KPIs del PRD (WAU, sesiones) son poco medibles con 1-5 usuarios | Alta | Bajo | Medición manual en la beta personal o redefinición de criterios (`S03`) |

## Decisiones a revisar

- **«Sin precios» (`P12`, `S02`)** — el mockup de Detalle de Receta dibuja «coste casero estimado»; decidimos no mostrar precios en ninguna vista. Importa porque el material de `uxdocs/` y el MVP quedan en desacuerdo explícito.
- **Entrada en «Mis Recetas» (`P18`)** — Mis Recetas arranca vacía; la alternativa (abrir en el Escáner) da una primera impresión más rica. Si se mantiene, el estado vacío debe empujar hacia el Escáner.
- **MediaPipe en lugar de YOLOv8 ONNX (`P23`)** — cambia el riesgo técnico principal (`RK02`); conviene validar el modelo concreto antes de construir el Escáner.
- **Sin ubicaciones físicas (`P37`)** — elimina el checklist por ubicaciones (nevera/despensa/vinagrera/especiero) del PRD; el Módulo 3 pierde esa agrupación de ingredientes.
- **Observabilidad mínima asumida (`P29` → `S01`)** — nadie la confirmó explícitamente.
