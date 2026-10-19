# Contexto del proyecto

Glosario y lenguaje compartido de **What's for Lunch?**. Cada término significa lo mismo para toda persona o agente que trabaje aquí, y los documentos, specs y el código los usan tal cual. Lo previsto o no confirmado lleva `⚠` (supuestos y riesgos: `docs/risks-and-open-questions.md`). El producto y su alcance están en `docs/build-brief.md`.

## Qué es

**What's for Lunch?** es una app **Android (Kotlin + Jetpack Compose)** que resuelve el «¿qué cocino hoy?» con los alimentos que ya hay en casa: se escanea la **Despensa**, la IA propone **Recetas** compatibles y se cocina en **Modo Cocina** manos libres. MVP de uso personal (1-5 usuarios), con datos locales, sin cuentas ni backend.

## Glosario

### Producto

**Borrador**: Receta guardada que aún no está lista para cocinar; al publicarla queda disponible en Mis Recetas.
**Ingrediente**: componente de una Receta, con cantidad, unidad y si es obligatorio u opcional.
**Lista de la Compra**: ítems que faltan por comprar, agrupables por Pasillo o por Receta.
**Mis Recetas**: Recetas del usuario (creadas a mano o aceptadas desde una Sugerencia) y pantalla de entrada de la app.
**Modo Cocina**: guía de una Receta paso a paso, manos libres, con temporizadores y voz.
**Momento culinario**: clasificación de una Receta en desayuno, almuerzo, cena o postre.
**Paso**: instrucción secuencial de una Receta, con temporizador opcional.
**Pinche**: asistente conversacional de cocina con IA.
**Raciones**: número de comensales de una Receta; ajusta las cantidades (entre 1 y 12).
**Receta**: plato con ingredientes, pasos y metadatos; se crea a mano o se acepta desde una Sugerencia.
**What's for Lunch?**: la app.

### Despensa y compra

**Alimento**: cada producto detectado o añadido a la Despensa (p. ej. «Tomate fresco»).
**Coincidencia**: porcentaje de ingredientes obligatorios de una Receta presentes en la Despensa.
**Despensa**: todos los alimentos del hogar en un único listado. No hay desglose por ubicaciones físicas.
**Escaneo**: captura con la cámara que detecta Alimentos y los propone para la Despensa.
**Escáner**: pantalla y flujo donde se escanea la Despensa para obtener Sugerencias.
**Origen**: trazabilidad de un ítem de la Lista de la Compra («Para: Receta X» o «Despensa baja»).
**Pasillo**: categoría de supermercado para agrupar la Lista de la Compra (Verduras & Frescos, Carnes & Pescados, Lácteos, Despensa & Aceites).
**Sugerencia**: Receta propuesta por la IA a partir de la Despensa; al aceptarla se guarda en Mis Recetas.

### Asistencia y comportamiento

**Modo de degradación**: comportamiento ante un fallo de la IA o de la red: el flujo continúa con entrada manual (sin bloquear).

## Términos rechazados o ambiguos

**ChefEnCasa** → usar **What's for Lunch?**. Es el nombre del PRD de origen (`chefencasa_prd_y_project_brief_completo.md`) y de las carpetas de `uxdocs/`; el nombre oficial del producto es What's for Lunch? (`P1`).
**nevera** (como inventario), **inventario** → usar **Despensa**. No confundir con **Escaneo**, que es la forma de llenarla con la cámara.
**colección**, **catálogo** → usar **Mis Recetas**. «Catálogo» sugiere además contenido compartido entre usuarios, que está fuera del MVP (`P32`).
**Pinche IA**, **chatbot** → usar **Pinche**.
**match** → usar **Coincidencia**. No confundir con el badge «85% Match» de la interfaz, que es el umbral mínimo de sugerencia (RD02) y no un sinónimo de Coincidencia.
**Escaneo** y **Escáner** no son sinónimos: **Escaneo** es una captura concreta de la cámara; **Escáner** es la pantalla y el flujo que contiene escaneos y termina en Sugerencias.
