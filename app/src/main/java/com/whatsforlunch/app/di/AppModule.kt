package com.whatsforlunch.app.di

import androidx.room.Room
import com.whatsforlunch.app.BuildConfig
import com.whatsforlunch.app.data.ai.AiKeyProvider
import com.whatsforlunch.app.data.ai.AiKeyStore
import com.whatsforlunch.app.data.ai.ImageGenerator
import com.whatsforlunch.app.data.ai.KeystoreAiKeyStore
import com.whatsforlunch.app.data.ai.OpenAiCompatibleImageGenerator
import com.whatsforlunch.app.data.ai.OpenAiCompatibleRecipeSuggester
import com.whatsforlunch.app.data.ai.OpenAiCompatibleScanImageAnalyzer
import com.whatsforlunch.app.data.ai.PriorityAiKeyProvider
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.repository.RoomPantryRepository
import com.whatsforlunch.app.data.photo.FilePhotoStore
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsViewModel
import com.whatsforlunch.app.ui.pantry.PantryViewModel
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeViewModel
import com.whatsforlunch.app.ui.myrecipes.MyRecipesViewModel
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailViewModel
import com.whatsforlunch.app.ui.settings.SettingsViewModel
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
    // La key puede venir del build (local.properties, D7-D8) con prioridad sobre la
    // guardada cifrada en el dispositivo (D2, ⚠ S05); ImageGenerator no cambia su
    // comportamiento (RF-21, Q-crear-receta-6).
    single<AiKeyStore> { KeystoreAiKeyStore(androidContext()) }
    single<AiKeyProvider> { PriorityAiKeyProvider(BuildConfig.AI_API_KEY, get<AiKeyStore>()) }
    single<ImageGenerator> {
        OpenAiCompatibleImageGenerator(get(), get(), BuildConfig.AI_BASE_URL)
    }
    single<ScanImageAnalyzer> {
        OpenAiCompatibleScanImageAnalyzer(get(), get(), BuildConfig.AI_BASE_URL)
    }
    single<RecipeSuggester> {
        OpenAiCompatibleRecipeSuggester(get(), get(), BuildConfig.AI_BASE_URL)
    }
    // Chips del Escaneo sin guardar compartidos entre el Escáner y Sugerencias (D3).
    single { ScanDraftHolder() }

    viewModel { MyRecipesViewModel(get(), get()) }
    viewModel { CreateRecipeViewModel(get(), get()) }
    viewModel { PantryViewModel(get()) }
    viewModel { ScannerViewModel(get(), get(), get(), get()) }
    viewModel { AiSuggestionsViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), BuildConfig.AI_API_KEY.isNotBlank()) }
    // El id de la Receta llega por ruta (recipe_detail/{recipeId}, RF-DR1).
    viewModel { parameters ->
        RecipeDetailViewModel(
            recipeRepository = get(),
            pantryRepository = get(),
            recipeId = parameters.get(),
        )
    }
}
