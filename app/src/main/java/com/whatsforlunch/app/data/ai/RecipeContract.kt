package com.whatsforlunch.app.data.ai

// Contrato JSON compacto compartido por las Sugerencias y las Propuestas del Pinche.
// Los prompts lo insertan con el marcador `{RECIPE_CONTRACT}` para que el formato de
// salida y el parser no puedan desincronizarse. Desde `redisenar-formulario-de-receta`
// el contrato no tiene tiempo de cocción (`cm`) ni opcionalidad (`o`, D8).
internal const val RECIPE_CONTRACT =
    "{\"s\":[{\"t\":\"title\",\"st\":\"short story\",\"mm\":\"BREAKFAST|LUNCH|DINNER|DESSERT\"," +
        "\"pm\":10,\"df\":\"EASY|MEDIUM|HARD\",\"bs\":4,\"kcal\":320," +
        "\"i\":[{\"n\":\"name\",\"q\":3,\"u\":\"unit\"}]," +
        "\"p\":[{\"t\":\"step text\",\"m\":10}]}]}"
