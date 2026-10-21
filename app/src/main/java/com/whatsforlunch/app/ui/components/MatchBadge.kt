package com.whatsforlunch.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Badge de Coincidencia compartido (D5 de ver-detalle-de-receta): componente único
// reutilizado por la tarjeta de Mis Recetas y por el hero del Detalle, con la misma
// semántica de mostrar-badge-de-coincidencia (RD01/RD02/RD10, RF-MB6). El testTag lo
// aporta cada pantalla para conservar sus aserciones.
private val BadgeShape: Shape = RoundedCornerShape(50)

@Composable
fun MatchBadge(
    match: RecipeMatch,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val fullMatch = match.isFullMatch
    val accessibilityDescription = stringResource(
        R.string.my_recipes_card_match_content_description,
        match.percentage,
    )
    Surface(
        shape = BadgeShape,
        color = if (fullMatch) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (fullMatch) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .semantics {
                contentDescription = accessibilityDescription
            }
            .testTag(testTag),
    ) {
        Text(
            text = if (fullMatch) stringResource(R.string.my_recipes_card_match_full)
            else stringResource(R.string.my_recipes_card_match, match.percentage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
        )
    }
}
