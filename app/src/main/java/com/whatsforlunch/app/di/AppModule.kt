package com.whatsforlunch.app.di

import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.ui.misrecetas.MisRecetasViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Módulos Koin del scaffold (Q-arrancar-app-en-mis-recetas-4).
val appModule = module {
    single<RecipeRepository> { InMemoryRecipeRepository() }
    viewModel { MisRecetasViewModel(get()) }
}
