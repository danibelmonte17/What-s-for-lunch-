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
| `uxdocs/chefencasa_modo_cocina_paso_a_paso/` | HTML + imagen | Paso a paso, barra de avance, temporizador, voz | Inspiración (`P16`) | Alarma sonora; permisos de micrófono |
| `uxdocs/chefencasa_pinche_asistente_de_cocina/` | HTML + imagen | Chat con chips de sugerencia y cards ejecutables | Inspiración (`P16`) | Estado sin conexión; error del proveedor de IA |
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
| Mis Recetas (entrada de la app, `P18`) | Encontrar y gestionar recetas | Buscador con debounce, filtro por Momento culinario, tarjetas con badge de Coincidencia, FAB de creación | `chefencasa_mis_recetas_y_crud/` |
| Escáner de la Despensa | Añadir Alimentos a mano o fotografiar la Despensa | Alta manual primero en el listado, botón «Escanear con la cámara» que despliega el visor con animación y lo pliega tras el análisis, progreso «Analizando la foto…», chips editables sin confianza con borrado terracota, recetas compatibles 100%/85% | `chefencasa_esc_ner_de_nevera_ia/` |
| Detalle de Receta | Decidir y preparar la cocina | Hero, métricas, escalador de Raciones `[-] N pers [+]`, checklist, CTA Modo Cocina | `chefencasa_detalle_de_receta/` |
| Modo Cocina | Cocinar manos libres | Barra «Paso X de Y», temporizador circular + alarma + `+1 min`, locución y comandos de voz, drawer de ingredientes | `chefencasa_modo_cocina_paso_a_paso/` |
| Pinche | Resolver dudas al instante | Chips de sugerencia, burbujas, cards ejecutables, micrófono con dictado | `chefencasa_pinche_asistente_de_cocina/` |
| Formulario de Receta | Crear/editar con foto | Metadatos, gestor de ingredientes con unidades y opcionalidad, Pasos con temporizador, Borrador/Publicar | `chefencasa_registrar_y_editar_receta/` |
| Lista de la Compra | Comprar solo lo que falta | Tabs por Pasillo/Receta, badges de Origen, checkboxes, barra de progreso, alta manual/voz | `chefencasa_lista_de_la_compra_sincronizada/` |
| Ajustes | Configurar la app | Idioma (es/en), API key de IA, aviso de privacidad | Sin referencia (`RK04`) |

## Estados a cubrir

- **Vacío**: Mis Recetas vacía y Lista de la Compra vacía → CTA de crear Receta o escanear la Despensa (ver casos límite en `docs/domain-model.md`).
- **Carga**: análisis de la foto del Escaneo y respuestas del Pinche/Sugerencias → progreso claro; la app nunca se congela.
- **Error**: fallo de IA/red → Modo de degradación con entrada manual y aviso (RD05).
- **Éxito**: confirmación al aceptar una Sugerencia, publicar una Receta o completar la Lista.

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
- **Alarma sonora del temporizador y animaciones**: el material estático no las define → propuesta libre dentro de los tokens.
- **Copy exacto de estados vacíos**: lo propone cada spec y se valida en la revisión visual (Q-arrancar-app-en-mis-recetas-5).
