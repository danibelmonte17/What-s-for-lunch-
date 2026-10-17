package com.whatsforlunch.app

import com.whatsforlunch.app.data.local.RecipeDatabase
import org.koin.core.context.GlobalContext

// La app persiste las Recetas en Room (RF-18), así que las pruebas que asumen el
// estado vacío deben limpiar la base de datos antes de cada escenario.
object E2eDatabase {
    fun clear() {
        GlobalContext.get().get<RecipeDatabase>().clearAllTables()
    }
}
