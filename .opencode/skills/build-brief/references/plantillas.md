# Plantillas de documentos

Contenido: Convenciones comunes · 1. CONTEXT.md · 2. docs/build-brief.md · 3. docs/domain-model.md · 4. docs/technical-discovery.md · 5. DESIGN.md · 6. docs/risks-and-open-questions.md

Adapta cada plantilla al proyecto concreto. Elimina las secciones que no apliquen en lugar de dejarlas vacías, y no dejes marcadores `<...>` sin rellenar. `feature_list.json`, `PROGRESS.md`, `docs/progress-history.md`, `AGENTS.md` y los scripts del harness (`init`/`lint`/`unit`/`e2e`) no se generan aquí: son responsabilidad de la skill `harness-starter`, que se ejecuta después usando estos documentos como entrada.

---

## Convenciones comunes

- **IDs estables**, para poder referirse a un elemento sin copiarlo: `P##` pregunta abierta (la numeración del banco), `S##` supuesto, `RK##` riesgo, `RD##` regla de dominio.
- **Marcas en línea**: si un contenido depende de un supuesto o de una pregunta abierta, añade `⚠ S03` o `⚠ P12` justo a su lado.
- **Vocabulario**: los términos de `CONTEXT.md` se escriben exactamente como allí; los alias de «Evitar» no se usan en ningún documento.
- **Idioma**: documentación y specs en el del usuario; el código en inglés. Los nombres de archivo no cambian.

---

## 1. CONTEXT.md (raíz)

Contexto persistente del agente. El harness lo carga antes de ejecutar tareas para que el agente conozca el proyecto sin que el usuario repita en cada prompt qué hace cada módulo, cómo está organizada la aplicación, qué patrones se usan, cómo se compila y se valida, y qué convenciones seguir.

Dos reglas lo hacen útil. Es **breve y estable**: se lee entero en cada tarea, así que cada línea debe ganarse su sitio, y no lleva instrucciones puntuales de una tarea. Y **enlaza en vez de copiar**: el detalle vive en `docs/`. «Términos» define palabras, no reglas: lo que es verdad entre ellas va en `docs/domain-model.md`.

```markdown
# Contexto del proyecto

Contexto persistente para el agente: se carga antes de cada tarea. El detalle está en `docs/`. Lo previsto o no confirmado lleva `⚠`.

## Qué es
<Una o dos frases: qué hace el proyecto y para quién.>

## Términos
Lenguaje común: cada término significa lo mismo para toda persona o agente que trabaje aquí. Los documentos y el código usan estos términos tal cual.

**<Término>**: <qué es, en una frase; no cómo se implementa>.
_Evitar_: <alias que no se usan>. _No confundir con_: <término cercano pero distinto>.

**Variante**: combinación concreta de atributos (talla, color) con la que se vende un Producto; tiene su propio stock.
_Evitar_: versión, modelo. _No confundir con_: Producto, que agrupa todas sus variantes.

## Arquitectura
- `<carpeta o módulo>/`: <qué hace>
- `<carpeta o módulo>/`: <qué hace>
Detalle y flujos: `docs/technical-discovery.md`.

## Dependencias y patrones
- <framework o librería clave y para qué se usa>
- <patrón obligatorio, p. ej. corrutinas para operaciones asíncronas>

## Convenciones
- <estilo de código, nombres y estructura de commits; p. ej. documentación en el idioma del usuario y código en inglés>
- <regla de dependencias, p. ej. la lógica de negocio no depende de la UI>

## Comandos
- Arrancar (instala y ejecuta): `<comando>` (`./init.sh` o `init.ps1` si existen)
- Compilar: `<comando>`
- Tests unitarios: `<comando>` (`./unit.sh` si existe)
- Lint: `<comando>` (`./lint.sh` si existe)
- Instrumentados/E2E: `<comando>` (`./e2e.sh` si existe)

## Validación antes de dar algo por terminado
1. `lint` y `unit` en verde (y el build que exija el stack).
2. `e2e` en verde: <verificación de extremo a extremo de la funcionalidad: herramienta y cómo ejecutarla>.
3. Si existe `feature_list.json`, actualizar el `status` de la feature solo después de lo anterior.

## Reglas de trabajo
- Prohibido: <p. ej. comandos destructivos fuera del directorio del proyecto; escribir credenciales, tokens o PII en el repositorio, logs o documentos; modificar o eliminar entradas de `feature_list.json` salvo `status`>.
- Pedir confirmación humana antes de: <acciones sensibles>.
- Si la tarea toca algo marcado `⚠` en `docs/risks-and-open-questions.md`, preguntar en vez de asumir.
- Si un cambio altera estructura, comandos, convenciones o términos, actualizar este archivo en el mismo cambio.

## Qué consultar y qué evitar
- Consultar según la tarea: `docs/build-brief.md` (qué sí y qué no se construye), `docs/domain-model.md` (reglas de negocio), `DESIGN.md` (interfaz), `docs/technical-discovery.md` (decisiones técnicas y su estado), `docs/risks-and-open-questions.md` (lo que aún no está decidido).
- Evitar: <carpetas generadas, código legado, ficheros con secretos>.
```

«Términos»: orden alfabético; si pasa de unos 15, agrúpalos por área bajo `###`. «Evitar» y «No confundir con» son opcionales: úsalos cuando el usuario haya mostrado solapamiento o ambigüedad real. En un proyecto nuevo, «Arquitectura» y «Comandos» describen lo previsto y llevan `⚠ S##`; en uno existente, se comprueban en el repositorio.

---

## 2. docs/build-brief.md

Qué construimos y, con el mismo peso, qué no. Es el primer documento que lee quien llega nuevo. No incluye tecnología (eso es `technical-discovery.md`) ni reglas de negocio (eso es `domain-model.md`).

```markdown
# Build brief

## Producto
<Una o dos frases: qué es.>

## Problema que resuelve
<Qué dolor o necesidad concreta cubre.>

## Usuarios objetivo
<Quiénes son y en qué situación lo usan.>

## Objetivo principal
<Qué resultado debe conseguir el usuario; una frase.>

## Qué construimos (MVP)
- <funcionalidad> — <valor para el usuario, en pocas palabras>

## Qué NO construimos
### Fuera del MVP (puede venir después)
- <elemento> — <motivo>

### Fuera del producto (no es su propósito)
- <elemento> — <motivo>

## Restricciones relevantes
- <plazo, plataforma, normativa, presupuesto, integraciones obligatorias>

## Criterios de éxito
- <cómo sabremos que el producto cumple su propósito>

Supuestos, riesgos y preguntas abiertas: ver `risks-and-open-questions.md`.
```

---

## 3. docs/domain-model.md

Las reglas del dominio, independientes de la tecnología: nada de tablas SQL, frameworks ni endpoints. Cada regla es una afirmación verificable, con ID, para que una prueba o un agente pueda citarla.

```markdown
# Modelo de dominio

Reglas del negocio. Términos definidos en `CONTEXT.md`.

## Entidades y relaciones
| Entidad | Qué representa | Atributos clave | Relaciones |
|---|---|---|---|

<Opcional: diagrama Mermaid `erDiagram` si ayuda a ver las relaciones.>

## Ciclo de vida
### <Entidad>
- `<estado A>` → `<estado B>`: <condición y quién puede ejecutarla>
- Transiciones prohibidas: <cuáles y por qué>

## Reglas de negocio
- **RD01**: <regla en una frase afirmativa y verificable>.
- **RD02**: <regla>.

## Cálculos y fórmulas
- <nombre>: <fórmula o criterio, con un ejemplo numérico>

## Validaciones de datos
- <campo o entidad>: <regla específica del negocio>

## Roles y permisos de negocio
| Acción | <Rol A> | <Rol B> |
|---|---|---|

## Casos límite
| Situación | Comportamiento esperado |
|---|---|
| <entrada vacía, duplicada, sobre el límite, edición concurrente, fallo de un servicio externo…> | <qué debe pasar> |

## Historial y auditoría
<Qué cambios se conservan y cuáles solo importan en su estado actual.>
```

---

## 4. docs/technical-discovery.md

Stack **candidato**, no decisiones cerradas. Cada elección lleva su estado para que nadie construya sobre algo que nadie validó. Si hay repositorio o documentación previos, cita de dónde sale cada dato.

```markdown
# Descubrimiento técnico

Exploración inicial. Nada de lo siguiente está decidido salvo lo marcado como Confirmado.

## Punto de partida
<Stack, convenciones e integraciones ya existentes o impuestas por el usuario, con el archivo de origen. Si es un proyecto desde cero, dilo.>

## Stack candidato
| Capa | Candidato | Motivo | Alternativas | Estado |
|---|---|---|---|---|

Estado: **Confirmado** (lo fijó el usuario o ya existe en el repo) · **Candidato** (propuesta razonada) · **Por validar** (requiere una prueba antes de comprometerse).

## Arquitectura y flujos
<Por qué esta división en módulos y cómo fluyen los datos; diagrama Mermaid opcional. La lista de carpetas y módulos vive en `CONTEXT.md`.>

## Integraciones externas
- <sistema o API> — <para qué, y restricciones conocidas>

## Datos y persistencia
- Datos duraderos frente a temporales/derivados: <…>
- Migración o importación: <…>

## Requisitos no funcionales
- Rendimiento, escalabilidad, disponibilidad, internacionalización: <expectativas concretas>

## Seguridad y datos sensibles
- Datos sensibles/PII y normativa aplicable: <…>
- Gestión de credenciales y tokens (proxy, bóveda): <…>

## Operación y despliegue
- Dónde se despliega, quién lo mantiene, observabilidad: <…>

## Verificación
- Estrategia de pruebas y validación E2E: <herramienta, entorno, datos de prueba>. Los comandos y las validaciones previas a dar algo por terminado viven en `CONTEXT.md`.

## A validar antes de construir
- <spike o comprobación> (ver `RK##` / `P##`)
```

---

## 5. DESIGN.md (raíz)

Dirección inicial de UX/UI. Si existe material de referencia, **manda sobre cualquier propuesta**: se lista, se extrae de él lo que diga (paleta, tipografía, estructura de pantallas) citando el archivo, y lo que no cubre se anota como laguna. Sin material, la dirección es una propuesta mínima marcada con `⚠ S##`. Si ya existía un `DESIGN.md`, se amplía conservando su contenido.

```markdown
# Diseño e interfaz

Dirección inicial. Lo que procede de material de referencia cita su archivo; lo demás es propuesta.

## Material de referencia
| Archivo o enlace | Tipo | Qué cubre | Estatus | Qué no cubre |
|---|---|---|---|---|
| `<ruta>` | <DESIGN.md · HTML · imagen · Figma · otro> | <pantallas, tokens, flujos> | <Definitivo / Orientativo> | <lagunas> |

Estatus: **Definitivo** (respetar tal cual) · **Orientativo** (inspiración).
<Si no existe material: "No hay material de UX/UI. La dirección siguiente es una propuesta inicial." ⚠ S##>

## Principios de experiencia
- <principio y por qué importa para estos usuarios>

## Vistas y flujos principales
| Vista | Objetivo | Componentes clave | Referencia |
|---|---|---|---|
| <nombre> | <qué logra el usuario> | <…> | <archivo, o "sin referencia" ⚠ P##> |

## Lenguaje visual inicial
- Tipografía: <…>
- Paleta: <colores con su uso>
- Espaciado y forma: <…>
- Componentes y librería: <…>
- Tono: <sensación que transmite>

## Estados a cubrir
<Vacío, carga, error y éxito: qué se muestra en cada vista clave.>

## Dispositivos, accesibilidad e idioma
- <tamaños soportados, requisitos de accesibilidad, idiomas>

## Lagunas del material de referencia
- <qué falta o se contradice> → `P##`
```

---

## 6. docs/risks-and-open-questions.md

Lo que todavía no sabemos, a la vista. Es el **único** lugar donde viven las preguntas pendientes, los supuestos y los riesgos; el resto de documentos solo los marcan con `⚠` y su ID. Que el resumen inicial esté siempre actualizado.

```markdown
# Riesgos y preguntas abiertas

Todo lo que falta por saber o que hemos asumido sin confirmar. Si otro documento depende de algo de aquí, lo marca con `⚠` y el ID.

## Resumen
Preguntas abiertas: <n> · Supuestos: <n> · Riesgos: <n> · Bloquean la construcción: <n>

## Preguntas abiertas
Preguntas del banco de descubrimiento (Fase 2) que el usuario no llegó a responder. Cópialas **literalmente**, numeradas, agrupadas por categoría. No resumir ni recortar el texto de la pregunta.

### <Categoría>
- P07: ¿<pregunta literal>? — Afecta a: `<documento>` · Bloquea: <sí / no>

## Supuestos
Decisiones tomadas sin confirmación explícita, incluidas las tomadas para desbloquear preguntas pendientes.

| ID | Asumimos que… | Por qué | Si es falso… | Cómo confirmarlo |
|---|---|---|---|---|
| S01 | | | | |

## Riesgos
| ID | Riesgo | Probabilidad | Impacto | Mitigación o siguiente paso |
|---|---|---|---|---|
| RK01 | | | | |

## Decisiones a revisar
- <decisión que tomó el agente y que el usuario debería validar> — <por qué importa>
```
