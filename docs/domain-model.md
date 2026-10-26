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
| Propuesta del Pinche | Receta que devuelve el Pinche en una conversación, sin depender de la Despensa | texto de la respuesta, título, historia, Momento culinario, tiempos, dificultad, calorías, Raciones base, Ingredientes, Pasos, Faltantes | se materializa en una Receta solo al guardarse desde su Ficha |
| Lista de la Compra | Conjunto de ítems pendientes de comprar | — | N ÍtemLista |
| ÍtemLista | Artículo a comprar | nombre, cantidad, Pasillo, Origen, completado | Lista de la Compra, Receta (opcional, vía Origen) |

Las Recetas aceptadas desde una Sugerencia y las creadas a mano conviven en Mis Recetas; se distinguen por el atributo `origen` (`P6`, `P32`).

## Relaciones

- Receta 1—N IngredienteReceta N—1 Ingrediente; Receta 1—N Paso. Borrar una Receta borra en cascada sus IngredienteReceta y Pasos.
- Escaneo propone N Alimento para la Despensa; los duplicados dentro de la propia respuesta de la IA (mismo nombre normalizado) se fusionan en un solo chip, y una coincidencia con un Alimento ya presente en la Despensa **suma su cantidad a la existente** al guardar (RD13), sin preguntar; en ambos casos queda un solo Alimento por nombre normalizado. Cada foto se analiza por la IA y se **fusiona con los chips actuales** (D1): los alimentos nuevos entran con su cantidad propuesta y los chips existentes conservan el nombre y la cantidad editados por el usuario; una segunda foto nunca pisa esas ediciones. Un Alimento ya añadido solo se elimina cuando el usuario lo quita o descarta el Escaneo. Si el usuario renombra un chip y una foto posterior vuelve a proponer el nombre original, se crea un Alimento nuevo (ya no comparte nombre normalizado) y puede fusionarse a mano.
- Sugerencia se materializa como máximo en una Receta (al aceptarse).
- Propuesta del Pinche se materializa como máximo en una Receta (al guardarse); si no se guarda, desaparece con la conversación.
- La Despensa y la Lista de la Compra son independientes: un Alimento no se convierte en ÍtemLista al agotarse ni al revés. Los Faltantes de una Propuesta solo se muestran; convertirlos en ÍtemLista es de `anadir-faltantes-a-la-lista`.
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

### Propuesta del Pinche
- Vive dentro de la conversación, **solo en memoria**: al salir del Pinche se pierde, igual que la conversación. No se persiste ni pasa a Mis Recetas por sí sola.
- `mostrada` → `guardada`: el usuario pulsa «Guardar en Mis Recetas» en su Ficha; se crea un Borrador con `origen = Sugerencia` e id estable derivado del título normalizado (RD16). La Ficha queda marcada como guardada y el usuario permanece en el chat.
- `mostrada` → descartada: el usuario cierra la Ficha o sigue conversando sin guardar; no se conserva nada.
- No tiene estados `aceptada`/`descartada` ni Coincidencia de Sugerencia (RD14); puede mostrar sus Faltantes (RD15).

Transiciones prohibidas: ninguna Receta se elimina sin confirmación (RD04); ninguna Sugerencia se acepta dos veces (la segunda aceptación reescribe la misma Receta por id estable y upsert, nunca crea otra); ninguna Propuesta del Pinche se guarda sin acción explícita del usuario.

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
- **RD14**: El Pinche puede proponer recetas **con independencia de la Despensa**. La Coincidencia y el umbral de RD02 no se aplican a las Propuestas del Pinche: ninguna Propuesta se descarta por no alcanzar el 85 % ni por tener Faltantes.
- **RD15**: Un **Faltante** es un Ingrediente **obligatorio** de la Propuesta del Pinche que no está en la Despensa por nombre normalizado; los opcionales no son Faltantes (RD10). Se calcula en el dispositivo al mostrar la Ficha; el conjunto se congela con la Despensa del momento de recibir la respuesta (guardar más tarde no lo recalcula).
- **RD16**: Guardar una Propuesta del Pinche crea una Receta **Borrador** sin foto, con `origen = Sugerencia` e id estable `UUID.nameUUIDFromBytes("pinche:" + normalizeName(título))`; guardar dos veces la misma Propuesta reescribe la misma Receta (upsert) y nunca duplica.
- **RD17**: El Pinche solo **lee** datos mediante dos funciones expuestas al modelo (Despensa y Mis Recetas); no escribe Recetas, Alimentos ni ÍtemLista. La conversación y las Propuestas no se persisten. Si el proveedor no admite function calling, se muestra un fallo con «Reintentar» y no se envía contexto por otra vía.
- **Implementación del Pinche (`resolver-dudas-con-pinche`, 2026-10-26)**: `get_pantry` devuelve nombre y cantidad (máximo 100 Alimentos); `get_my_recipes` devuelve título, Momento culinario e ingredientes (máximo 50 Recetas y 20 nombres por Receta, sin pasos, foto ni estado). El contexto se limita a 20 mensajes y 500 caracteres por mensaje del usuario. Cada turno admite **máximo 3 llamadas HTTP** (inicial + 2 rondas de tools) sin reintentos automáticos; agotarlo es un turno fallido. La respuesta final viaja sin `response_format`: texto y, si procede, un bloque cercado `receta` con `{"s":[{…}]}`; un JSON inválido deja solo el texto. Tool desconocida → `{"error":"unknown_function"}`; excepción de la tool → `{"error":"unavailable"}`.

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
- Consulta del Pinche: un mensaje vacío o solo con espacios no se envía ni llama a la IA. La conversación se envía completa en cada turno mientras dura la pantalla; al salir se descarta.
- Propuesta del Pinche: se normaliza con las mismas reglas que una Sugerencia (D8 de `sugerir-recetas-con-ia`), salvo que se admite **como máximo una Propuesta por respuesta**. Si el título queda vacío o no le quedan Ingredientes o Pasos, la respuesta se muestra solo como texto y sin Ficha.
- Ficha del Pinche: muestra ingredientes obligatorios y opcionales, pasos y la lista de Faltantes (solo obligatorios, RD15). Si la Despensa está vacía, todos los obligatorios son Faltantes.

## Escenarios importantes

1. **Escaneo → Sugerencia → Receta**: el usuario hace una foto de la Despensa, revisa los chips de Alimentos propuestos por la IA, recibe Sugerencias compatibles (RD02), acepta una y queda en Mis Recetas con `origen = Sugerencia` (RD07).
2. **Alta manual → cocina**: el usuario crea una Receta en el Formulario, la guarda como Borrador o la Publica (⚠ S07), la abre en el Detalle, ajusta Raciones (RD03) y arranca el Modo Cocina.
3. **Faltantes → Lista de la Compra**: desde el Detalle o tras un Escaneo, lo que falta acaba como ÍtemLista con su Origen (RD09); la compra se agrupa por Pasillo o por Receta.
4. **Fallo de IA o red**: Escáner o Pinche degradan a entrada manual con aviso (RD05) y el flujo continúa.
5. **Duda o receta libre en el Pinche**: el usuario pregunta por una sustitución o pide una receta sin tener los ingredientes; el Pinche pide a la app la Despensa y/o Mis Recetas solo si lo necesita (function calling), responde y puede devolver una Propuesta con su Ficha. El usuario la guarda como Borrador o la deja pasar; los Faltantes se muestran, no se compran todavía.

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Escaneo sin alimentos reconocidos | Mensaje claro y alta manual de Alimentos por chips |
| Fallo de la IA o de la red (escáner o Pinche) | Modo de degradación: entrada manual y aviso (RD05) |
| El proveedor de IA no admite function calling | Fallo visible con «Reintentar»; no se envía la Despensa ni las Recetas por otra vía (RD17) |
| Falta la API key al abrir el Pinche | Aviso con acceso a Ajustes; la conversación no se envía |
| Mensaje del Pinche vacío o solo espacios | No se envía ni se llama a la IA |
| Propuesta del Pinche con el mismo título que una Receta existente | La Ficha se muestra igualmente; guardarla crea una Receta nueva con id de Pinche (RD16), no fusiona con la existente. El modelo recibe Mis Recetas con `get_my_recipes` para no proponer duplicados |
| Propuesta del Pinche sin ingredientes o pasos válidos | Se muestra solo el texto, sin Ficha ni acción de guardar |
| Salir del Pinche | La conversación y las Propuestas se pierden; las Recetas guardadas permanecen en Mis Recetas |
| Despensa vacía en una Propuesta del Pinche | Todos los ingredientes obligatorios se muestran como Faltantes; la Propuesta no se bloquea (RD14) |
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
