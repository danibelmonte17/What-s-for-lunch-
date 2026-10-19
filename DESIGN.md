# Diseño e interfaz

Dirección inicial. Lo que procede de material de referencia cita su archivo; lo demás es propuesta. El material de `uxdocs/` es **orientativo** (`P16`): inspiración a respetar en espíritu, con libertad para ajustar componentes y jerarquía.

## Material de referencia

| Archivo o enlace | Tipo | Qué cubre | Estatus | Qué no cubre |
|---|---|---|---|---|
| `uxdocs/DESIGN.md` | DESIGN.md (tokens + guía) | Sistema «Warm Culinary Craft»: colores, tipografía, spacing, formas, componentes | Orientativo (`P16`); los tokens YAML mandan sobre su prosa (`P17`) | Estados vacío/carga/error; interacciones |
| `uxdocs/chefencasa_mis_recetas_y_crud/` (`code.html`, `screen.png`) | HTML + imagen | Catálogo con buscador, filtros, tarjetas y acciones CRUD | Orientativo | Estados vacío/carga; pantalla de confirmación de borrado |
| `uxdocs/chefencasa_esc_ner_de_nevera_ia/` | HTML + imagen | Visor de cámara, HUD de detección, chips, carrusel de recetas | Orientativo | Estado sin detecciones; fallo de la IA |
| `uxdocs/chefencasa_detalle_de_receta/` | HTML + imagen | Hero, métricas, escalador de raciones, ingredientes, CTA Modo Cocina | Orientativo | Muestra «coste casero estimado», que la app no tendrá (`P12`, `⚠ S02`) |
| `uxdocs/chefencasa_modo_cocina_paso_a_paso/` | HTML + imagen | Paso a paso, barra de avance, temporizador, voz | Orientativo | Alarma sonora; permisos de micrófono |
| `uxdocs/chefencasa_pinche_asistente_de_cocina/` | HTML + imagen | Chat con chips de sugerencia y cards ejecutables | Orientativo | Estado sin conexión; error del proveedor de IA |
| `uxdocs/chefencasa_registrar_y_editar_receta/` | HTML + imagen | Formulario modal de receta (metadatos, ingredientes, pasos) | Orientativo | Validaciones de campos; guardado Borrador/Publicar |
| `uxdocs/chefencasa_lista_de_la_compra_sincronizada/` | HTML + imagen | Lista agrupada por Pasillo/Receta, badges de Origen, progreso | Orientativo | Estado vacío; alta por voz |

Los nombres de carpeta conservan «ChefEnCasa» y «escáner de nevera» por ser material de origen: el producto es **What's for Lunch?** y sus alimentos viven en la **Despensa** (`CONTEXT.md`).

## Principios de experiencia

- **Calidez táctil (Tactile Modern)**: superficies cálidas y esquinas suaves; nada de minimalismo frío (`uxdocs/DESIGN.md`).
- **Legibilidad a distancia en Modo Cocina**: instrucciones grandes, alto contraste (AAA), legibles a 1,5 m.
- **Objetivos táctiles ≥ 48×48 px**: se cocina con manos ocupadas o mojadas.
- **IA discreta**: la tecnología se nota en la ayuda, no en la interfaz (chips, badges, sugerencias).

## Vistas y flujos principales

| Vista | Objetivo | Componentes clave | Referencia |
|---|---|---|---|
| Mis Recetas (entrada de la app, `P18`) | Encontrar y gestionar recetas | Buscador con debounce, filtro por Momento culinario, tarjetas con badge de Coincidencia, FAB de creación | `chefencasa_mis_recetas_y_crud/` |
| Escáner de la Despensa | Detectar Alimentos y obtener recetas | Visor de cámara, HUD con etiquetas y confianza, chips editables, carrusel 100%/85% | `chefencasa_esc_ner_de_nevera_ia/` |
| Detalle de Receta | Decidir y preparar la cocina | Hero, métricas, escalador de Raciones `[-] N pers [+]`, checklist, CTA Modo Cocina | `chefencasa_detalle_de_receta/` |
| Modo Cocina | Cocinar manos libres | Barra «Paso X de Y», temporizador circular + alarma + `+1 min`, locución y comandos de voz, drawer de ingredientes | `chefencasa_modo_cocina_paso_a_paso/` |
| Pinche | Resolver dudas al instante | Chips de sugerencia, burbujas, cards ejecutables, micrófono con dictado | `chefencasa_pinche_asistente_de_cocina/` |
| Formulario de Receta | Crear/editar con foto | Metadatos, gestor de ingredientes con unidades y opcionalidad, Pasos con temporizador, Borrador/Publicar | `chefencasa_registrar_y_editar_receta/` |
| Lista de la Compra | Comprar solo lo que falta | Tabs por Pasillo/Receta, badges de Origen, checkboxes, barra de progreso, alta manual/voz | `chefencasa_lista_de_la_compra_sincronizada/` |
| Ajustes | Configurar la app | Idioma (es/en), API key de IA, aviso de privacidad | Sin referencia (`RK04`) |

## Lenguaje visual inicial

Procede de `uxdocs/DESIGN.md`; mandan sus **tokens YAML** (`P17`):

- **Tipografía**: Plus Jakarta Sans (títulos/labels) e Inter (cuerpo). En Modo Cocina, cuerpo ≥ 18 px (`body-lg`).
- **Paleta**: primary `#a83212` (terracota), secondary `#006c4a` (verde salvia), tertiary `#8d4b00` (ámbar), superficies cálidas `#fbf9f5`… según el front-matter de `uxdocs/DESIGN.md`. (La prosa «Brand & Style» con `#E05A37` queda descartada por `P17`.)
- **Espaciado y forma**: escala `space-xs`…`space-xl`; radios 0.5-1.5 rem, píldoras para chips y badges.
- **Componentes**: botones primarios terracota (altura ≥ 52 px; excepción compacta aprobada por el usuario en las acciones del pie de la tarjeta de Mis Recetas, 36 px visuales con área táctil ≥ 48 px), chips/badges píldora, tarjetas de receta con borde tenue y sombra cálida, inputs con anillo de foco terracota.
- **Tono**: cercano, apetitoso, tranquilo.

## Estados a cubrir

- **Vacío**: Mis Recetas vacía y Lista de la Compra vacía → CTA de crear Receta o escanear la Despensa (ver casos límite en `docs/domain-model.md`).
- **Carga**: detección del Escaneo y respuestas del Pinche/Sugerencias → progreso claro; la app nunca se congela.
- **Error**: fallo de IA/red → Modo de degradación con entrada manual y aviso (RD05).
- **Éxito**: confirmación al aceptar una Sugerencia, publicar una Receta o completar la Lista.

## Dispositivos, accesibilidad e idioma

- Solo **Android móvil** en el MVP (`P20`); tablet/escritorio y PWA web fuera (`docs/build-brief.md`).
- Accesibilidad: WCAG AA general, **AAA en Modo Cocina**, targets ≥ 48×48 px.
- Idioma: **español e inglés** (`P19`).

## Lagunas del material de referencia

- Estados vacío, de carga y error de cada pantalla: no están dibujados → cubiertos en «Estados a cubrir».
- Pantalla de **Ajustes** (API key, idioma, privacidad): sin referencia → `RK04`.
- Pantalla de entrada (Mis Recetas como home) no existe como tal en el material → `P18`.
- Coste estimado que dibuja el mockup de Detalle: no se implementa → `⚠ S02`.
- Interacciones, animaciones y alarma sonora del temporizador: el material estático no las define → propuesta libre.
