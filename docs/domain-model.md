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
| Escaneo | Foto analizada por la IA desde el visor de la cámara | Alimentos propuestos con nombre y cantidad propuesta (entera 0..99; 0 = sin cantidad) | propone Alimentos para la Despensa |
| Sugerencia | Receta completa propuesta por la IA a partir de los Alimentos que el usuario elige enviar | título, historia, Momento culinario, tiempos, dificultad, calorías, Raciones base, Ingredientes, Pasos, Coincidencia, estado (propuesta/aceptada/descartada) | se materializa en una Receta al aceptarse |
| Lista de la Compra | Conjunto de ítems pendientes de comprar | — | N ÍtemLista |
| ÍtemLista | Artículo a comprar | nombre, cantidad, Pasillo, Origen, completado | Lista de la Compra, Receta (opcional, vía Origen) |

Las Recetas aceptadas desde una Sugerencia y las creadas a mano conviven en Mis Recetas; se distinguen por el atributo `origen` (`P6`, `P32`).

## Relaciones

- Receta 1—N IngredienteReceta N—1 Ingrediente; Receta 1—N Paso. Borrar una Receta borra en cascada sus IngredienteReceta y Pasos.
- Escaneo propone N Alimento para la Despensa; los duplicados dentro de la propia respuesta de la IA (mismo nombre normalizado) se fusionan en un solo chip, y una coincidencia con un Alimento ya presente en la Despensa **suma su cantidad a la existente** al guardar (RD13), sin preguntar; en ambos casos queda un solo Alimento por nombre normalizado. Cada foto se analiza por la IA y se **fusiona con los chips actuales** (D1): los alimentos nuevos entran con su cantidad propuesta y los chips existentes conservan el nombre y la cantidad editados por el usuario; una segunda foto nunca pisa esas ediciones. Un Alimento ya añadido solo se elimina cuando el usuario lo quita o descarta el Escaneo. Si el usuario renombra un chip y una foto posterior vuelve a proponer el nombre original, se crea un Alimento nuevo (ya no comparte nombre normalizado) y puede fusionarse a mano.
- Sugerencia se materializa como máximo en una Receta (al aceptarse).
- ÍtemLista referencia opcionalmente a una Receta vía su Origen.

## Estados y ciclos de vida

### Receta
- `Borrador` → `Publicada`: el usuario pulsa «Publicar Receta» con título, foto y al menos 1 Ingrediente y 1 Paso válidos, y sin filas de Ingrediente ni Pasos inválidos (`⚠ S07`, Q-crear-receta-1).
- `Publicada` → `Borrador`: no hay vuelta atrás en el MVP; editar una Receta publicada la mantiene publicada.
- Cualquier estado → eliminada: solo con confirmación explícita y de forma definitiva (RD04).

### Sugerencia
- Se pide desde el Escáner con una **selección de Alimentos** (chips del Escaneo sin guardar + Despensa, un ítem por nombre normalizado); la IA devuelve hasta 3 propuestas completas en JSON compacto y la app las muestra con su Coincidencia (RD01/RD10) calculada en el cliente contra esos Alimentos seleccionados. El prompt pide además no proponer recetas iguales ni equivalentes a las Recetas de Mis Recetas con Coincidencia > 60 % con esos Alimentos.
- La Sugerencia vive **solo en memoria** (tarjeta con acciones, estados `propuesta`/`aceptada`/`descartada`) y se pierde al salir; el cuerpo de la tarjeta sigue siendo inerte.
- `propuesta` → `aceptada`: crea una Receta **Borrador** sin foto en Mis Recetas, con `origen = Sugerencia` (RD07) e **id estable** derivado del título normalizado. Se muestra el aviso «Guardada en Mis Recetas», la tarjeta queda «Aceptada» con sus acciones deshabilitadas y el usuario permanece en la lista.
- `propuesta` → `descartada`: no se guarda nada, la tarjeta queda «Descartada» con sus acciones deshabilitadas y sigue visible.
- Cada lista nueva de propuestas (nueva petición o reintento) empieza con todas en `propuesta`.

### ÍtemLista
- `pendiente` → `completado`: el usuario marca el checkbox.
- `completado` → `pendiente`: se puede desmarcar.

Transiciones prohibidas: ninguna Receta se elimina sin confirmación (RD04); ninguna Sugerencia se acepta dos veces (la segunda aceptación reescribe la misma Receta por id estable y upsert, nunca crea otra).

## Reglas de negocio

- **RD01**: La Coincidencia se calcula solo sobre los ingredientes obligatorios: `% = obligatorios disponibles / obligatorios totales`.
- **RD02**: Una Receta se sugiere si su Coincidencia es ≥ 85%; el badge «100% Match» se muestra cuando todos los obligatorios están en la Despensa.
- **RD03**: Las cantidades escalan linealmente con las Raciones: `cantidad = cantidad base × (raciones / raciones base)`, con Raciones entre 1 y 12. La cantidad escalada se muestra redondeada a un máximo de 1 decimal (`Q-escalar-raciones-1`); con Raciones iguales a las base se conserva la precisión original.
- **RD04**: Eliminar una Receta requiere confirmación y es definitivo (sin papelera ni deshacer).
- **RD05**: Ante un fallo de la IA o de la red, el flujo continúa en Modo de degradación con entrada manual (chips), sin bloquear al usuario.
- **RD06**: Las fotos del Escaneo no se almacenan en el dispositivo: se capturan en memoria, se envían a DevExpert Inference para su análisis y se descartan al terminar (RGPD; el envío al proveedor se rige por `P27`/`RK03`). No hay ficheros, MediaStore ni tabla de Escaneos.
- **RD07**: Una Sugerencia aceptada crea una Receta en Mis Recetas con `origen = Sugerencia`.
- **RD09**: Cada ÍtemLista conserva su Origen («Para: Receta X» o «Despensa baja»).
- **RD10**: Los ingredientes opcionales no afectan a la Coincidencia.
- **RD11**: La Despensa no tiene desglose por ubicaciones físicas.
- **RD12**: *(Derogada por `escanear-despensa-con-ia`.)* La detección on-device con umbral de confianza ≥ 0.60 se retiró junto con MediaPipe y el HUD. La IA de visión no devuelve confianza: todos los Alimentos propuestos entran como chips editables y RD05 conserva la entrada manual ante degradación. Se mantiene el ID para no renumerar el resto de reglas.
- **RD13**: Al guardar un Escaneo, la cantidad de cada chip se **suma** a la del Alimento existente con el mismo nombre normalizado, conservando su id y su nombre y sin crear fila nueva. «Sin cantidad» cuenta como 0 (`null + N → N`, `M + 0 → M`, `null + 0 → sin cantidad`) y la suma se recorta al máximo de 99 sin aviso. Los chips sin coincidencia crean Alimentos nuevos como hasta ahora. No hay diálogo ni decisión de «Mantener cantidad»/«Actualizar cantidad».

## Cálculos y fórmulas

- **Coincidencia**: obligatorios disponibles / obligatorios totales. Ej.: Receta con 4 obligatorios y 3 en la Despensa → 75% → no se sugiere (umbral 85%). Con 4 de 4 → 100% → badge «100% Match».
- **Escalado de Raciones**: base 4 Raciones, se cocinan 2 → cada cantidad se divide entre 2. Ej.: 200 g → 100 g. Límites: 1-12. La cantidad escalada se muestra redondeada a máximo 1 decimal (1 × 3/4 = 0,75 → 0,8; `Q-escalar-raciones-1`); cuando las Raciones coinciden con las base se muestra la cantidad guardada con su precisión original. El escalado es solo de visualización y no cambia las cantidades base guardadas.
- **Tiempo total de una Receta**: tiempo de preparación + tiempo de cocción.

## Validaciones de datos

- Raciones: entero entre 1 y 12 (RD03).
- Cantidades de IngredienteReceta: mayores que 0.
- Título de la Receta: obligatorio.
- Publicar una Receta: título y foto obligatorios, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila de Ingrediente ni Paso inválidos (Q-crear-receta-1, que revisa `⚠ S07`: la foto pasa a ser obligatoria).
- Nombre de un ÍtemLista o Alimento: no vacío; los duplicados (mismo nombre normalizado) se fusionan en un solo Alimento (dentro de una respuesta de la IA, en un solo chip). Al renombrar un chip hacia un duplicado, gana el chip editado (nombre/cantidad conservados); al intentar añadir manualmente un duplicado, se conserva el chip ya existente. En el alta y edición manual de Alimentos de la Despensa, un duplicado se rechaza con aviso y no se crea fila nueva (Q-gestionar-alimentos-de-la-despensa-3).
- Cantidad de un Alimento: entero entre 0 y 99; 0 equivale a sin cantidad (revisión 2026-10-21: máximo 99 en toda la app).
- Cantidad propuesta de un Alimento en el Escaneo: misma regla que la de un Alimento —entero 0..99, 0 = sin cantidad— editada con un stepper igual que en la Despensa; al guardar se suma a la del Alimento existente con el mismo nombre normalizado (RD13) o se conserva como cantidad del Alimento nuevo si no hay coincidencia. Las propuestas de la IA se normalizan (D2): cantidad no entera, ausente o ≤ 0 → 0 («sin cantidad») y > 99 → 99; el Alimento nunca se descarta por su cantidad, solo si su nombre queda vacío.
- ~~Confianza de detección~~: retirada con RD12; la IA de visión no devuelve confianza y los chips no muestran porcentaje.
- Propuesta de Sugerencia (D8 de `sugerir-recetas-con-ia`): se descarta si el título queda vacío, si no le quedan Ingredientes o Pasos tras limpiar, o si repite el título normalizado de otra propuesta (se conserva la primera); se listan como máximo 3. Cantidad de ingrediente no numérica o ≤ 0 → `1` (el ingrediente nunca se descarta por su cantidad); unidad recortada (puede quedar vacía); `opcional` distinto de booleano → obligatorio; momento o dificultad con token desconocido → Almuerzo / Media; tiempos de preparación/cocción no numéricos o < 0 → `0`; Raciones base fuera de 1..12 → `4`; calorías no numéricas o ≤ 0 → sin valor; temporizador de paso no numérico o < 0 → sin temporizador.
- Selección de Alimentos para Sugerencias: todos preseleccionados, mínimo 1 para pedir; la selección y las propuestas no se persisten (solo memoria, sin Room ni caché).
- Aceptación de una Sugerencia: la Receta destino es un Borrador con `photoPath = null`, `favorite = false`, `origen = Sugerencia`, los Ingredientes y Pasos de la propuesta (con cantidad, unidad, opcionalidad y temporizador) e ids de hijos `"$id-ingredient-$index"` / `"$id-step-$index"`. El id de la Receta es `UUID.nameUUIDFromBytes("suggestion:" + normalizeName(título))`, de modo que aceptar dos veces la misma propuesta reescribe la misma fila. Descartar no guarda nada.
- Anti-duplicados del prompt (D2 de `aceptar-sugerencia-de-receta`): solo entran las Recetas de Mis Recetas (Borrador o Publicada, cualquier origen) cuya Coincidencia con los Alimentos seleccionados es **estrictamente mayor que 60 %** (porcentaje redondeado, misma base que RD02). Las Recetas sin ingredientes obligatorios no tienen Coincidencia y quedan fuera; con 60 % exacto tampoco se envían.

## Escenarios importantes

1. **Escaneo → Sugerencia → Receta**: el usuario hace una foto de la Despensa, revisa los chips de Alimentos propuestos por la IA, recibe Sugerencias compatibles (RD02), acepta una y queda en Mis Recetas con `origen = Sugerencia` (RD07).
2. **Alta manual → cocina**: el usuario crea una Receta en el Formulario, la guarda como Borrador o la Publica (⚠ S07), la abre en el Detalle, ajusta Raciones (RD03) y arranca el Modo Cocina.
3. **Faltantes → Lista de la Compra**: desde el Detalle o tras un Escaneo, lo que falta acaba como ÍtemLista con su Origen (RD09); la compra se agrupa por Pasillo o por Receta.
4. **Fallo de IA o red**: Escáner o Pinche degradan a entrada manual con aviso (RD05) y el flujo continúa.

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Escaneo sin alimentos reconocidos | Mensaje claro y alta manual de Alimentos por chips |
| Fallo de la IA o de la red (escáner o Pinche) | Modo de degradación: entrada manual y aviso (RD05) |
| Fallo de la IA en Sugerencias | Aviso mínimo con «Reintentar»; la degradación manual completa (RD05) vive en el Escáner (`degradar-ante-fallo-de-ia`) |
| Ningún Alimento seleccionado en Sugerencias | «Pedir sugerencias» deshabilitado; no se llama a la IA |
| Ninguna propuesta de Sugerencia válida | Estado vacío con «Reintentar» |
| Chips del Escaneo y Despensa con el mismo Alimento | Un solo ítem en la selección por nombre normalizado |
| Salir de Sugerencias | La selección y las propuestas se pierden (solo memoria); el Escáner conserva sus chips sin guardar. Las Recetas aceptadas permanecen en Mis Recetas |
| Sugerencia aceptada y luego editada | El estado «Aceptada» deshabilita las acciones; si se forzara otra aceptación, reescribe la Receta por id estable (upsert) |
| Receta existente con Coincidencia exactamente 60 % | No se envía al prompt anti-duplicados (umbral estrictamente `> 60`) |
| Ingrediente propuesto duplicado | Dentro de la respuesta de la IA se fusiona en un solo chip; si su nombre normalizado coincide con un Alimento ya presente en la Despensa, al guardar se suman las cantidades (RD13) y no se crea fila nueva |
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
