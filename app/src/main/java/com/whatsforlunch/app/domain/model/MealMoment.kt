package com.whatsforlunch.app.domain.model

// Momento culinario de una Receta (docs/domain-model.md, CONTEXT.md).
// Los cuatro valores del dominio: Desayuno, Almuerzo, Cena y Postre.
enum class MealMoment {
    BREAKFAST,
    LUNCH,
    DINNER,
    DESSERT,
}
