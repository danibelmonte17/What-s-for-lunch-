package com.whatsforlunch.app.domain.model

// Tipo de alimentación del perfil (`crear-mi-perfil`, D2): selección única entre
// Omnívora, Pescetariana, Vegetariana, Vegana y Cetogénica, **sin preselección**
// inicial (un perfil nuevo lo tiene en `null` y cuenta como pendiente, D5).
// Identificadores en inglés y etiquetas visibles en strings.xml con paridad es/en
// (convención de `MealMoment`/`Aisle`).
enum class DietType {
    OMNIVORE,
    PESCETARIAN,
    VEGETARIAN,
    VEGAN,
    KETOGENIC,
}
