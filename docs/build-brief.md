# Build brief

## Producto

**What's for Lunch?** es una app Android que transforma los alimentos que ya hay en casa en recetas para cocinar: escanea la Despensa, sugiere recetas compatibles y guía la cocina paso a paso. (El PRD de origen la llamaba «ChefEnCasa»; el nombre oficial es What's for Lunch? — `P1`.)

## Problema que resuelve

- **Fatiga de decisión**: se pierde tiempo diario decidiendo qué cocinar con ingredientes sueltos.
- **Desperdicio alimentario**: los alimentos frescos se tiran por falta de planificación.
- **Fricción al cocinar**: consultar la pantalla con las manos ocupadas o sucias arruina tiempos y cocciones.
- **Desconexión receta-Despensa**: los recetarios piden ingredientes que no se tienen.

## Usuarios objetivo

Uso personal (1-5 usuarios): personas que cocinan en casa, con poco tiempo para planificar, que quieren aprovechar lo que ya tienen (p. ej. el profesional ocupado o la pareja que compra comida y la tira por no planificar).

## Objetivo principal

Decidir y cocinar «¿qué hay hoy?» en pocos minutos, usando solo los alimentos de la Despensa.

## Qué construimos (MVP)

- **Mis Recetas (CRUD)** — vista de entrada de la app; buscador, filtro por Momento culinario, tarjetas con badge de Coincidencia, alta, edición y borrado de Recetas.
- **Escáner de la Despensa** — captura con la cámara, detección de Alimentos en el dispositivo, chips editables para corregir/confirmar, y carrusel de Recetas compatibles (100% / 85%).
- **Sugerencias de recetas por IA** — la IA propone Recetas compatibles con lo escaneado; al aceptar una, se guarda en Mis Recetas.
- **Detalle de Receta** — métricas (tiempo total, dificultad, calorías por porción), escalador de Raciones (1-12) con recálculo de cantidades, checklist de ingredientes (obligatorios/opcionales), utensilios, pasos resumidos y CTA de Modo Cocina.
- **Modo Cocina** — paso a paso manos libres, alto contraste legible a distancia, barra de avance, temporizador con alarma y `+1 min`, locución de instrucciones, comandos de voz y drawer con los ingredientes del Paso activo.
- **Pinche** — asistente conversacional con chips de sugerencias, cards ejecutables de Receta y dictado por voz.
- **Formulario de Receta** — creación/edición con foto, metadatos, ingredientes con unidades y opcionalidad, Pasos con temporizador, y guardado dual Borrador/Publicar.
- **Lista de la Compra** — alta de ítems manuales o por voz, detección de faltantes tras escaneos o visualización de recetas, agrupación por Pasillo o por Receta, badge de Origen y progreso con checkboxes.
- **Interfaz en español e inglés**.

## Qué NO construimos

### Fuera del MVP (puede venir después)

- **Cuentas, registro y login** — el MVP usa sesión local con datos en el dispositivo (`P31`).
- **Sincronización entre dispositivos y hogar compartido** — la pareja que comparte nevera y lista llega después (`P7`).
- **Backend y base de datos en servidor** — queda como fase posterior (`P34`; su stack en `docs/technical-discovery.md`); el MVP es 100% cliente (`P33`).
- **Seguimiento de caducidad de alimentos** — fuera del MVP, posible ampliación futura (`P5`).
- **Precios y coste estimado por receta** — la app no muestra precios en ninguna vista (`P12`, `⚠ S02`).
- **Tablet, escritorio y PWA web** — el MVP es solo Android (`P20`).
- **Publicación en Play Store** — distribución por APK instalada a mano (`P28`).
- **Catálogo compartido entre usuarios** — las Recetas aceptadas van solo a Mis Recetas del usuario (`P32`).
- **Desglose por ubicaciones físicas** (nevera/vinagrera/especiero del PRD) — solo existe la Despensa (`P37`).

### Fuera del producto (no es su propósito)

- **Compra de alimentos online o delivery** — la Lista de la Compra es manual.
- **Red social o feed de recetas** — no hay contenido público entre usuarios.
- **Nutrición clínica o dietas médicas** — las calorías son informativas, no prescripción.

## Restricciones relevantes

- Solo Android (plataforma móvil), un solo repositorio (`P20`, `P24`).
- Sin backend en el MVP; datos duraderos en el dispositivo (`P31`, `P33`).
- IA real desde el MVP: detección de alimentos en el propio dispositivo y asistente/sugerencias vía un proveedor de IA externo (`P4`); las tecnologías concretas en `docs/technical-discovery.md`.
- RGPD: las fotos de escaneo no se almacenan y el proveedor de IA no retiene datos (`P27`).
- La API key de IA la introduce el usuario en Ajustes; nunca se guarda en el repositorio (`⚠ S05`).
- Interfaz en español e inglés; documentación en español y código en inglés (`P19`, `P25`).
- Sin fecha límite; presupuesto de IA sin límite definido para el MVP (`P3`, `P30`).

## Criterios de éxito

- Más del 65% de las sesiones completan Recetas con Coincidencia superior al 80% (`⚠ S03`).
- Más del 50% de las Recetas vistas activan el Modo Cocina.
- Confianza media de detección del Escaneo superior al 88%.
- 40% de retención a 30 días (matizado por el uso personal del MVP).

Supuestos, riesgos y preguntas abiertas: ver `risks-and-open-questions.md`.
