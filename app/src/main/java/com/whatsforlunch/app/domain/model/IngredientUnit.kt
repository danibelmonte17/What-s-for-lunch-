package com.whatsforlunch.app.domain.model

// Unidad de un Ingrediente o de un ítem de la Lista de la Compra (RD19): vocabulario
// cerrado (D1) con cinco valores —ud, g, kg, ml, l— compartido por toda la app. Los
// códigos visibles son los mismos en es/en y cada uno vive como clave en strings.xml
// (`unit_ud`…`unit_l`, P19/P25); la presentación (código → texto) es responsabilidad
// de la UI (D4).
//
// `RecipeIngredient.unit` y `ShoppingListItem.unit` siguen siendo `String`: el enum es
// el vocabulario canónico, no el tipo de almacenamiento. Las unidades ya guardadas
// fuera de la lista no se migran ni se recalculan (D5) y se muestran como texto; lo que
// llega de la IA se normaliza aquí con `normalize` (D2, regla pura en domain/, P25).
enum class IngredientUnit(val code: String) {
    UD("ud"),
    G("g"),
    KG("kg"),
    ML("ml"),
    L("l");

    companion object {
        /**
         * Código → enum, insensible a mayúsculas y a espacios alrededor (trim).
         * Devuelve `null` para la unidad vacía y para cualquier texto legado
         * fuera de la lista (D1/D5).
         */
        fun fromCode(text: String?): IngredientUnit? {
            val code = text?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.code == code }
        }

        /**
         * D2: normaliza la unidad que llega de la IA (Sugerencias y Propuestas del
         * Pinche) al vocabulario cerrado; lo vacío o desconocido cae a «ud». Lo ya
         * guardado no se toca.
         */
        fun normalize(text: String?): String = fromCode(text)?.code ?: UD.code
    }
}
