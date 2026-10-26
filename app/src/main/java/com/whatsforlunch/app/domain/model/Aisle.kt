package com.whatsforlunch.app.domain.model

// Pasillo de un ÍtemLista (CONTEXT.md): las cuatro categorías de supermercado
// para agrupar la Lista de la Compra. El Pasillo ausente es `null` y se muestra
// como «Sin clasificar» (RD19). Identificadores en inglés y etiquetas visibles
// en strings.xml con paridad es/en (convención de MealMoment).
enum class Aisle {
    PRODUCE,
    MEAT_FISH,
    DAIRY,
    PANTRY_OILS,
}
