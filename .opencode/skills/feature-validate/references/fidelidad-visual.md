# Rúbrica de fidelidad visual 1:1

Procedimiento que el validador aplica cuando la feature toca una pantalla. El objetivo es comprobar que la vista desarrollada es **1:1** con el prototipo UX/UI de `uxdocs/` que la Parte B cita en «Impacto visual». Se apoya en la «Norma de fidelidad visual» de `DESIGN.md`.

## Cuándo aplica

Solo si la Parte B declara UI (pantalla, estado o estilo visible): se ejecuta tras el E2E, sobre las capturas ya tomadas en el emulador. Si no declara UI, no se aplica y no se consigna como no verificable.

## Fuentes

1. El prototipo concreto: `uxdocs/<pantalla>/code.html` (y `screen.png` si existe).
2. `uxdocs/DESIGN.md`: tokens (colores, tipografía, radios, espaciados, elevación).
3. `DESIGN.md`: «Norma de fidelidad visual» y «Estados a cubrir».
4. La Parte A de la spec: el requisito no funcional de fidelidad y las extensiones declaradas en «Fuera de alcance».

## Atributos a comparar (uno a uno)

| # | Atributo | Qué comprobar |
|---|---|---|
| 1 | Estructura y jerarquía | Bloques presentes, orden y anidamiento respecto al HTML. |
| 2 | Dimensión y posición | Alturas, anchos, `padding`, `margin`, alineación y offsets (px → dp). |
| 3 | Color | Cada color por su rol/token (`surface`, `primary`, `secondary`, `*-fixed`, `on-*`), no «parecido». |
| 4 | Forma y borde | Radios (`rounded-*`), bordes (`border-*`), recortes y formas circulares. |
| 5 | Sombra, elevación y halo | `shadow-sm/md/xl/inner` y halos `0 0 Npx`; ausencia = elemento plano. |
| 6 | Tipografía | Tamaño (sp) y peso (400/600/700) de cada texto; familia = fuente del sistema (P21). |
| 7 | Estados | Normal, pulsado/activo, **deshabilitado** (visible y distinguible) y vacío/carga/error. |
| 8 | Textos e idioma | Cadenas exactas; paridad ES/EN. |
| 9 | Iconos | Icono/símbolo correcto y su tamaño. |
| 10 | Animación | Pulsos, rebotes y transiciones del prototipo. |

## Método de comparación

El modelo puede no ingerir imágenes; la comparación debe ser objetiva y reproducible:

1. **Estructura por código:** lee `uxdocs/<pantalla>/code.html` y el/los archivo(s) Compose de la pantalla; empareja cada clase Tailwind del prototipo con su equivalente (tamaño, color, radio, sombra, peso).
2. **Inventario de UI:** `adb shell uiautomator dump` durante el E2E; comprueba que existen los textos, iconos y controles del prototipo.
3. **Muestreo de píxeles:** sobre las capturas, verifica los colores clave contra los tokens de `uxdocs/DESIGN.md` (permite afirmar igualdad de color sin «ver» la imagen).
4. **Capturas versionadas:** deja en `docs/evidence/<feature-id>/` **hasta 2 capturas** de los estados clave (no una por pantalla) para la revisión humana final.

## Desviaciones permitidas

- Extensiones de la feature que el prototipo no tiene (p. ej. controles añadidos) y estados que el prototipo no cubre (vacío, carga, error, permisos), **solo si están declarados en la spec** («Impacto visual» o «Fuera de alcance»).
- Toda diferencia no declarada es un hallazgo.

## Severidad y veredicto

- **Bloqueante:** divergencia material que incumple el requisito no funcional de fidelidad de la Parte A (falta un bloque, tamaño/posición/color/forma/sombra/peso visiblemente distintos, control deshabilitado indistinguible, texto o idioma incorrectos, icono equivocado, animación ausente). Devuelve la feature a `in_progress`.
- **No bloqueante:** matiz de acabado no exigido por la Parte A (diferencias de pocos px/dp, sombra muy sutil no declarada). Se reporta sin bloquear.
- Aplica el mismo formato de feedback de bugs de la skill.

## Evidencia

En `docs/evidence/<feature-id>/`, junto a las capturas:

| Elemento del prototipo | Atributo | Implementación | Veredicto |
|---|---|---|---|
| `<clase/estructura en code.html>` | dimensión/color/sombra/peso… | `<composable/modificador>` | igual / divergencia (severidad) |

Cierra con el resultado: nº de atributos comparados, desviaciones declaradas aceptadas y divergencias abiertas con su severidad.
