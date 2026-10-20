package com.whatsforlunch.app.di

import androidx.room.Room
import com.whatsforlunch.app.data.ai.AiKeyProvider
import com.whatsforlunch.app.data.ai.ImageGenerator
import com.whatsforlunch.app.data.ai.OpenAiCompatibleImageGenerator
import com.whatsforlunch.app.data.ai.UnconfiguredAiKeyProvider
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.repository.RoomPantryRepository
import com.whatsforlunch.app.data.photo.FilePhotoStore
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.ui.pantry.PantryViewModel
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaViewModel
import com.whatsforlunch.app.ui.misrecetas.MisRecetasViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Módulos Koin del scaffold (Q-arrancar-app-en-mis-recetas-4), ampliados para la
// persistencia Room y el formulario de alta (Parte B, T2/T6).
val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), RecipeDatabase::class.java, "whatsforlunch.db")
            .addMigrations(RecipeDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<RecipeDatabase>().recipeDao() }
    single { get<RecipeDatabase>().pantryItemDao() }
    single<RecipeRepository> { RoomRecipeRepository(get(), get()) }
    single<PantryRepository> { RoomPantryRepository(get()) }
    single<PhotoStore> { FilePhotoStore(androidContext()) }

    single { OkHttpClient() }
    // La API key llega con configurar-acceso-a-ia (⚠ S05, Q-crear-receta-6).
    single<AiKeyProvider> { UnconfiguredAiKeyProvider() }
    single<ImageGenerator> { OpenAiCompatibleImageGenerator(get(), get()) }

    viewModel { MisRecetasViewModel(get()) }
    viewModel { CrearRecetaViewModel(get(), get()) }
    viewModel { PantryViewModel(get()) }
}
