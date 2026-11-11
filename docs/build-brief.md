# Build brief

## Problema

- **Fatiga de decisión**: se pierde tiempo diario decidiendo qué cocinar con ingredientes sueltos.
- **Desperdicio alimentario**: los alimentos frescos se tiran por falta de planificación.
- **Fricción al cocinar**: consultar la pantalla con las manos ocupadas o sucias arruina tiempos y cocciones.
- **Desconexión receta-Despensa**: los recetarios piden ingredientes que no se tienen.
- **Dudas sin resolver al momento**: sustituciones, técnicas, cantidades o «¿qué hago con esto?» obligan a salir a buscar en otra app o web.

## Sistema actual / workaround

Hoy se resuelve con recetarios genéricos (papel, web, apps) que piden ingredientes que no se tienen, listas de compra manuales e inconexas, y planificación de memoria: el usuario compra comida y acaba tirándola, o pide a domicilio por no saber qué preparar.

El PRD de origen (`chefencasa_prd_y_project_brief_completo.md`, «ChefEnCasa») proponía una PWA React 18/Vite/TS con backend Python FastAPI o Node.js + PostgreSQL, YOLOv8 ONNX y Gemini/Claude. En el descubrimiento el usuario fijó un rumbo distinto: **app Android nativa, sin backend, IA real desde el MVP**. No hay sistema previo que migrar: el proyecto arranca desde cero.

## Usuarios objetivo

Uso personal (1-5 usuarios): personas que cocinan en casa, con poco tiempo para planificar, que quieren aprovechar lo que ya tienen (p. ej. el profesional ocupado o la pareja que compra comida y la tira por no planificar). Un único rol: el **Usuario** propietario del dispositivo; sin cuentas ni login (`P7`, `P31`).

## Objetivos

- **Principal**: decidir y cocinar «¿qué hay hoy?» en pocos minutos usando los alimentos de la Despensa, y poder salir de esa restricción con el Pinche cuando la Despensa no alcanza.
- Aprovechar los alimentos que ya hay en casa y reducir el desperdicio alimentario.
- Cocinar manos libres: guía paso a paso legible a distancia con voz y temporizadores.
- Resolver dudas de cocina en el momento y pedir recetas sin quedar atado a lo que haya en la Despensa.
- Cerrar el ciclo Despensa → Receta → compra: lo que falta para una Receta acaba en la Lista de la Compra.

## Qué construimos (Rebanada MVP)

La rebanada que prueba el valor de punta a punta: **el usuario escanea lo que tiene en casa, obtiene Recetas compatibles, acepta una y cocina guiado, sin salir de la app y sin red salvo la IA**. El comportamiento más arriesgado (detección de alimentos on-device + sugerencias por IA real) queda dentro del MVP en lugar de posponerse (`P4`, `RK02`).

- **Mi recetario (CRUD)** — vista de entrada de la app; buscador, filtro por Momento culinario, tarjetas con badge de Coincidencia, alta, edición y borrado de Recetas.
- **Escáner de la Despensa** — alta manual y captura con la cámara; la IA analiza la foto y propone Alimentos con cantidad como chips editables que se guardan en la Despensa. Solo llena la Despensa: las Recetas compatibles viven en su propia pantalla.
- **Recetas con mi Despensa** — lista las Recetas compatibles (RD02) con su badge y desglose, y da paso a las Sugerencias de IA.
- **Sugerencias de recetas por IA** — la IA propone Recetas compatibles con lo escaneado; al aceptar una, se guarda en Mi recetario.
- **Detalle de Receta** — métricas (tiempo total, dificultad, calorías por porción), escalador de Raciones (1-12) con recálculo de cantidades, checklist de ingredientes, pasos resumidos y CTA de Modo Cocina.
- **Modo Cocina** — paso a paso manos libres, alto contraste legible a distancia, barra de avance, temporizador con alarma y `+1 min`, locución de instrucciones, comandos de voz y drawer con los ingredientes del Paso activo.
- **Pinche** — asistente conversacional con chips de sugerencias y burbujas. Responde dudas de cocina y propone recetas **sin depender de la Despensa**; la propuesta se muestra en una Ficha con sus ingredientes, pasos y Faltantes, y desde ella se guarda o se descarta. La app expone al modelo **tres funciones**: dos de **solo lectura** (Despensa y Mi recetario) para que pida contexto cuando lo necesita y **`save_profile`, de escritura acotada al perfil**, con la que el Pinche rellena «Mi perfil» en el chat antes de proponer (`rellenar-perfil-con-el-pinche`). Conversación solo en memoria. Dictado por voz y envío de Faltantes a la Lista de la Compra quedan para sus features (ver «Qué NO construimos»).
- **Formulario de Receta** — creación/edición con foto, metadatos (tiempo de preparación con reloj y sin tiempo de cocción), ingredientes con unidades, Pasos con temporizador, y guardado único sin estados (guardar + marcar favorito).
- **Lista de la Compra** — alta de ítems manuales o por voz, detección de faltantes tras escaneos o visualización de recetas, agrupación por Pasillo o por Receta, badge de Origen y progreso con checkboxes.
- **Interfaz en español e inglés**.

El desglose por sesiones de implementación vive en `feature_list.json`; este documento define el qué y el hasta dónde, no el plan de tareas.

## Qué NO construimos

### Fuera del MVP (puede venir después)

- **Cuentas, registro y login** — el MVP usa sesión local con datos en el dispositivo (`P31`).
- **Sincronización entre dispositivos y hogar compartido** — la pareja que comparte nevera y lista llega después (`P7`).
- **Backend y base de datos en servidor** — queda como fase posterior (`P34`; su stack en `docs/technical-discovery.md`); el MVP es 100% cliente (`P33`).
- **Seguimiento de caducidad de alimentos** — fuera del MVP, posible ampliación futura (`P5`).
- **Precios y coste estimado por receta** — la app no muestra precios en ninguna vista (`P12`, `⚠ S02`).
- **Tablet, escritorio y PWA web** — el MVP es solo Android (`P20`).
- **Publicación en Play Store** — distribución por APK instalada a mano (`P28`).
- **Catálogo compartido entre usuarios** — las Recetas aceptadas van solo a Mi recetario del usuario (`P32`).
- **Dictado por voz en el Pinche** — llega con la voz del Modo Cocina y la Lista de la Compra (`⚠ S04`); esta versión del Pinche es solo texto.
- **Enviar los Faltantes del Pinche a la Lista de la Compra** — la Ficha solo los muestra; el alta llega con `anadir-faltantes-a-la-lista`.
- **Historial de conversación entre sesiones** — la conversación del Pinche vive solo en memoria mientras dura la pantalla.
- **Desglose por ubicaciones físicas** (nevera/vinagrera/especiero del PRD) — solo existe la Despensa (`P37`).

### Fuera del producto (no es su propósito)

- **Compra de alimentos online o delivery** — la Lista de la Compra es manual.
- **Red social o feed de recetas** — no hay contenido público entre usuarios.
- **Nutrición clínica o dietas médicas** — las calorías son informativas, no prescripción.
- **Asistente generalista** — el Pinche solo atiende dudas de cocina y recetas; no responde temas ajenos a la cocina.

## Restricciones relevantes

- Solo Android (plataforma móvil), un solo repositorio (`P20`, `P24`).
- Sin backend en el MVP; datos duraderos en el dispositivo (`P31`, `P33`).
- IA real desde el MVP: detección de alimentos en el propio dispositivo y asistente/sugerencias vía un proveedor de IA externo (`P4`); las tecnologías concretas en `docs/technical-discovery.md`.
- El Pinche usa **function calling**: la app expone dos funciones de solo lectura (Despensa y Mi recetario) y, desde `rellenar-perfil-con-el-pinche`, una tercera de escritura **`save_profile`, acotada al perfil** —el único dato que el modelo puede escribir en el dispositivo—, y el modelo decide cuándo pedirlas (`⚠ S08`). Si el proveedor no las admite, la app falla de forma visible con «Reintentar» y no cambia en silencio los datos que salen del dispositivo.
- RGPD: las fotos de escaneo no se almacenan y el proveedor de IA no retiene datos (`P27`).
- La API key de IA la introduce el usuario en Ajustes; nunca se guarda en el repositorio (`⚠ S05`).
- Interfaz en español e inglés; documentación en español y código en inglés (`P19`, `P25`).
- Sin fecha límite; presupuesto de IA sin límite definido para el MVP (`P3`, `P30`).

## Plan de validación

- **Automatizada**: los scripts del repo (`init`, `lint`, `unit`, `e2e`) — `lintDebug`, `testDebugUnitTest` y `connectedDebugAndroidTest` (pruebas instrumentadas Compose sobre emulador, con fixtures locales y cliente de IA simulado). Una funcionalidad pasa a `passing` con `lint` + `unit` en verde.
- **En entorno real**: los `verify` de cada feature de `feature_list.json` se ejecutan sobre emulador/dispositivo (`adb` + uiautomator para medidas y capturas); solo entonces pasa a `validated`. No vale la autopercepción del modelo.
- **Con el usuario**: uso personal (1-5 usuarios) como prueba real del MVP; los criterios de éxito siguientes se revisan con él (⚠ S03).
- El detalle de estados y qué significa «hecho» está en `AGENTS.md`.

## Criterios de éxito

- Más del 65% de las sesiones completan Recetas con Coincidencia superior al 80% (`⚠ S03`).
- Más del 50% de las Recetas vistas activan el Modo Cocina.
- Confianza media de detección del Escaneo superior al 88%.
- 40% de retención a 30 días (matizado por el uso personal del MVP).

## Notas

- Términos y lenguaje común: `CONTEXT.md`. Reglas de negocio: `docs/domain-model.md`. Interfaz: `DESIGN.md`. Stack y operación: `docs/technical-discovery.md`.
- Supuestos, riesgos y preguntas abiertas: `docs/risks-and-open-questions.md`. Todo lo marcado `⚠` se consulta antes de decidir por cuenta propia.
- Los materiales de `uxdocs/` son orientativos (`P16`) y sus nombres conservan «ChefEnCasa» por ser de origen.
