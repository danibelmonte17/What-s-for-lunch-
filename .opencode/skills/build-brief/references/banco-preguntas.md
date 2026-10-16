# Banco de preguntas de descubrimiento

Lista de categorías y preguntas candidatas para la Fase 2 (Descubrimiento exhaustivo por rondas). No es un cuestionario fijo: **selecciona y adapta** las preguntas relevantes para el proyecto concreto, reformúlalas con el vocabulario del usuario, y añade las que surjan del material de UX/UI aportado. Numera el resultado de forma continua (P1, P2, P3…) manteniendo el agrupado por categoría.

No hace falta usar todas; sí hace falta **revisar todas las categorías** antes de dar por cerrado el banco, para no dejar huecos obvios.

Cada categoría indica el documento al que alimenta (`→`). Sirve para saber dónde acabará la respuesta y, si queda sin responder, qué documento llevará la marca `⚠`.

---

## 1. Producto e intención → `docs/build-brief.md`
- ¿Qué problema concreto resuelve este producto/funcionalidad? ¿Qué pasa hoy sin él?
- ¿Quién es el usuario objetivo? ¿Hay más de un tipo de usuario (roles distintos)?
- ¿Cuál es el objetivo principal que debe cumplir el usuario al usarlo?
- ¿Cómo sabremos que el producto cumple su propósito (criterio de éxito medible)?
- ¿Es un producto nuevo, una funcionalidad sobre algo existente, o un rediseño?
- ¿Hay competidores o referencias que el usuario quiera imitar o evitar explícitamente?
- ¿Hay una fecha límite o evento que condicione el alcance?

## 2. Alcance del MVP → `docs/build-brief.md`
- ¿Qué funcionalidades son imprescindibles para la primera versión?
- ¿Qué queda explícitamente fuera del MVP (para no construirlo por error)?
- ¿Hay algo que parezca natural incluir, o que alguien pedirá, pero que el producto no debe hacer nunca?
- Si algo parece imprescindible pero es costoso, ¿hay una versión simplificada aceptable para el MVP?
- ¿Hay funcionalidades ya construidas (en otro sistema) que haya que igualar o sustituir?

## 3. Usuarios y roles → `docs/build-brief.md` (quiénes son) y `docs/domain-model.md` (qué puede hacer cada uno)
- ¿Cuántos roles de usuario existen (p. ej. admin, usuario normal, invitado)?
- ¿Qué puede ver/hacer cada rol de forma distinta?
- ¿Los usuarios se registran ellos mismos, los invita alguien, o se crean por otro medio (SSO, importación)?
- ¿Hace falta gestionar equipos, organizaciones o cuentas compartidas, o es un usuario individual?

## 4. Lenguaje y reglas del dominio → `CONTEXT.md` (lenguaje) y `docs/domain-model.md` (reglas)
Lenguaje común
- ¿Cuáles son los conceptos principales del negocio y qué palabra usa el usuario para cada uno?
- ¿Hay dos palabras que se usen para lo mismo (p. ej. «cliente» y «usuario»)? ¿Cuál es la oficial?
- ¿Hay alguna palabra que signifique cosas distintas según quién la use (p. ej. «variante», «cuenta»)?
- ¿Hay siglas o jerga del sector que alguien nuevo no entendería?

Reglas
- ¿Cómo se relacionan entre sí esos conceptos (uno a muchos, muchos a muchos…)?
- ¿Por qué estados pasa cada concepto principal y qué transiciones están permitidas o prohibidas?
- ¿Qué reglas de negocio nunca pueden violarse?
- ¿Hay cálculos o fórmulas del negocio (precios, impuestos, plazos, puntuaciones)?
- ¿Hay reglas de validación de datos específicas del negocio (más allá de "campo obligatorio")?
- ¿Hace falta historial o auditoría de cambios, o basta con el estado actual?
- ¿Hay excepciones que el negocio resuelve hoy "a mano"?

## 5. Funcional y casos límite → `docs/domain-model.md`
- Para cada funcionalidad principal: ¿qué pasa si la entrada está vacía, duplicada, o excede un límite razonable?
- ¿Qué ocurre si dos personas modifican lo mismo a la vez (concurrencia)?
- ¿Qué pasa si falla la red o un servicio externo a mitad de una operación?
- ¿Hay operaciones que deban poder deshacerse?
- ¿Hay límites de volumen esperados (número de usuarios, registros, tamaño de archivos)?
- ¿Qué pasa en los estados de carga, vacío (sin datos aún) y error en cada pantalla o endpoint clave?

## 6. UX / UI → `DESIGN.md`
- ¿Hay material de referencia de UX/UI (prototipo, wireframes, otro `DESIGN.md`, archivos HTML, imágenes, Figma)? ¿Dónde está? (Si sí: analizarlo y añadir el apartado 6.1. Lo que la inspección de la Fase 1 ya encontró no se pregunta.)
- ¿Qué dispositivos/tamaños de pantalla hay que soportar (móvil, escritorio, ambos)?
- ¿Hay una identidad visual o guía de estilo que seguir?
- ¿Hay un sistema de diseño o librería de componentes obligatorio?
- ¿Qué sensación debe transmitir la interfaz (p. ej. sobria, cercana, técnica)?
- ¿Qué idioma(s) debe soportar la interfaz?
- ¿Hay requisitos de accesibilidad conocidos?

### 6.1 Si hay material de referencia de UX/UI, añade además
- ¿Qué pantallas o flujos del producto **no** están representados en el material?
- Para cada pantalla del material: ¿qué pasa en sus estados vacío, de carga y de error, que normalmente no se dibujan?
- ¿Qué validaciones tiene cada campo o formulario del material?
- ¿Qué lógica de negocio hay detrás de cada acción visible (botones, formularios) que el material no explica?
- ¿El material es definitivo (hay que respetarlo tal cual) o es solo orientativo?
- Si hay varios materiales que se contradicen (p. ej. un HTML y un `DESIGN.md` distintos), ¿cuál manda?
- Si el material solo muestra un tamaño de pantalla, ¿cómo debe comportarse en los demás?
- ¿Hay interacciones o animaciones esperadas que el material estático no puede mostrar?

## 7. Técnico y datos → `docs/technical-discovery.md` (los patrones y módulos también a `CONTEXT.md`)
- ¿Hay stack tecnológico obligatorio (lenguaje, framework, base de datos) o es libre elección?
- ¿El producto debe integrarse con sistemas o APIs externas ya existentes?
- ¿Hay preferencia de arquitectura (monolito, servicios separados, serverless)?
- ¿En qué módulos debería dividirse la aplicación y qué responsabilidad tiene cada uno?
- ¿Hace falta trabajo offline o sincronización entre dispositivos?
- ¿Qué patrones de arquitectura o librerías clave usa ya el repositorio y hay que respetar?
- ¿Qué datos hay que persistir de forma duradera frente a los que son temporales/derivados?
- ¿Hay que migrar o importar datos de un sistema existente?

## 8. Convenciones y reglas de trabajo del agente → `CONTEXT.md`
Es lo que el agente debe saber antes de cada tarea para que el usuario no tenga que repetirlo en cada prompt. En un repositorio existente, la mayoría se responde inspeccionándolo.
- ¿Cuáles son los comandos para instalar dependencias, compilar y arrancar el proyecto?
- ¿Qué comprobaciones deben pasar antes de dar un cambio por terminado (tests, lint, tipos, E2E)?
- ¿Qué convenciones de código debe seguir el agente (estilo, nombres, estructura de commits)? ¿En qué idioma van la documentación y los identificadores del código?
- ¿Hay reglas de dependencia entre módulos (p. ej. la lógica de negocio no depende de la UI)?
- ¿Qué partes del proyecto debe consultar primero el agente?
- ¿Qué partes debe evitar o no tocar (código legado, archivos generados, ficheros con secretos)?
- ¿Hay acciones que el agente tenga prohibidas siempre (p. ej. comandos destructivos fuera del proyecto)?
- ¿Qué acciones requieren confirmación humana explícita antes de ejecutarse?

## 9. No funcional → `docs/technical-discovery.md`
- ¿Hay expectativas de rendimiento (tiempo de respuesta, usuarios concurrentes)?
- ¿Hay que planear para crecer mucho (escalabilidad) o es un uso acotado?
- ¿Hay restricciones de disponibilidad (uptime) relevantes?
- ¿Hay requisitos de internacionalización más allá del idioma (formatos de fecha, moneda, zona horaria)?

## 10. Seguridad y permisos → `docs/technical-discovery.md`
- ¿Qué datos son sensibles o personales (PII) y cómo deben protegerse?
- ¿Hay normativa aplicable (RGPD, HIPAA, PCI u otra) que condicione el diseño?
- ¿Cómo se gestionan las credenciales y tokens de servicios externos (proxy, bóveda de secretos)?

## 11. Operación y despliegue → `docs/technical-discovery.md`
- ¿Dónde se va a desplegar (proveedor, entorno propio, on-premise)? ¿Hay restricciones de región/residencia de datos?
- ¿Quién mantiene el sistema una vez construido (el propio usuario, un equipo, nadie)?
- ¿Hace falta observabilidad (logs, métricas, alertas) desde el principio o puede añadirse después?
- ¿Con qué entorno y datos de prueba se ejecutan los tests y la validación E2E?

## 12. Restricciones y criterios de éxito del producto → `docs/build-brief.md`
- ¿Hay un presupuesto o límite de coste (infraestructura, licencias) a respetar?
- ¿Hay restricciones legales, contractuales o de marca?
- Más allá del MVP funcional, ¿qué convertiría este proyecto en un éxito o en un fracaso desde el punto de vista del usuario?
