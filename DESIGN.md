---
name: "What's for Lunch? — Warm Culinary Craft"
description: "Calidez táctil (Tactile Modern) para cocinar en casa con IA discreta: superficies cálidas, terracota y verde salvia, tipografía Plus Jakarta Sans + Inter y objetivos táctiles grandes"
designAssets:
  sourceOfTruth:
    - path: uxdocs/DESIGN.md
      role: sistema «Warm Culinary Craft» (tokens YAML + guía)
      notes: su prosa es orientativa (P16) y sus tokens YAML mandan (P17); carpeta uxdocs/ es de solo lectura
  reference:
    - path: uxdocs/chefencasa_mis_recetas_y_crud/
      role: catálogo Mis Recetas (code.html + screen.png)
    - path: uxdocs/chefencasa_esc_ner_de_nevera_ia/
      role: Escáner de la Despensa (cámara, chips, carrusel)
    - path: uxdocs/chefencasa_detalle_de_receta/
      role: Detalle de Receta (hero, métricas, escalador)
    - path: uxdocs/chefencasa_modo_cocina_paso_a_paso/
      role: Modo Cocina (paso a paso, temporizador, voz)
    - path: uxdocs/chefencasa_pinche_asistente_de_cocina/
      role: Pinche (chat, chips, cards ejecutables)
    - path: uxdocs/chefencasa_registrar_y_editar_receta/
      role: Formulario de Receta (metadatos, ingredientes, pasos)
    - path: uxdocs/chefencasa_lista_de_la_compra_sincronizada/
      role: Lista de la Compra (pasillos, orígenes, progreso)
  generatedConcepts: []
colors:
  primary: "#a83212"
  on-primary: "#ffffff"
  secondary: "#006c4a"
  tertiary: "#8d4b00"
  background: "#fbf9f5"
  surface: "#fbf9f5"
  surface-container: "#efeeea"
  surface-container-highest: "#e4e2de"
  text: "#1b1c1a"
  text-muted: "#59413b"
  outline: "#8c716a"
  error: "#ba1a1a"
typography:
  h1:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: "700"
    lineHeight: 36px
  body:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: "400"
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: "400"
    lineHeight: 28px
rounded:
  sm: 0.25rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  xs: 0.25rem
  sm: 0.5rem
  md: 1rem
  lg: 1.5rem
  xl: 2.5rem
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.md}"
    minHeight: "52px"
  button-compact:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.md}"
    visualHeight: "36dp"
    touchArea: ">=48dp"
  chip:
    rounded: "{rounded.full}"
  card:
    rounded: "{rounded.lg}"
---

# Dirección de diseño

## Visión general

Dirección visual de **What's for Lunch?** para agentes de codificación: tokens y reglas de aplicación, no un plan de implementación. Lo que procede de material de referencia cita su archivo; lo demás es propuesta. El material de `uxdocs/` es **orientativo** (`P16`): inspiración a respetar en espíritu, con libertad para ajustar componentes y jerarquía; ante conflicto entre su prosa y sus tokens YAML, **mandan los tokens** (`P17`). El front-matter de este archivo es la copia de trabajo de esos tokens; si difieren, manda `uxdocs/DESIGN.md`.

## Material de diseño existente

| Archivo o enlace | Tipo | Qué cubre | Autoridad | Qué no cubre |
|---|---|---|---|---|
| `uxdocs/DESIGN.md` | DESIGN.md (tokens + guía) | Sistema «Warm Culinary Craft»: colores, tipografía, spacing, formas, componentes | Tokens YAML = source of truth (`P17`); prosa = inspiración (`P16`) | Estados vacío/carga/error; interacciones |
| `uxdocs/chefencasa_mis_recetas_y_crud/` (`code.html`, `screen.png`) | HTML + imagen | Catálogo con buscador, filtros, tarjetas y acciones CRUD | Inspiración (`P16`) | Estados vacío/carga; pantalla de confirmación de borrado |
| `uxdocs/chefencasa_esc_ner_de_nevera_ia/` | HTML + imagen | Visor de cámara, chips, carrusel de recetas | Inspiración (`P16`) | HUD de detección retirado; obturador y estado «analizando» sin referencia |
| `uxdocs/chefencasa_detalle_de_receta/` | HTML + imagen | Hero, métricas, escalador de raciones, ingredientes, CTA Modo Cocina | Inspiración (`P16`) | Muestra «coste casero estimado», que la app no tendrá (`P12`, `⚠ S02`) |
| `uxdocs/chefencasa_modo_cocina_paso_a_paso/` | HTML + imagen | Paso a paso, barra de avance, temporizador, voz | Inspiración (`P16`) | Alarma sonora resuelta con el tono del sistema (`gestionar-temporizador-de-paso`, D4); la voz se implementó con los servicios del sistema y el chip «Voz» apagado por defecto (`controlar-cocina-con-voz`, RD24) |
| `uxdocs/chefencasa_pinche_asistente_de_cocina/` | HTML + imagen | Chat con chips de sugerencia y Ficha de receta; el micrófono del prototipo se difiere (`⚠ S04`) | Inspiración (`P16`) | Estado sin conexión; error del proveedor de IA; falta de API key |
| `uxdocs/chefencasa_registrar_y_editar_receta/` | HTML + imagen | Formulario modal de receta (metadatos, ingredientes, pasos) | Inspiración (`P16`) | Validaciones de campos; guardado Borrador/Publicar |
| `uxdocs/chefencasa_lista_de_la_compra_sincronizada/` | HTML + imagen | Lista agrupada por Pasillo/Receta, badges de Origen, progreso | Inspiración (`P16`) | Estado vacío; alta por voz |

Los nombres de carpeta conservan «ChefEnCasa» y «escáner de nevera» por ser material de origen: el producto es **What's for Lunch?** y sus alimentos viven en la **Despensa** (`CONTEXT.md`).

## Conceptos generados

Ninguno: no se ha usado generación de imágenes (`imagegen`). Si algún día se genera un concepto, se guarda en `docs/design/concepts/`, se referencia aquí y se marca como inspiración (nunca como fuente exacta de UI).

## Sensación del producto

- **Calidez táctil (Tactile Modern)**: superficies cálidas y esquinas suaves; nada de minimalismo frío (`uxdocs/DESIGN.md`).
- **Legibilidad a distancia en Modo Cocina**: instrucciones grandes, alto contraste (AAA), legibles a 1,5 m.
- **Objetivos táctiles ≥ 48×48 px**: se cocina con manos ocupadas o mojadas.
- **IA discreta**: la tecnología se nota en la ayuda, no en la interfaz (chips, badges, sugerencias).
- **Tono**: cercano, apetitoso, tranquilo.

## Color

Procede de `uxdocs/DESIGN.md`; mandan sus **tokens YAML** (`P17`):

- **Primary `#a83212`** (terracota): acciones primarias y focos; sobre fondos claros aporta energía sin agresividad.
- **Secondary `#006c4a`** (verde salvia): frescura, disponibilidad en la Despensa, detecciones confirmadas.
- **Tertiary `#8d4b00`** (ámbar): temporizadores, avisos y tips.
- **Superficies cálidas** `#fbf9f5`…`#e4e2de` (niveles `surface-container`), texto `#1b1c1a` / `#59413b`, outline `#8c716a`, error `#ba1a1a`.
- La prosa «Brand & Style» de `uxdocs/DESIGN.md` con `#E05A37`/`#059669`/`#D97706` queda descartada por `P17`.

## Tipografía

- **Plus Jakarta Sans** para títulos y labels; **Inter** para cuerpo e instrucciones.
- En Modo Cocina, cuerpo ≥ 18 px (`body-lg`) para leer sin agacharse.
- Escala completa (`display`…`label-sm`) en el front-matter de `uxdocs/DESIGN.md`.

## Maquetación

- Móvil (< 768 px): 4 columnas, margen 1 rem, gutter 1 rem; todo control interactivo ≥ 48×48 px.
- Ritmo de espaciado `space-xs`…`space-xl` (0.25 rem → 2.5 rem) para tarjetas, badges y banners.
- Solo aplica el tramo móvil en el MVP (sin tablet ni escritorio, `P20`); la rejilla responsive completa de `uxdocs/DESIGN.md` queda como referencia futura.

## Formas

- Lenguaje redondeado: base `0.5 rem`, tarjetas y sheets `1 rem`, hero `1.5 rem`; radios en el front-matter (`sm`…`xl`).
- Chips, badges y contadores de paso con perfil píldora (`full`).

## Componentes

- **Botón primario**: relleno terracota `#a83212`, texto blanco, radio `0.75 rem`, altura mínima 52 px.
  - **Excepción compacta aprobada por el usuario (Q-editar-receta-6 / Q-eliminar-receta-4)**: las acciones del pie de la tarjeta de Mis Recetas («Editar», «Eliminar») son botones primarios compactos de **16 dp de icono y 36 dp de altura visual**, ancho al contenido, con **área táctil ≥ 48 dp** (mínimo interactivo de Material 3). «Eliminar» lleva tinte error en lugar de terracota.
- **Botón secundario**: superficie `#efeeea` con texto `#1b1c1a`.
- **Chips y badges**: píldora; chips de detección confirmada con fondo/verde salvia.
- **Tarjetas de receta**: borde tenue y sombra cálida; badges de Coincidencia («100% Match» / «85% Match», RD02).
- **Inputs**: anillo de foco terracota; estado deshabilitado distinguible.

## Pantallas principales

| Vista | Objetivo | Componentes clave | Referencia |
|---|---|---|---|
| Mis Recetas (entrada de la app, `P18`) | Encontrar y gestionar recetas | Buscador con debounce, filtro por Momento culinario con chip «Favoritas» combinable al final de la fila (selección tipo toggle, icono `Favorite`), tarjetas con badge de Coincidencia (sin distintivos de estado), botón «Crear receta» al final del listado y en el estado vacío, top bar con solo «Ajustes», barra inferior de navegación con 5 pestañas («Mis Recetas», Despensa, Lista de la Compra, Pinche y «Mi perfil»; la activa resaltada y tocarla devuelve al inicio de la pantalla) y dos FABs apilados solo cuando hay Recetas («Recetas con mi Despensa» sobre «Escanear la Despensa») | `chefencasa_mis_recetas_y_crud/` |
| Despensa | Gestionar los Alimentos disponibles | Lista de Alimentos con nombre y cantidad y acciones «Editar»/«Eliminar» por fila, alta manual con diálogo (nombre y cantidad opcional con stepper), confirmación de borrado, estado vacío, top bar con el título y barra inferior de navegación («Despensa» activa), sin «Volver» | Sin referencia `uxdocs` (composición de componentes existentes) |
| Escáner de la Despensa | Añadir Alimentos a mano o fotografiar la Despensa | Alta manual primero en el listado, botón «Escanear con la cámara» que despliega el visor con animación y lo pliega tras el análisis, progreso «Analizando la foto…», chips editables sin confianza con borrado terracota y guardado en la Despensa (sin Recetas compatibles, que viven en su propia pantalla) | `chefencasa_esc_ner_de_nevera_ia/` |
| Recetas con mi Despensa | Decidir qué cocinar con lo que hay | Cabecera con «Volver» y título, sección «Recetas Compatibles» con tarjetas (badge 100 %/86 %, miniatura, título y «X de Y ingredientes en la Despensa»), estado vacío y botón «Sugerir receta con IA» habilitado solo con Despensa no vacía | Sin referencia `uxdocs` (composición de componentes existentes) |
| Detalle de Receta | Decidir y preparar la cocina | Hero, métricas, escalador de Raciones `[-] N pers [+]`, checklist, CTA Modo Cocina | `chefencasa_detalle_de_receta/` |
| Modo Cocina | Cocinar paso a paso | Top bar «Volver» + «Modo Cocina», título de la Receta, barra de avance (`LinearProgressIndicator` + «Paso X de Y»), **controles de voz** (chip «Voz»/«Voz activa» en `secondaryContainer` + botón «Escuchar»; con la voz activa, barra «Comandos de voz activos» con la pista «Di «Siguiente», «Repetir» o «Pausar tiempo»»; avisos de degradación en `errorContainer` —permiso, sin TTS, sin reconocimiento— con `liveRegion` polido, RD24), tarjeta del Paso (rótulo «PASO X» y texto del paso ≥ 18 sp), **widget del temporizador del Paso** (anillo circular + reloj digital `mm:ss` + «Meta: N min» + píldora de estado «Listo»/«En marcha»/«Pausado»/«¡Tiempo cumplido!» y controles «Iniciar»/«Pausar»/«Reanudar»/«Reiniciar»/«+1 min»/«Detener» en tertiary ámbar, con la alarma sonora al llegar a cero; sustituye al hint pasivo «⏱ N min»), drawer colapsable «Ingredientes para este paso» de **solo lectura** con los Ingredientes vinculados del Paso y sus cantidades escaladas (estado «Sin ingredientes» sin fallback) y dock inferior fijo con «Paso Anterior» (≥ 48 dp, deshabilitado en el Paso 1) y botón primario (≥ 52 dp) «Siguiente» → «Terminar» en el último Paso; pantalla encendida mientras está visible; **sin stepper de Raciones** (se vuelve al Detalle para cambiarlas) | `chefencasa_modo_cocina_paso_a_paso/` |
| Pinche | Resolver dudas al instante | Chips de sugerencia al inicio, burbujas de conversación, Ficha con ingredientes, pasos y Faltantes, acciones «Guardar en Mis Recetas»; **mensaje de petición del perfil** cuando falta alguna parte (lista solo lo pendiente con la acción «Abrir Mi perfil», sin respuesta de la IA, `crear-mi-perfil` D8); barra inferior de navegación («Pinche» activa), sin «Volver»; **sin micrófono** (dictado diferido, `⚠ S04`) | `chefencasa_pinche_asistente_de_cocina/` |
| Formulario de Receta | Crear/editar con foto | Metadatos, gestor de ingredientes con unidades y opcionalidad, Pasos con temporizador y sus Ingredientes usados («Ingredientes de este paso» con diálogo de casillas), botón único de guardado «Guardar»/«Guardar cambios» en el dock (sin «Borrador»/«Publicar» ni distintivos de estado) | `chefencasa_registrar_y_editar_receta/` |
| Lista de la Compra | Comprar solo lo que falta | Lista **agrupada** por «Pasillo de Super» (por defecto) o «Por Receta» con selector segmentado en la tarjeta de progreso (en memoria, se reinicia al salir), cabeceras de grupo con contador de ítems y «Sin clasificar»/«Sin receta» al final; fila con checkbox Material3, nombre y cantidad («{nombre} ({cantidad} {unidad})»), badge de Origen «Para: {Receta}»/«Para: N recetas» o «Despensa baja» (ítems sin fuentes) y etiqueta de Pasillo como texto secundario según el modo (en «Pasillo» la etiqueta la lleva la cabecera y se oculta el badge solo con fuente única en «Por Receta»); fila completada con nombre tachado, fila atenuada y checkbox relleno (el nombre nunca se sustituye); tarjeta de progreso «N de M artículos en la cesta» con porcentaje y barra (oculta con la lista vacía); «Limpiar comprados» en la top bar (solo con ≥1 completado) que borra sin confirmación y ofrece «Deshacer» en snackbar; borrado por fila con «Deshacer», alta y edición manuales con FAB «Añadir ítem» + icono de edición por fila + diálogo único (nombre de solo lectura al editar, cantidad decimal opcional, unidad opcional y 5 `FilterChip` de Pasillo con validación inline), estado vacío solo con «Añadir ítem» como CTA (sin acceso al Escáner: el escaneo vive en la Despensa, `mover-escaneo-a-la-despensa`, D2), y entrada desde la barra inferior de navegación («Lista de la Compra» activa, sin «Volver»). El alta por voz queda diferida (`anadir-item-a-la-lista`, D1) | `chefencasa_lista_de_la_compra_sincronizada/` |
| Ajustes | Configurar la app | **Idioma** (tarjeta con selector «Español»/«English» en endónimos; la elección persiste y el cambio es inmediato en toda la app sin reinicio, `cambiar-idioma-de-la-interfaz`), API key de IA, aviso de privacidad ampliado con los datos de salud que salen al chatear con el Pinche (`crear-mi-perfil`, D7) | Sin referencia (`RK04`) |
| Mi perfil | Mantener las restricciones dietéticas que respeta el Pinche | Quinta pestaña de la barra inferior (icono `Person`, al final tras Pinche); tres tarjetas al estilo de Ajustes: **Alergias** e **Intolerancias o enfermedades que limitan la dieta** con chips píldora (alta por campo + «Añadir», borrado por chip solo tras «Editar»/«Listo», icono terracota) y fila «No tengo ninguna» (marca exclusiva: limpia la chips y bloquea el alta), y **Tipo de alimentación** con 5 `FilterChip` de selección única sin preselección; estados por defecto sin nada marcado | Sin referencia `uxdocs` (composición de componentes existentes) |

## Estados a cubrir

- **Vacío**: Mis Recetas vacía → CTAs de crear Receta o escanear la Despensa (sus accesos al Escáner se conservan intactos); Lista de la Compra vacía → solo «Añadir ítem» como CTA, sin CTA de escaneo (`mover-escaneo-a-la-despensa`, D2); Despensa vacía → «Escanear alimentos» (primario, primero) y «Añadir alimento» debajo, replicando sus dos FABs apilados («Escanear alimentos» encima con icono `PhotoCamera` y «Añadir alimento» abajo; D1/D3) (ver casos límite en `docs/domain-model.md`). En «Recetas con mi Despensa», sin Recetas compatibles se muestra el estado vacío y, sin Alimentos en la Despensa, el botón «Sugerir receta con IA» aparece deshabilitado. En el **Pinche**, el primer estado muestra un saludo breve y chips de ejemplo («¿Qué cocino con lo que tengo?», «Sustituto de nata», «Una receta sin comprar nada»); el micrófono no aparece.
- **Carga**: análisis de la foto del Escaneo y respuestas del Pinche/Sugerencias → progreso claro; la app nunca se congela. En el Pinche, la entrada se deshabilita mientras se espera la respuesta.
- **Error**: fallo de IA/red → Modo de degradación con entrada manual y aviso (RD05). En el Pinche no hay entrada manual: aviso claro con «Reintentar» sin perder lo escrito; sin key → acceso a Ajustes. Si el proveedor no admite function calling, el mismo aviso (RD17).
- **Éxito**: confirmación al aceptar una Sugerencia, guardar una Receta o completar la Lista. Al guardar una Propuesta del Pinche, la Ficha queda marcada como guardada con el aviso «Guardada en Mis Recetas» y el usuario sigue en el chat.
- **Ficha del Pinche**: se despliega dentro del hilo (no modal), con ingredientes obligatorios/opcionales, pasos y Faltantes destacados; el botón «Añadir faltantes a la Lista de la Compra» aparece desde que existe la Lista y, tras pulsarlo, queda «Añadido a la Lista de la Compra» deshabilitado (mismo tratamiento que «Guardada en Mis Recetas»).
- **Actualización en caliente**: la lista de «Recetas con mi Despensa» se recalcula al cambiar la Despensa o las Recetas, sin reiniciar ni reabrir la pantalla.

## Base responsive

Solo **Android móvil** en el MVP (`P20`); tablet/escritorio y PWA web fuera del alcance (`docs/build-brief.md`). Se diseña para una mano y uso en encimera.

## Base de accesibilidad

- WCAG 2.1 AA en toda la app y **AAA en Modo Cocina**.
- Objetivos táctiles ≥ 48×48 px (la excepción compacta del pie de tarjeta conserva área táctil ≥ 48 dp).
- Contraste suficiente en badges y chips sobre superficies cálidas; idioma es/en (`P19`).

## Qué hacer y qué no

- Sí: respetar los tokens del front-matter y de `uxdocs/DESIGN.md`; cubrir estados vacío/carga/error en cada pantalla nueva; mantener la calidez (sombras cálidas, bordes tenues).
- No: usar la prosa «Brand & Style» de `uxdocs/DESIGN.md` ni sus colores `#E05A37`/`#059669`/`#D97706` (`P17`); mostrar precios (`P12`, `⚠ S02`); improvisar la pantalla de Ajustes sin pasar por `RK04`; inventar contenido de marca (logo, marketing): no existe.

## Preguntas de diseño abiertas

- **Ajustes** (API key, idioma, privacidad): sin referencia visual → diseñar siguiendo los tokens (`RK04`).
- **Alarma sonora del temporizador y animaciones**: el material estático no las define → la alarma se resuelve con el **tono de alarma del sistema** (D4 de `gestionar-temporizador-de-paso`, sin assets ni permisos) y las animaciones se limitan al anillo del widget del temporizador.
- **Copy exacto de estados vacíos**: lo propone cada spec y se valida en la revisión visual (Q-arrancar-app-en-mis-recetas-5).
