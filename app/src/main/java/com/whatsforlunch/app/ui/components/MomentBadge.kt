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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Badge de Momento culinario en píldora compartido: la tarjeta de Mi recetario lo
// superpone a la portada (o lo muestra junto al título sin foto) y el hero del
// Detalle de Receta lo reutiliza con la misma semántica (RF-DR3). El testTag lo
// aporta cada pantalla.
private val BadgeShape: Shape = RoundedCornerShape(50)

@Composable
fun MomentBadge(
    mealMoment: MealMoment,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = BadgeShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.testTag(testTag),
    ) {
        Text(
            text = stringResource(
                when (mealMoment) {
                    MealMoment.BREAKFAST -> R.string.meal_moment_breakfast
                    MealMoment.LUNCH -> R.string.meal_moment_lunch
                    MealMoment.DINNER -> R.string.meal_moment_dinner
                    MealMoment.DESSERT -> R.string.meal_moment_dessert
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
        )
    }
}
