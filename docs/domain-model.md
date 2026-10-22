# Modelo de dominio

Reglas del negocio. Términos definidos en `CONTEXT.md`. Sin tecnología: las decisiones de implementación viven en `docs/technical-discovery.md`. Los IDs `RD##` son estables y las specs los citan como fuente única de las reglas.

## Conceptos centrales

| Entidad | Qué representa | Atributos clave | Relaciones |
|---|---|---|---|
| Receta | Un plato que se puede cocinar | título, historia, Momento culinario, tiempo preparación, tiempo cocción, dificultad, calorías por porción, Raciones base, foto, favorito, estado (Borrador/Publicada), origen (Usuario/Sugerencia) | N Ingrediente (vía IngredienteReceta), N Paso |
| Ingrediente | Producto de cocina reutilizable entre Recetas | nombre, unidad, Pasillo | N Receta |
| IngredienteReceta | Cantidad de un Ingrediente en una Receta | cantidad, unidad, obligatorio (sí/no) | Receta, Ingrediente |
| Paso | Instrucción secuencial de una Receta | orden, texto, temporizador (opcional) | Receta |
| Alimento | Producto disponible en la Despensa | nombre, cantidad (entero 0..99; 0 = sin cantidad) | detectado en Escaneo |
| Escaneo | Detección continua de la cámara (visor en vivo) | fecha, Alimentos detectados con nombre, confianza y cantidad propuesta (entera 0..99; 0 = sin cantidad) | propone Alimentos para la Despensa |
| Sugerencia | Receta propuesta por la IA desde la Despensa | Coincidencia, estado (propuesta/aceptada/descartada) | se materializa en una Receta al aceptarse |
| Lista de la Compra | Conjunto de ítems pendientes de comprar | — | N ÍtemLista |
| ÍtemLista | Artículo a comprar | nombre, cantidad, Pasillo, Origen, completado | Lista de la Compra, Receta (opcional, vía Origen) |

Las Recetas aceptadas desde una Sugerencia y las creadas a mano conviven en Mis Recetas; se distinguen por el atributo `origen` (`P6`, `P32`).

## Relaciones

- Receta 1—N IngredienteReceta N—1 Ingrediente; Receta 1—N Paso. Borrar una Receta borra en cascada sus IngredienteReceta y Pasos.
- Escaneo propone N Alimento para la Despensa; los duplicados dentro del propio Escaneo (mismo nombre normalizado) se fusionan en un solo chip, y una coincidencia con un Alimento ya presente en la Despensa **suma su cantidad a la existente** al guardar (RD13), sin preguntar; en ambos casos queda un solo Alimento por nombre normalizado. La detección es **continua sobre el visor**: cada Alimento entra una sola vez por nombre normalizado, su confianza pasa a ser la **máxima observada** (una detección posterior más floja no la reduce) y nunca se sobrescriben las ediciones del usuario (nombre y cantidad); un Alimento ya añadido **no se elimina** aunque deje de verse, solo el usuario lo quita. Si el usuario renombra un chip y el detector vuelve a proponer la etiqueta original, se crea un Alimento nuevo (ya no comparte nombre normalizado) y puede fusionarse a mano.
- Sugerencia se materializa como máximo en una Receta (al aceptarse).
- ÍtemLista referencia opcionalmente a una Receta vía su Origen.

## Estados y ciclos de vida

### Receta
- `Borrador` → `Publicada`: el usuario pulsa «Publicar Receta» con título, foto y al menos 1 Ingrediente y 1 Paso válidos, y sin filas de Ingrediente ni Pasos inválidos (`⚠ S07`, Q-crear-receta-1).
- `Publicada` → `Borrador`: no hay vuelta atrás en el MVP; editar una Receta publicada la mantiene publicada.
- Cualquier estado → eliminada: solo con confirmación explícita y de forma definitiva (RD04).

### Sugerencia
- `propuesta` → `aceptada`: crea una Receta en Mis Recetas con `origen = Sugerencia` (RD07).
- `propuesta` → `descartada`: no se guarda nada.

### ÍtemLista
- `pendiente` → `completado`: el usuario marca el checkbox.
- `completado` → `pendiente`: se puede desmarcar.

Transiciones prohibidas: ninguna Receta se elimina sin confirmación (RD04); ninguna Sugerencia se acepta dos veces (la segunda aceptación no crea otra Receta).

## Reglas de negocio

- **RD01**: La Coincidencia se calcula solo sobre los ingredientes obligatorios: `% = obligatorios disponibles / obligatorios totales`.
- **RD02**: Una Receta se sugiere si su Coincidencia es ≥ 85%; el badge «100% Match» se muestra cuando todos los obligatorios están en la Despensa.
- **RD03**: Las cantidades escalan linealmente con las Raciones: `cantidad = cantidad base × (raciones / raciones base)`, con Raciones entre 1 y 12.
- **RD04**: Eliminar una Receta requiere confirmación y es definitivo (sin papelera ni deshacer).
- **RD05**: Ante un fallo de la IA o de la red, el flujo continúa en Modo de degradación con entrada manual (chips), sin bloquear al usuario.
- **RD06**: Las fotos del Escaneo no se almacenan: se procesan y se descartan (RGPD).
- **RD07**: Una Sugerencia aceptada crea una Receta en Mis Recetas con `origen = Sugerencia`.
- **RD09**: Cada ÍtemLista conserva su Origen («Para: Receta X» o «Despensa baja»).
- **RD10**: Los ingredientes opcionales no afectan a la Coincidencia.
- **RD11**: La Despensa no tiene desglose por ubicaciones físicas.
- **RD12**: El Escaneo solo propone detecciones de Alimentos con confianza ≥ 0.60. Umbral adoptado por decisión del usuario para EfficientDet-Lite2 (2026-10-21); los chips editables permiten corregir falsos positivos y RD05 conserva la entrada manual ante degradación.
- **RD13**: Al guardar un Escaneo, la cantidad de cada chip se **suma** a la del Alimento existente con el mismo nombre normalizado, conservando su id y su nombre y sin crear fila nueva. «Sin cantidad» cuenta como 0 (`null + N → N`, `M + 0 → M`, `null + 0 → sin cantidad`) y la suma se recorta al máximo de 99 sin aviso. Los chips sin coincidencia crean Alimentos nuevos como hasta ahora. No hay diálogo ni decisión de «Mantener cantidad»/«Actualizar cantidad».

## Cálculos y fórmulas

- **Coincidencia**: obligatorios disponibles / obligatorios totales. Ej.: Receta con 4 obligatorios y 3 en la Despensa → 75% → no se sugiere (umbral 85%). Con 4 de 4 → 100% → badge «100% Match».
- **Escalado de Raciones**: base 4 Raciones, se cocinan 2 → cada cantidad se divide entre 2. Ej.: 200 g → 100 g. Límites: 1-12.
- **Tiempo total de una Receta**: tiempo de preparación + tiempo de cocción.

## Validaciones de datos

- Raciones: entero entre 1 y 12 (RD03).
- Cantidades de IngredienteReceta: mayores que 0.
- Título de la Receta: obligatorio.
- Publicar una Receta: título y foto obligatorios, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila de Ingrediente ni Paso inválidos (Q-crear-receta-1, que revisa `⚠ S07`: la foto pasa a ser obligatoria).
- Nombre de un ÍtemLista o Alimento: no vacío; los duplicados (mismo nombre normalizado) se fusionan en un solo Alimento (dentro de un Escaneo, en un solo chip). Al renombrar un chip hacia un duplicado, gana el chip editado (nombre/cantidad conservados, confianza máxima); al intentar añadir manualmente un duplicado, se conserva el chip ya existente. En el alta y edición manual de Alimentos de la Despensa, un duplicado se rechaza con aviso y no se crea fila nueva (Q-gestionar-alimentos-de-la-despensa-3).
- Cantidad de un Alimento: entero entre 0 y 99; 0 equivale a sin cantidad (revisión 2026-10-21: máximo 99 en toda la app).
- Cantidad propuesta de un Alimento en el Escaneo: misma regla que la de un Alimento —entero 0..99, 0 = sin cantidad— editada con un stepper igual que en la Despensa; al guardar se suma a la del Alimento existente con el mismo nombre normalizado (RD13) o se conserva como cantidad del Alimento nuevo si no hay coincidencia.
- Confianza de detección: valor entre 0 y 1; solo detecciones ≥ 0.60 del vocabulario alimentario COCO del modelo seleccionado pueden crear chips detectados. Los Alimentos añadidos manualmente tienen `confidence = null`.

## Escenarios importantes

1. **Escaneo → Sugerencia → Receta**: el usuario escanea la Despensa, confirma los chips de Alimentos detectados, recibe Sugerencias compatibles (RD02), acepta una y queda en Mis Recetas con `origen = Sugerencia` (RD07).
2. **Alta manual → cocina**: el usuario crea una Receta en el Formulario, la guarda como Borrador o la Publica (⚠ S07), la abre en el Detalle, ajusta Raciones (RD03) y arranca el Modo Cocina.
3. **Faltantes → Lista de la Compra**: desde el Detalle o tras un Escaneo, lo que falta acaba como ÍtemLista con su Origen (RD09); la compra se agrupa por Pasillo o por Receta.
4. **Fallo de IA o red**: Escáner o Pinche degradan a entrada manual con aviso (RD05) y el flujo continúa.

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Escaneo sin detecciones | Mensaje claro y alta manual de Alimentos por chips |
| Fallo de la IA o de la red (escáner o Pinche) | Modo de degradación: entrada manual y aviso (RD05) |
| Ingrediente detectado duplicado | Dentro del Escaneo se fusiona en un solo chip; si su nombre normalizado coincide con un Alimento ya presente en la Despensa, al guardar se suman las cantidades (RD13) y no se crea fila nueva |
| Raciones fuera de límite | El escalador no permite valores fuera de 1-12 |
| Edición concurrente desde dos dispositivos | No aplica: datos locales, un solo Usuario (P31) |
| Receta sin foto o sin ingredientes suficientes | Sin título, foto, 1 Ingrediente y 1 Paso válidos no se puede Publicar (Q-crear-receta-1, `⚠ S07`) |
| Lista de la Compra vacía | Estado vacío con CTA para escanear la Despensa o añadir ítems |
| Colección de Recetas vacía | Estado vacío con CTA para crear una Receta o escanear la Despensa |

## Roles y acceso

Un único rol en el MVP: el **Usuario** propietario del dispositivo. No hay invitados, administradores ni cuentas compartidas (`P7`, `P31`).

| Acción | Usuario |
|---|---|
| Crear, editar y eliminar Recetas | ✅ |
| Escanear la Despensa y aceptar/descartar Sugerencias | ✅ |
| Usar el Modo Cocina y el Pinche | ✅ |
| Gestionar la Lista de la Compra | ✅ |
| Ver Recetas de otros usuarios | ❌ (fuera de alcance: ver `docs/build-brief.md`) |

No hay acceso que conceder, revocar ni que caduque: sin cuentas ni sesión en red, la única «revocación» posible es borrar los datos del dispositivo (o la app). La API de IA usa la key del propio usuario (`⚠ S05`), no un acceso de la app.

## Historial y auditoría

No se conserva historial de cambios: solo importa el estado actual (uso personal, sin cuentas). Los Escaneos no dejan rastro de imágenes (RD06); sus resultados viven solo hasta confirmar los chips.
