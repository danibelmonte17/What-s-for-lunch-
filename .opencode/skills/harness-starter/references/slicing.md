# Granularidad de las funcionalidades

Una funcionalidad es un *slice* del tamaño de una sesión: ni un épico ni una tarea de bajo nivel. Representa un comportamiento visible para el usuario o verificable por el sistema, plausiblemente completable y verificable en una sesión de agente enfocada.

Regla de granularidad de Learn Harness Engineering: demasiado amplia no terminará; demasiado fina crea sobrecarga de gestión. Prefiere `El alumno puede solicitar un email de acceso con enlace mágico` a un épico como `API de enlace mágico y matriculación`.

## Pruebas de slicing

Aplícalas a cada candidata antes de escribir `feature_list.json`:

- Si el título une varios dominios con "y", "/" o comas, divídela.
- Si para verificarla harían falta varias pantallas, endpoints, jobs o integraciones independientes, divídela.
- Si no se puede verificar con un pequeño escenario más los checks del repo (`lint`/`unit`/`e2e`), divídela.
- Contraprueba: si una candidata no tiene comportamiento observable por sí sola (p. ej. "crear el modelo User"), es una tarea de bajo nivel; intégrala en la funcionalidad que la necesita.

## Cuántas esperar

- Un MVP no trivial con menos de 10 funcionalidades es una señal de alarma: haz otra pasada de slicing, o explica en la sesión 0 de `docs/progress-history.md` por qué el proyecto es genuinamente tan pequeño.
- Un MVP de producto con autenticación, roles, contenido o datos, flujos de usuario e integraciones debería dar unas 12-30 funcionalidades del tamaño de una sesión, no 5-8 hitos amplios.
- Es una señal, no una cuota. La segunda pasada divide lo que ya está en el MVP; nunca añade alcance para llegar a un número.

## Ids

El `id` es un nombre único que una persona entiende sin abrir la funcionalidad: minúsculas y guiones, verbo más objeto, como `registrar-cuenta`, `solicitar-enlace-acceso` o `rechazar-registro-duplicado`. No uses números ni prefijos tipo `F001`. Es estable: no se renombra, porque `depends_on` y `PROGRESS.md` lo referencian.

## Estado y orden

- Todas nacen `not_started`. Como máximo una puede estar `in_progress`, y solo si el repo ya tenía trabajo activo.
- `depends_on` expresa los prerrequisitos de ejecución. El orden de la lista es solo el plan preferido cuando varias están listas. No añadas campos numéricos de prioridad.
