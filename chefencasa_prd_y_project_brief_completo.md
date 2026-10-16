# Product Requirements Document (PRD) & Project Brief: ChefEnCasa

**Versión:** 1.0  
**Fecha:** Octubre 2023 / Actualizado  
**Estado:** Aprobado / En Fase de Implementación  
**Autor:** Equipo de Producto & Arquitectura de Software UX/UI  
**Público objetivo:** Desarrolladores Full Stack, Diseñadores UX/UI, Product Managers y Stakeholders  

---

## 1. Resumen Ejecutivo (Executive Summary)

**ChefEnCasa** es una aplicación móvil y web (PWA) de asistencia culinaria inteligente orientada a transformar ingredientes cotidianos disponibles en el hogar en recetas saludables, sabrosas y fáciles de preparar. 

A través de la combinación sinérgica de **visión computacional (Edge AI)** para el reconocimiento automático de alimentos en la nevera/despensa, un **motor de compatibilidad SQL** de cero desperdicio (*zero-waste*), un **asistente conversacional interactivo (Pinche IA)** con dictado por voz y un **Modo Cocina Paso a Paso manos libres con temporizadores interactivos**, ChefEnCasa resuelve la frustración diaria de "¿Qué cocino hoy?", reduciendo el desperdicio alimentario y el tiempo de planificación semanal.

---

## 2. Declaración del Problema & Propuesta de Valor

### 2.1. El Problema
1. **Fatiga de decisión culinaria:** El usuario promedio gasta más de 20 minutos diarios decidiendo qué cocinar con ingredientes dispersos.
2. **Desperdicio de comida (Food Waste):** Más del 30% de los alimentos frescos se desechan por falta de planificación o caducidad inadvertida.
3. **Fricción durante el cocinado:** Consultar pantallas táctiles con manos sucias o mojadas arruina la fluidez y provoca fallos en tiempos y cocciones.
4. **Desconexión entre recetas e inventario:** Los recetarios tradicionales requieren ingredientes que el usuario no tiene, obligando a listas de compra manuales e inconexas.

### 2.2. La Solución ChefEnCasa
- **Escanear & Cocinar en 5 segundos:** Apunta la cámara a la nevera y obtén recetas con coincidencia del 100% o 85% de ingredientes disponibles.
- **Asistencia manos libres:** Guía paso a paso con fuentes de gran tamaño legibles a distancia, comandos por voz y temporizadores automáticos integrados.
- **Ciclo cerrado Nevera-Receta-Compra:** Si falta un ingrediente clave, se añade a la *Lista de la Compra Sincronizada* clasificado por pasillo de supermercado con un solo toque.

---

## 3. Objetivos del Producto & Métricas de Éxito (OKRs & KPIs)

| Objetivo Estratégico | Métrica Clave (KPI) | Meta (Q1-Q2) |
|---|---|---|
| Reducción del desperdicio en el hogar | Tasa de recetas completadas con >80% de ingredientes en stock | > 65% de las sesiones |
| Adopción del flujo guiado | Inicio y finalización del *Modo Cocina Paso a Paso* | > 50% de las recetas vistas |
| Precisión de la Visión IA | Confianza media de detección en escáner de nevera | > 88% de acierto validado |
| Retención y engagement semanal | Usuarios activos semanales (WAU) con lista sincronizada | > 40% de retención a 30 días |

---

## 4. User Personas

### Persona 1: Carlos Mendoza (El Profesional Ocupado)
- **Perfil:** 32 años, trabaja en remoto, poco tiempo al mediodía para cocinar.
- **Frustración:** Abre la nevera llena de restos (medio pimiento, huevos, queso) y termina pidiendo comida rápida a domicilio.
- **Momento clave con ChefEnCasa:** Hace un escaneo de 3 segundos de su nevera, el motor SQL le recomienda *Tortilla de Cebolla Pochada* o *Shakshuka Express*, activa el modo manos libres y cocina en 20 minutos.

### Persona 2: Lucía & Marc (La Pareja Eficiente y Sostenible)
- **Perfil:** 28 y 30 años, concienciados con el medio ambiente y el ahorro doméstico.
- **Frustración:** Compran comida duplicada y tiran verduras pochas al final de la semana.
- **Momento clave con ChefEnCasa:** Consultan la lista sincronizada de compra agrupada por pasillos antes de salir del trabajo; preparan comidas ajustando el stepper de raciones a 2 comensales.

---

## 5. Alcance Funcional y Módulos del Sistema (Scope)

El ecosistema de ChefEnCasa se estructura en **7 pantallas y módulos de alta fidelidad** ya definidos e interconectados:

### Módulo 1: Catálogo de Recetas & Gestión CRUD (`ChefEnCasa - Mis Recetas y CRUD`)
- Buscador reactivo con debounce y soporte para términos culinarios e ingredientes.
- Filtro por momento del día: *Desayuno, Almuerzo, Cena, Postre*.
- Tarjetas con tiempo de preparación, dificultad, badge de disponibilidad y acciones de edición/eliminación.
- Botón de acción rápida para creación de receta.

### Módulo 2: Escáner de Nevera con IA (`ChefEnCasa - Escáner de Nevera IA`)
- Visor de cámara WebRTC a 60 FPS optimizado (640x640px).
- HUD de Bounding Boxes dinámicas con etiquetas y porcentaje de confianza (*YOLOv8-Food / Multimodal*).
- Chips editables para agregar/remover ingredientes detectados manualmente.
- Carrusel de recetas clasificadas por orden de coincidencia SQL (*100% Match*, *85% Match*).

### Módulo 3: Detalle y Lectura de Receta (`ChefEnCasa - Detalle de Receta`)
- Hero visual inmersivo con badge de stock en nevera (*"Nevera: 100%"*).
- Métricas rápidas: Tiempo total (prep + cocción), dificultad, calorías por porción y coste casero estimado.
- **Escalador dinámico de raciones:** Selector `[-] N pers [+]` con recálculo en vivo de cantidades requeridas.
- Checklist de ingredientes por ubicación de despensa (*nevera, despensa, vinagrera, especiero*).
- Utensilios necesarios, pasos resumidos y CTA destacado para activar el Modo Cocina.

### Módulo 4: Modo Cocina Paso a Paso Manos Libres (`ChefEnCasa - Modo Cocina Paso a Paso`)
- Modo UI de alto contraste y tipografía extragrande, legible a 1,5 metros de distancia.
- Barra de avance secuencial (*Paso X de Y*).
- Temporizador circular interactivo con cuenta atrás, alarma sonora y botón rápido `+1 min`.
- Integración con **Web Speech Synthesis** (locución de instrucciones) y **Web Speech Recognition** (comandos: "Siguiente", "Repetir", "Pausar tiempo").
- Drawer colapsable con los ingredientes necesarios para el paso activo.

### Módulo 5: Pinche Asistente de Cocina IA (`ChefEnCasa - Pinche Asistente de Cocina`)
- Interfaz conversacional con chips de sugerencias rápidas (*"¿Qué cocino con huevos y tomate?"*, *"Sustituto de nata"*).
- Respuestas estructuradas con cards ejecutables de recetas y acceso directo al Modo Cocina.
- Soporte para transcripción de audio / dictado por voz.

### Módulo 6: Modal Formulario de Registro & Edición (`ChefEnCasa - Registrar y Editar Receta`)
- Formulario modal estructurado con selector de cámara / IA generativa para fotos de platos.
- Metadatos: Título, breve historia, momento culinario, tiempo, nivel y raciones.
- Gestor dinámico de ingredientes con botón de escaneo IA, unidades y selector de ingrediente opcional.
- Definición de pasos secuenciales con temporizadores específicos en segundos/minutos.
- Guardado dual: *Borrador* (`is_draft = true`) y *Publicar Receta*.

### Módulo 7: Lista de la Compra Sincronizada (`ChefEnCasa - Lista de la Compra Sincronizada`)
- Notificación interactiva por detección automática de ingredientes faltantes tras escaneos o visualización de recetas.
- Agrupación configurable en tabs: **Por Pasillo de Supermercado** (*Verduras & Frescos, Carnes & Pescados, Lácteos, Despensa & Aceites*) o **Por Receta**.
- Trazabilidad de origen en cada ítem (*badge "Para: Receta X"* o *"Despensa baja"*).
- Barra de progreso de cesta completada con checkboxes táctiles para el carrito físico.
- Entrada de texto y dictado rápido por voz.

---

## 6. Arquitectura Técnica y Stack Tecnológico

```
┌────────────────────────────────────────────────────────────────────────┐
│                   Frontend (PWA / Responsive Mobile)                   │
│  - Framework: React 18 / Vite / TypeScript                             │
│  - Styling: Tailwind CSS (Tokens Warm Culinary Craft)                  │
│  - Hardware APIS: WebRTC MediaStream, Web Speech API (TTS & STT)       │
│  - Icons & UI: Lucide React, Radix UI Primitives                       │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │ HTTPS / REST                    │ WSS / WebSocket
                   ▼                                 ▼
┌────────────────────────────────────┐  ┌────────────────────────────────┐
│            Backend API             │  │        Servicios de IA         │
│  - Python FastAPI / Node.js        │  │  - YOLOv8-Food ONNX (Edge/Web) │
│  - Autenticación JWT / OAuth2      │  │  - Gemini 1.5 Flash / Claude   │
│  - CRUD & Endpoints de sincronía   │  │  - Motor de Matching SQL       │
└──────────────────┬─────────────────┘  └────────────────┬───────────────┘
                   │                                     │
                   ▼                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Capa de Datos Persistente (PostgreSQL 15+)               │
│  - Tablas: users, recipes, ingredients, recipe_ingredients,            │
│            recipe_steps, fridge_scans, shopping_list_items             │
│  - Búsqueda Full-Text con tsvector / GIN Indexes                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Requisitos No Funcionales (NFRs)

1. **Rendimiento (Performance):**
   - Tiempo de carga inicial (FCP) < 1.2 segundos en redes móviles 4G.
   - Tiempo de inferencia de visión en cliente < 800 ms.
2. **Usabilidad y Accesibilidad (a11y):**
   - Contraste WCAG 2.1 AA mínimo en toda la aplicación; WCAG AAA en el *Modo Cocina*.
   - Áreas de pulsación táctil mínimas de 48x48px para facilitar el uso mientras se cocina.
3. **Disponibilidad Offline:**
   - La PWA debe permitir la lectura de recetas guardadas y el funcionamiento de los temporizadores sin conexión a Internet mediante Service Workers e IndexedDB.
4. **Privacidad y Seguridad:**
   - Cifrado en tránsito TLS 1.3. Las imágenes escaneadas de la nevera se procesan en memoria o en cliente sin almacenar rostros ni metadatos de ubicación personal.

---

## 8. Hoja de Ruta (Roadmap) y Fases de Despliegue

- **Fase 1 (Completada):** Especificación técnica, arquitectura relacional SQL y diseño UI de alta fidelidad de las 7 pantallas móviles principales.
- **Fase 2 (Próxima):** Implementación del frontend React PWA y cableado con base de datos PostgreSQL y endpoints de mock API.
- **Fase 3:** Integración del modelo ONNX WebRTC de visión por computador para detección en vivo de verduras y lácteos.
- **Fase 4:** Beta cerrada con usuarios reales para medición de reducción de desperdicio y calibración del asistente Pinche IA por voz.
