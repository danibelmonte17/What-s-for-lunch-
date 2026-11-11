# Contexto del proyecto

Glosario y lenguaje compartido de **What's for Lunch?**. Cada término significa lo mismo para toda persona o agente que trabaje aquí, y se usa tal cual en documentación, textos de UI y comentarios. Los identificadores del código usan la equivalencia inglesa fijada en `docs/specs/revisar-identificadores-en-ingles.md`. Lo previsto o no confirmado lleva `⚠` (supuestos y riesgos: `docs/risks-and-open-questions.md`). El producto y su alcance están en `docs/build-brief.md`.

## Qué es

**What's for Lunch?** es una app **Android (Kotlin + Jetpack Compose)** que resuelve el «¿qué cocino hoy?» con los alimentos que ya hay en casa: se escanea la **Despensa**, la IA propone **Recetas** compatibles y se cocina en **Modo Cocina** manos libres. El **Pinche** añade dudas de cocina y recetas que no dependen de la Despensa. MVP de uso personal (1-5 usuarios), con datos locales, sin cuentas ni backend.

## Glosario

### Producto

**Comando de voz**: frase exacta que el Modo Cocina reconoce para actuar: «Siguiente», «Repetir» o «Pausar tiempo» (y «Next», «Repeat», «Pause timer» en inglés).
**Ingrediente**: componente de una Receta, con cantidad y unidad de lista cerrada (ud, g, kg, ml, l —`unificar-unidades`—; las unidades legadas guardadas se muestran como texto). Todos los Ingredientes cuentan en la Coincidencia y en los Faltantes: no hay obligatorios ni opcionales (`redisenar-formulario-de-receta`).
**Lista de la Compra**: ítems que faltan por comprar, agrupables por Pasillo o por Receta.
**Mi perfil**: apartado en su propia pestaña con las restricciones dietéticas del usuario —**Alergias** e **Intolerancias o enfermedades que limitan la dieta** como chips con «No tengo ninguna», y **Tipo de alimentación** en selección única— que el Pinche usa en sus propuestas y **rellena por chat antes de proponer** cuando faltan partes: una pregunta a la vez y cada respuesta se guarda con `save_profile` (RD25/RD26/RD27). Son datos de salud que solo salen del dispositivo al chatear con el Pinche (`RK03`).
**Mi recetario**: Recetas del usuario (creadas a mano, aceptadas desde una Sugerencia o guardadas desde el Pinche) y pantalla de entrada de la app (antes «Mis Recetas»).
**Modo Cocina**: guía de una Receta paso a paso, manos libres, con temporizadores y voz.
**Momento culinario**: clasificación de una Receta en desayuno, almuerzo, cena o postre.
**Paso**: instrucción secuencial de una Receta, con temporizador opcional y con los Ingredientes que usa (vínculo opcional; un Ingrediente puede usarse en varios Pasos).
**Pinche**: asistente conversacional de cocina con IA. Responde dudas de cocina y puede proponer recetas con independencia de lo que haya en la Despensa; si «Mi perfil» está incompleto, lo rellena en el chat —una pregunta a la vez— antes de proponer (RD26/RD27); no sustituye a las Sugerencias de la Despensa.
**Propuesta del Pinche**: receta que devuelve el Pinche dentro de una conversación, con texto, ingredientes y pasos. No es una Receta ni una Sugerencia: solo pasa a ser Receta si el usuario la guarda desde su Ficha.
**Favorita**: Receta marcada como favorita por el usuario; Mi recetario puede filtrarlas con el chip «Favoritas», combinable con la búsqueda y el Momento culinario.
**Ficha**: vista dentro del Pinche con el texto, los ingredientes, los pasos y los Faltantes de una Propuesta del Pinche. Desde ella se guarda la Propuesta en Mi recetario.
**Raciones**: número de comensales de una Receta; ajusta las cantidades (entre 1 y 12).
**Receta**: plato con ingredientes, pasos y metadatos; se crea a mano, se acepta desde una Sugerencia o se guarda desde el Pinche.
**Voz**: función opcional del Modo Cocina que locuta los Pasos y escucha Comandos de voz con los servicios del sistema (`TextToSpeech` y `SpeechRecognizer`); apagada por defecto, solo en memoria y sin escucha en segundo plano (RD24).
**What's for Lunch?**: la app.

### Despensa y compra

**Alimento**: cada producto detectado o añadido a la Despensa (p. ej. «Tomate fresco»).
**Coincidencia**: porcentaje de ingredientes de una Receta presentes en la Despensa (todos cuentan: sin obligatorios ni opcionales, `redisenar-formulario-de-receta`).
**Despensa**: todos los alimentos del hogar en un único listado. No hay desglose por ubicaciones físicas. Es independiente de la Lista de la Compra: la Despensa es lo que hay y la Lista es lo que falta por comprar.
**Faltante**: Ingrediente de una Receta o de una Propuesta del Pinche que no está en la Despensa. Todos los Ingredientes pueden ser Faltantes (`redisenar-formulario-de-receta`).
**Escaneo**: captura con la cámara que detecta Alimentos y los propone para la Despensa.
**Escáner**: pantalla y flujo donde se escanea la Despensa para obtener Sugerencias.
**Origen**: trazabilidad de un ítem de la Lista de la Compra («Para: Receta X» o «Despensa baja»).
**Pasillo**: categoría de supermercado para agrupar la Lista de la Compra (Verduras & Frescos, Carnes & Pescados, Lácteos, Despensa & Aceites).
**Sugerencia**: Receta propuesta por la IA a partir de la Despensa; al aceptarla se guarda en Mi recetario.

### Asistencia y comportamiento

**Modo de degradación**: comportamiento ante un fallo de la IA o de la red: el flujo no se bloquea. Donde hay entrada manual (Escáner) se ofrece; en el Pinche se avisa y se ofrece «Reintentar».

## Términos rechazados o ambiguos

**ChefEnCasa** → usar **What's for Lunch?**. Es el nombre del PRD de origen (`chefencasa_prd_y_project_brief_completo.md`) y de las carpetas de `uxdocs/`; el nombre oficial del producto es What's for Lunch? (`P1`).
**nevera** (como inventario), **inventario** → usar **Despensa**. No confundir con **Escaneo**, que es la forma de llenarla con la cámara.
**colección**, **catálogo** → como nombre, usar **Mi recetario**. «Catálogo» sugiere además contenido compartido entre usuarios, que está fuera del MVP (`P32`).
**Pinche IA**, **chatbot**, **chat** (como pantalla) → usar **Pinche**.
**card**, **tarjeta de receta** (en el Pinche) → usar **Ficha**.
**receta del chat**, **sugerencia del chat** → usar **Propuesta del Pinche**; **Sugerencia** queda reservado a las de la Despensa.
**match** → usar **Coincidencia**. No confundir con el badge «85% Match» de la interfaz, que es el umbral mínimo de sugerencia (RD02) y no un sinónimo de Coincidencia.
**Escaneo** y **Escáner** no son sinónimos: **Escaneo** es una captura concreta de la cámara; **Escáner** es la pantalla y el flujo que contiene escaneos y termina en Sugerencias.
**Borrador**, **Publicada**, **publicar** (como transición de estado) → retirados con `eliminar-estado-de-borrador-y-filtrar-favoritos`: las Recetas ya no tienen estado y el formulario guarda con un único botón («Guardar»/«Guardar cambios»).
