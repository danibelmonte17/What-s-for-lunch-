package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.RoomPantryRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myprofile.MyProfileTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.pinche.PincheTestTags
import com.whatsforlunch.app.ui.recommendations.RecommendationsTestTags
import com.whatsforlunch.app.ui.shoppinglist.ShoppingListTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

// verify 1–2 · RF-MOB1..RF-MOB7: la barra inferior con sus 5 pestañas y sus
// destinos, la top bar de Mi recetario solo con «Ajustes», los dos FABs apilados
// solo con Recetas, la pestaña activa resaltada, «Volver» retirado en las
// pantallas de pestañas y un back stack coherente (D1–D8).
@RunWith(AndroidJUnit4::class)
class BottomBarE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1 (RF-MOB1, verify 1): las 5 pestañas en orden con sus etiquetas y cada
    // una abre su pantalla actual.
    @Test
    fun bottomBarShowsFiveTabsInOrderAndEachOpensItsScreen() {
        composeRule.onNodeWithTag(AppBottomBarTestTags.BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES)
            .assertIsDisplayed()
            .assertTextEquals(localized(R.string.my_recipes_tab_label))
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY)
            .assertIsDisplayed()
            .assertTextEquals(localized(R.string.pantry_title))
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST)
            .assertIsDisplayed()
            .assertTextEquals(localized(R.string.shopping_list_title))
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE)
            .assertIsDisplayed()
            .assertTextEquals(localized(R.string.pinche_title))
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PROFILE)
            .assertIsDisplayed()
            .assertTextEquals(localized(R.string.my_profile_title))

        // Orden de izquierda a derecha: Mi recetario, Despensa, Lista, Pinche y Mi perfil.
        val lefts = listOf(
            AppBottomBarTestTags.TAB_MY_RECIPES,
            AppBottomBarTestTags.TAB_PANTRY,
            AppBottomBarTestTags.TAB_SHOPPING_LIST,
            AppBottomBarTestTags.TAB_PINCHE,
            AppBottomBarTestTags.TAB_PROFILE,
        ).map { composeRule.onNodeWithTag(it).getUnclippedBoundsInRoot().left }
        assertEquals("Las pestañas deben respetar su orden", lefts, lefts.sorted())

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        composeRule.onNodeWithTag(PincheTestTags.SCREEN).assertIsDisplayed()

        // `crear-mi-perfil` (D4): «Mi perfil» es la quinta pestaña, al final.
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PROFILE).performClick()
        composeRule.onNodeWithTag(MyProfileTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_MY_RECIPES).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
    }

    // S2 (RF-MOB2, RF-MOB3, verify 2): la top bar solo conserva «Ajustes» y
    // «Recetas con mi Despensa» se abre desde el segundo FAB con su destino de siempre.
    @Test
    fun topBarKeepsOnlySettingsAndPantryRecipesOpensFromTheFab() {
        seedRecipe(Recipe(id = "tortilla", title = "Tortilla"))
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_SETTINGS).assertIsDisplayed()
        // Los accesos retirados de la top bar ya no existen (RF-MOB2): se comprueban
        // por sus antiguos testTags.
        composeRule.onAllNodesWithTag("action_open_shopping_list").assertCountEquals(0)
        composeRule.onAllNodesWithTag("action_open_pinche").assertCountEquals(0)
        composeRule.onAllNodesWithTag("action_open_pantry").assertCountEquals(0)

        // «Recetas con mi Despensa» conserva su acción y su destino (RF-MOB3).
        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(RecommendationsTestTags.SCREEN).assertIsDisplayed()
    }

    // S4 (RF-MOB4, RF-MOB5, RF-MOB6): la pestaña activa está resaltada, «Volver»
    // ya no aparece, tocar la pestaña activa vuelve al inicio de la pantalla sin
    // resetearla y el back del sistema vuelve a Mi recetario (D1).
    @Test
    fun activeTabIsHighlightedBackIsRemovedAndTheBackStackStaysCoherent() {
        repeat(20) { index -> seedPantry("Alimento %02d".format(index + 1)) }

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).assertIsSelected()
        assertNoBackButton()

        // RF-MOB6: al tocar la pestaña activa el contenido vuelve a su primera
        // posición y los datos no se pierden.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Alimento 01").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PantryTestTags.ITEM_LIST).performTouchInput {
            repeat(3) { swipeUp() }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Alimento 01").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Alimento 01").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Alimento 01").assertIsDisplayed()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).performClick()
        composeRule.onNodeWithTag(ShoppingListTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_SHOPPING_LIST).assertIsSelected()
        assertNoBackButton()

        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).performClick()
        composeRule.onNodeWithTag(PincheTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PINCHE).assertIsSelected()
        assertNoBackButton()

        // RF-MOB4 (D1): el back del sistema vuelve a Mi recetario…
        Espresso.pressBack()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        // …y desde Mi recetario el back sale de la app. El destroy llega tras la
        // animación de salida, así que se espera de forma acotada antes de exigirlo.
        Espresso.pressBackUnconditionally()
        val destroyDeadline = System.currentTimeMillis() + 5_000
        while (
            composeRule.activityRule.scenario.state != Lifecycle.State.DESTROYED &&
            System.currentTimeMillis() < destroyDeadline
        ) {
            Thread.sleep(100)
        }
        assertEquals(Lifecycle.State.DESTROYED, composeRule.activityRule.scenario.state)
    }

    // S5 (RF-MOB3, D3): sin Recetas no aparece ningún botón flotante.
    @Test
    fun emptyStateShowsNoFloatingButtons() {
        composeRule.onNodeWithTag(MyRecipesTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB).assertCountEquals(0)
        composeRule.onAllNodesWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES).assertCountEquals(0)
    }

    // S5 (RF-MOB3, D3): con Recetas aparecen los dos FABs juntos, «Recetas con mi
    // Despensa» apilado encima del de cámara.
    @Test
    fun recipesShowBothFloatingButtonsStacked() {
        seedRecipe(Recipe(id = "tortilla", title = "Tortilla"))
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB).assertIsDisplayed()
        val pantryRecipesTop = composeRule.onNodeWithTag(MyRecipesTestTags.OPEN_PANTRY_RECIPES)
            .getUnclippedBoundsInRoot().top
        val cameraTop = composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB)
            .getUnclippedBoundsInRoot().top
        assertTrue(
            "«Recetas con mi Despensa» debe quedar apilado encima del FAB de cámara",
            pantryRecipesTop < cameraTop,
        )
    }

    // RF-MOB5: ninguna pantalla de pestaña muestra «Volver» ni su equivalente
    // accesible (las tres lo retiraron al ser destinos de primer nivel).
    private fun assertNoBackButton() {
        val back = localized(R.string.action_back)
        composeRule.onAllNodesWithText(back).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription(back).assertCountEquals(0)
    }

    // El idioma efectivo de la app es el de AppLocaleController (Ajustes), no el
    // del dispositivo: las etiquetas esperadas se resuelven en ese idioma para que
    // la aserción no dependa del locale del AVD.
    private fun localized(resourceId: Int, vararg arguments: Any): String {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val language = GlobalContext.get().get<AppLocaleController>().language.value
        val context = baseContext.createConfigurationContext(
            Configuration(baseContext.resources.configuration).apply { setLocale(language.locale) },
        )
        return context.getString(resourceId, *arguments)
    }

    private fun seedRecipe(recipe: Recipe) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomRecipeRepository(database.recipeDao(), BottomBarNoOpPhotoStore)
        runBlocking { repository.saveRecipe(recipe) }
    }

    private fun seedPantry(name: String) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomPantryRepository(database.pantryItemDao())
        runBlocking { repository.savePantryItem(PantryItem(id = "pantry-$name", name = name)) }
    }
}

private object BottomBarNoOpPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) = Unit
}
