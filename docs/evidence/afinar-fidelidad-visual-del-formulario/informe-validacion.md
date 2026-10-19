# Informe de validación — `afinar-fidelidad-visual-del-formulario`

- **Ciclo**: 3 (final). Cierre: **`validated`**.
- **Entorno**: AVD `Pixel_10a` (API 37, 1080×2424, densidad 420 dpi), app `com.whatsforlunch.app`, locale `es-ES`.
- **Fecha**: 2026-10-17.
- **Validador**: agente `feature-validate` (wrapper de ejecución, skill `$feature-validate`).

## 1. Arnés (preflight + ejecución)

| Paso | Comando | Resultado |
|---|---|---|
| Preflight | `feature_list.json` status `passing`, JSON válido | ✓ |
| Arranque | `bash ./init.sh` | exit 0 («Entorno listo») |
| Estático | `bash ./lint.sh` | exit 0 (`BUILD SUCCESSFUL`) |
| Unit | `bash ./unit.sh` | exit 0 — 10 tests (RecipeValidationTest 8, MisRecetasStateTest 2), 0 fallos |
| E2E | `bash ./e2e.sh` | exit 0 (`BUILD SUCCESSFUL`) — 29 tests, 0 fallos, 0 errores (incluye la regresión de `crear-receta`) |

## 2. Matriz de requisitos (verify 2 — uiautomator + recorrido manual)

Recorrido manual por UI real: alta completa (foto desde el selector del sistema con imagen sembrada en MediaStore, título «Lentejas de la abuela», ingrediente «Lentejas»/«2», paso «Sofreir y anadir lentejas») y segunda receta solo con título («Tortilla de patatas») guardada como borrador desde el dock.

| RF | Requisito | Resultado | Evidencia (ciclo 3) |
|---|---|---|---|
| RF-1 | Orden: portada → info básica → momento → detalles → ingredientes → pasos → favorito → dock | CUMPLE | dumps `ui-form-empty`, `ui-f8`, `ui-fav2` (bloques en orden) |
| RF-2 | Cabecera «Volver» + título + estado «Borrador» | CUMPLE | `ui-form-empty`: «Volver», «Crear receta», «Borrador» |
| RF-3 | Cabecera con título y distintivo de estado | CUMPLE (revisado 2026-10-19) | `ui-form-empty`: «Volver», «Crear receta», «Borrador»; ya **sin** «Guardar» (Q-editar-receta-5) |
| RF-4 | Portada con «Sin foto (opcional)» y Galería/Cámara/Generar con IA | CUMPLE | `ui-form-empty`: 3 controles tras el título de portada |
| RF-5 | «Paso a paso con IA» en cabecera, no bloque de sección | CUMPLE | `ui-fav2`: presente en la cabecera, fuera de secciones |
| RF-6 | Bloque de favorito «Marcar en Mis Favoritas» + subtítulo | CUMPLE | `ui-fav2`: «Marcar en Mis Favoritas», «Acceso directo en tu biblioteca» |
| RF-7 | Pasos con círculo numerado y «Temporizador (min, opcional)» | CUMPLE | `ui-f7`/`ui-f8`: «1» + «Texto del paso» + «Temporizador (min, opcional)» |
| RF-8 | «Detalles de cocina» con Tiempo/Cocción, Nivel, Raciones con −/+ | CUMPLE | `ui-f8`: «Preparación»/«Cocción»/«min», «Fácil Media Difícil», «4 pers − +» |
| RF-9 | Contenedor de sección blanco | CUMPLE | verify 1: `#FFFFFF` (255,255,255) |
| RF-10 | Campo relleno `#F5F3EF` con borde cálido | CUMPLE | verify 1: interior (245,243,239) = `#F5F3EF` |
| RF-11 | Chip seleccionado `#A83212`/texto claro; libres `#F5F3EF` | CUMPLE | verify 1: fila chips y «Media» (168,50,18) / «Fácil» (245,243,239) |
| RF-12 | Anillo de foco `primary` solo con foco | CUMPLE | verify 1: 0 px sin foco vs 4 348+5 256+1 196 px con foco |
| RF-13 | Sombbras WarmShadow (no grises frías) | CUMPLE | verify 1: sombra cálida R−B≈5–6; tokens exactos de `uxdocs/DESIGN.md` en `WarmShadow.kt` |
| RF-14 | Botón «Publicar» deshabilitado distinguible | CUMPLE | verify 1: (228,226,222) ≈ `#E4E2DE` + texto `#59413B` (vs habilitado `#A83212`) |
| RF-15 | Lista «No se puede publicar todavía:» junto al dock | CUMPLE | `ui-form-empty` (4 ítems) → `ui-f5` (1) → `ui-f7` («Hay pasos sin texto») → desaparece al completar |
| RF-16 | «Publicar» habilitado solo con todo completo | CUMPLE | `ui-form-empty` enabled=false → `ui-f8` enabled=true |
| RF-17 | Badge de momento con foto: esquina superior izquierda de la portada | CUMPLE | `ui-rf21`: (105,357,242,393) sobre portada (42,310,1038,772) |
| RF-18 | Metadatos: tiempo, nivel, raciones con iconos de rol correcto | CUMPLE | verify 1: iconos `tertiary` 293 px / `secondary` 399 px / `primary` 319 px |
| RF-19 | Píldora «Borrador» a la derecha del título | CUMPLE | verify 1: `primary/10` = (246,234,231) + texto `primary` (878 px) |
| RF-20 | Tarjeta blanca con sombra WarmShadow | CUMPLE | verify 1: tarjeta (255,255,255); sombra cálida (251,249,246) |
| RF-21 | Sin «Guardar» de cabecera; el dock guarda el Borrador | CUMPLE (revisado 2026-10-19) | `headerHasNoSaveActionAndDockSavesDraft`: 0 nodos `HEADER_SAVE`; tap «Guardar Borrador» → tarjeta «Tortilla de patatas»/«Borrador» |
| RF-22 | Sin foto: badge junto al título, sin portada ni metadatos | CUMPLE | `ui-g2`: badge (105,1074,242,1110) junto a título (284,1062,804,1121) |

**22/22 CUMPLEN.**

## 3. Fidelidad visual 1:1 (verify 1 — muestreo de píxeles)

Muestreo sobre las capturas de evidencia (`formulario-receta.png`, `mis-recetas-tarjetas.png`) y temporales del estado deshabilitado/focalizado.

| Atributo | Prototipo (`code.html` / `DESIGN.md`) | Medido en la app | ¿Coincide? |
|---|---|---|---|
| Sección | `#FFFFFF` | (255,255,255) | ✓ |
| Campo relleno | `#F5F3EF` | (245,243,239) = `#F5F3EF` | ✓ |
| Chip seleccionado | `#A83212` + `#FFFFFF` | (168,50,18) + glifos claros (2 390 px blancos en botón analogo; chips con mezcla 218–228 de AA sobre terracota) | ✓ |
| Chip libre | `#F5F3EF` | (245,243,239) | ✓ |
| Anillo de foco | `primary` solo con foco | 0 px / +10 700 px | ✓ |
| **Botón «Guardar Borrador» (dock)** | **`surface-container` `#EFEEEA` / `on-surface-variant` `#59413B`** | **(239,238,234) = `#EFEEEA`; glifos 3 368 px en `#59413B`** | **✓ (B-2 corregido)** |
| Botón «Publicar Receta» | `#A83212` + `#FFFFFF` | (168,50,18) + 2 390 px blancos | ✓ |
| Botón deshabilitado | superficie alta + `#59413B` | (228,226,222) + 2 636 px en `#59413B` | ✓ |
| Badge de momento | `surface-container-lowest/95` + `primary` | ~3 478 px blanquecinos + 891 px `primary` | ✓ |
| Píldora «Borrador» | `primary/10` + `primary` | (246,234,231) + 878 px `primary` | ✓ |
| Iconos de metadatos | `tertiary`/`secondary`/`primary` | 293/399/319 px por rol | ✓ |
| Sombra | WarmShadow cálida (tokens `DESIGN.md`) | R−B≈5–6 bajo tarjeta; tokens exactos en `WarmShadow.kt` | ✓ |

**Sin divergencias bloqueantes.** Divergencia del ciclo 2 (**B-2**, rol de color del «Guardar Borrador») **corregida y verificada por píxeles** en este ciclo.

## 4. Requisitos no funcionales

| NFR | Requisito | Resultado |
|---|---|---|
| NFR-1 | Textos es/en equivalentes | CUMPLE — 79/79 claves, sin diferencias entre `values` y `values-en` |
| NFR-2 | Táctil ≥ 48 dp | CUMPLE — dock «Guardar Borrador» 148 px, «Publicar» 137 px (52,2 dp), chips `heightIn(48.dp)`, botones de cabecera ≥ 48 dp (hoy solo «Volver») |
| NFR-4 | Colores con los roles del prototipo | CUMPLE — verify 1 (tabla §3, incluido B-2) |

## 5. Alcance y evidencia

- `git status`: solo archivos de esta feature (12 modificados + `WarmShadow.kt` + carpeta de evidencia). Sin trabajo de otras features. Scripts del arnés (`init/lint/unit/e2e*`) intactos.
- Evidencia (3 artefactos ≤ presupuesto 4, sustituye al set del ciclo 2): este informe + `formulario-receta.png` + `mis-recetas-tarjetas.png`.

## 6. Observaciones (no bloqueantes)

1. ~~Icono del «Guardar Borrador»: `Save` frente a `drafts` del prototipo~~ — **corregido en este ciclo** (`Icons.Outlined.Drafts`).
2. Titular del formulario «Crear receta» frente a «Crear Receta» del prototipo (mayúscula inicial).
3. «Volver» como texto frente al icono `close` del prototipo.
4. Fila de Ingrediente segmentada frente a la tira continua con filete del prototipo.
5. Badge de momento en `text-primary` fijo frente al color por momento del prototipo.

Matices de acabado no exigidos por la Parte A; no impiden `validated`.

## 7. Pendiente documentado (no es bug de producto)

- **T6 (parcial)**: falta actualizar `docs/specs/crear-receta.md` (RF-15/RF-16, Casos límites, Plan de verificación y Trazabilidad) por restricción de permisos sobre `docs/specs/**`. La parte de tests E2E de la T6 (adaptación + regresión) está completa y en verde. Decisión registrada en `PROGRESS.md` (Sesión 14): no retiene la feature; la recogerá un agente con permiso sobre `docs/specs/**`. Por ello su checkbox queda sin marcar.

## 8. Veredicto

**`validated`.** Arnés completo en verde (init/lint/unit/e2e, 29 E2E sin fallos), RF-1…RF-22 verificadas en entorno real, fidelidad 1:1 confirmada por muestreo de píxeles (B-2 corregido), NFR-1/NFR-2/NFR-4 cumplidos, alcance y evidencia dentro del presupuesto.

### Revisión 2026-10-19 — retirada de «Guardar» de la cabecera (Q-editar-receta-5)

- **RF-3 y RF-21 revisados**: el usuario pidió eliminar el «Guardar» de la cabecera en todos los modos por ser redundante con el dock. La cabecera queda con «Volver», título, distintivo de estado y «Paso a paso con IA».
- **Afecta a**: `CrearRecetaScreen.kt` (`FormHeader` sin `actions`), `strings.xml` es/en (retirada de `action_save`), `CrearRecetaE2ETest` (los dos casos de «Guardar» de cabecera se sustituyen por `headerHasNoSaveActionAndDockSavesDraft`) y `EditarRecetaE2ETest`.
- **Regresión ejecutada en esta revisión**: `lint` exit 0, `unit` exit 0 y `e2e` exit 0 (44 tests, 0 fallos) — cubre `afinar-fidelidad-visual-del-formulario` y `crear-receta`.
- **Desviación declarada**: el prototipo dibuja el botón; se omite por decisión explícita del usuario. El resto de RF (1, 2, 4-20 y 22) no cambian.
- **Veredicto tras la revisión**: `validated`.
