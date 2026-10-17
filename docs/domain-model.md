# Modelo de dominio

Reglas del negocio. Términos definidos en `CONTEXT.md`. Sin tecnología: las decisiones de implementación viven en `docs/technical-discovery.md`.

## Entidades y relaciones

| Entidad | Qué representa | Atributos clave | Relaciones |
|---|---|---|---|
| Receta | Un plato que se puede cocinar | título, historia, Momento culinario, tiempo preparación, tiempo cocción, dificultad, calorías por porción, Raciones base, foto, favorito, estado (Borrador/Publicada), origen (Usuario/Sugerencia) | N Ingrediente (vía IngredienteReceta), N Paso |
| Ingrediente | Producto de cocina reutilizable entre Recetas | nombre, unidad, Pasillo | N Receta |
| IngredienteReceta | Cantidad de un Ingrediente en una Receta | cantidad, unidad, obligatorio (sí/no) | Receta, Ingrediente |
| Paso | Instrucción secuencial de una Receta | orden, texto, temporizador (opcional) | Receta |
| Alimento | Producto disponible en la Despensa | nombre, cantidad (opcional) | detectado en Escaneo |
| Escaneo | Captura de la cámara y detección asociada | fecha, Alimentos detectados con confianza | propone Alimentos para la Despensa |
| Sugerencia | Receta propuesta por la IA desde la Despensa | Coincidencia, estado (propuesta/aceptada/descartada) | se materializa en una Receta al aceptarse |
| Lista de la Compra | Conjunto de ítems pendientes de comprar | — | N ÍtemLista |
| ÍtemLista | Artículo a comprar | nombre, cantidad, Pasillo, Origen, completado | Lista de la Compra, Receta (opcional, vía Origen) |

Las Recetas aceptadas desde una Sugerencia y las creadas a mano conviven en Mis Recetas; se distinguen por el atributo `origen` (`P6`, `P32`).

## Ciclo de vida

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

## Cálculos y fórmulas

- **Coincidencia**: obligatorios disponibles / obligatorios totales. Ej.: Receta con 4 obligatorios y 3 en la Despensa → 75% → no se sugiere (umbral 85%). Con 4 de 4 → 100% → badge «100% Match».
- **Escalado de Raciones**: base 4 Raciones, se cocinan 2 → cada cantidad se divide entre 2. Ej.: 200 g → 100 g. Límites: 1-12.
- **Tiempo total de una Receta**: tiempo de preparación + tiempo de cocción.

## Validaciones de datos

- Raciones: entero entre 1 y 12 (RD03).
- Cantidades de IngredienteReceta: mayores que 0.
- Título de la Receta: obligatorio.
- Publicar una Receta: título y foto obligatorios, al menos 1 Ingrediente y 1 Paso válidos y ninguna fila de Ingrediente ni Paso inválidos (Q-crear-receta-1, que revisa `⚠ S07`: la foto pasa a ser obligatoria).
- Nombre de un ÍtemLista o Alimento: no vacío; los duplicados (mismo nombre normalizado) se fusionan.

## Roles y permisos de negocio

Un único rol en el MVP: el **Usuario** propietario del dispositivo. No hay invitados, administradores ni cuentas compartidas (`P7`, `P31`).

| Acción | Usuario |
|---|---|
| Crear, editar y eliminar Recetas | ✅ |
| Escanear la Despensa y aceptar/descartar Sugerencias | ✅ |
| Usar el Modo Cocina y el Pinche | ✅ |
| Gestionar la Lista de la Compra | ✅ |
| Ver Recetas de otros usuarios | ❌ (fuera de alcance: ver `docs/build-brief.md`) |

## Casos límite

| Situación | Comportamiento esperado |
|---|---|
| Escaneo sin detecciones | Mensaje claro y alta manual de Alimentos por chips |
| Fallo de la IA o de la red (escáner o Pinche) | Modo de degradación: entrada manual y aviso (RD05) |
| Ingrediente detectado duplicado | Se fusiona en un solo Alimento |
| Raciones fuera de límite | El escalador no permite valores fuera de 1-12 |
| Edición concurrente desde dos dispositivos | No aplica: datos locales, un solo Usuario (P31) |
| Receta sin foto o sin ingredientes suficientes | Sin título, foto, 1 Ingrediente y 1 Paso válidos no se puede Publicar (Q-crear-receta-1, `⚠ S07`) |
| Lista de la Compra vacía | Estado vacío con CTA para escanear la Despensa o añadir ítems |
| Colección de Recetas vacía | Estado vacío con CTA para crear una Receta o escanear la Despensa |

## Historial y auditoría

No se conserva historial de cambios: solo importa el estado actual (uso personal, sin cuentas). Los Escaneos no dejan rastro de imágenes (RD06); sus resultados viven solo hasta confirmar los chips.
