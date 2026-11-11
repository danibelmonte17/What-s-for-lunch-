package com.whatsforlunch.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.StringRes
import com.whatsforlunch.app.R

// Identificadores estables para las pruebas instrumentadas (E2E) de la barra
// inferior de navegación.
object AppBottomBarTestTags {
    const val BAR = "bottom_bar"
    const val TAB_MY_RECIPES = "bottom_bar_tab_my_recipes"
    const val TAB_PANTRY = "bottom_bar_tab_pantry"
    const val TAB_SHOPPING_LIST = "bottom_bar_tab_shopping_list"
    const val TAB_PINCHE = "bottom_bar_tab_pinche"
    const val TAB_PROFILE = "bottom_bar_tab_profile"
}

// Pestañas de la barra inferior en su orden (RF-MOB1, D2/D7/D8): las etiquetas
// reutilizan las claves de título de cada pantalla («Mi recetario» abre con su
// etiqueta corta «Recetario») y la quinta pestaña («Mi perfil») la añade
// `crear-mi-perfil` al final, tras Pinche (D4).
enum class AppTab(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
) {
    MY_RECIPES(
        route = AppRoutes.MY_RECIPES,
        labelRes = R.string.my_recipes_tab_label,
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = AppBottomBarTestTags.TAB_MY_RECIPES,
    ),
    PANTRY(
        route = AppRoutes.PANTRY,
        labelRes = R.string.pantry_title,
        icon = Icons.Outlined.Kitchen,
        testTag = AppBottomBarTestTags.TAB_PANTRY,
    ),
    SHOPPING_LIST(
        route = AppRoutes.SHOPPING_LIST,
        labelRes = R.string.shopping_list_title,
        icon = Icons.Outlined.ShoppingCart,
        testTag = AppBottomBarTestTags.TAB_SHOPPING_LIST,
    ),
    PINCHE(
        route = AppRoutes.PINCHE,
        labelRes = R.string.pinche_title,
        icon = Icons.AutoMirrored.Outlined.Chat,
        testTag = AppBottomBarTestTags.TAB_PINCHE,
    ),
    MY_PROFILE(
        route = AppRoutes.MY_PROFILE,
        labelRes = R.string.my_profile_title,
        icon = Icons.Outlined.Person,
        testTag = AppBottomBarTestTags.TAB_PROFILE,
    ),
}

// Barra inferior de navegación común a las cinco pantallas de pestañas (RF-MOB1,
// D1/D6). Tocar la pestaña activa no navega: dispara `onReselect` para que la
// pantalla actual vuelva al inicio de su contenido (RF-MOB6), sin crear una
// entrada nueva en el back stack.
@Composable
fun AppBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onReselect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.testTag(AppBottomBarTestTags.BAR),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AppTab.values().forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { if (selected) onReselect() else onTabSelected(tab) },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = null)
                },
                label = {
                    // La etiqueta larga («Lista de la Compra») admite dos líneas
                    // y la accesibilidad del ítem usa este texto.
                    Text(
                        text = stringResource(tab.labelRes),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontWeight = if (selected) FontWeight.Bold else null,
                    )
                },
                alwaysShowLabel = true,
                // RF-MOB6: la pestaña activa solo tinta icono y texto en el
                // color primary; sin fondo ni indicador pastilla.
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
                modifier = Modifier.testTag(tab.testTag),
            )
        }
    }
}
