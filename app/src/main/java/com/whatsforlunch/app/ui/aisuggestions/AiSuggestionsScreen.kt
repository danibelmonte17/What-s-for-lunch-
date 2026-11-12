package com.whatsforlunch.app.ui.aisuggestions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.domain.suggestion.SuggestionAcceptance
import com.whatsforlunch.app.domain.suggestion.SuggestionState
import com.whatsforlunch.app.domain.suggestion.SuggestionWithMatch
import com.whatsforlunch.app.ui.components.MatchBadge
import com.whatsforlunch.app.ui.components.unitLabel
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import java.text.NumberFormat
import java.util.Locale

// Identificadores estables de la pantalla de Sugerencias (E2E).
object AiSuggestionsTestTags {
    const val SCREEN = "ai_suggestions_screen"
    const val SELECTION_LIST = "ai_suggestions_selection_list"
    const val REQUEST = "ai_suggestions_request"
    const val PROGRESS = "ai_suggestions_progress"
    const val RESULT_LIST = "ai_suggestions_result_list"
    const val SUGGESTION_TITLE = "ai_suggestions_suggestion_title"
    const val SUGGESTION_INGREDIENTS = "ai_suggestions_suggestion_ingredients"
    const val SUGGESTION_STEPS = "ai_suggestions_suggestion_steps"
    const val EMPTY = "ai_suggestions_empty"
    const val NOTICE = "ai_suggestions_notice"
    const val NOTICE_DISMISS = "ai_suggestions_notice_dismiss"
    const val RETRY = "ai_suggestions_retry"
    const val OPEN_SETTINGS = "ai_suggestions_open_settings"

    fun selectionItem(name: String) = "ai_suggestions_selection_item_$name"

    fun selectionToggle(name: String) = "ai_suggestions_selection_toggle_$name"

    fun suggestionCard(index: Int) = "ai_suggestions_suggestion_card_$index"

    fun suggestionMatch(index: Int) = "ai_suggestions_suggestion_match_$index"

    fun suggestionState(index: Int) = "ai_suggestions_suggestion_state_$index"

    fun suggestionAccept(index: Int) = "ai_suggestions_suggestion_accept_$index"

    fun suggestionDiscard(index: Int) = "ai_suggestions_suggestion_discard_$index"
}

private val CardShape = RoundedCornerShape(WarmRadius.lg)
private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSuggestionsScreen(
    uiState: AiSuggestionsUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFood: (String) -> Unit,
    onRequest: () -> Unit,
    onRetry: () -> Unit,
    onAccept: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phase = uiState.phase
    Scaffold(
        modifier = modifier.testTag(AiSuggestionsTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_suggestions_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            if (phase is AiSuggestionsPhase.Selection) {
                RequestBar(
                    enabled = phase.canRequest,
                    onRequest = onRequest,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            uiState.notice?.let { notice ->
                NoticeBanner(
                    notice = notice,
                    onOpenSettings = onOpenSettings,
                    onRetry = onRetry,
                    onDismiss = onClearNotice,
                )
            }
            when (phase) {
                is AiSuggestionsPhase.Selection -> SelectionContent(
                    phase = phase,
                    onToggleFood = onToggleFood,
                )

                AiSuggestionsPhase.Loading -> LoadingContent()

                is AiSuggestionsPhase.Results -> ResultsContent(
                    items = phase.items,
                    states = phase.states,
                    onAccept = onAccept,
                    onDiscard = onDiscard,
                )

                AiSuggestionsPhase.Empty -> EmptyContent(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun SelectionContent(
    phase: AiSuggestionsPhase.Selection,
    onToggleFood: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AiSuggestionsTestTags.SELECTION_LIST),
        contentPadding = PaddingValues(
            start = WarmSpacing.md,
            end = WarmSpacing.md,
            top = WarmSpacing.md,
            bottom = WarmSpacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        item(key = "selection_title") {
            Text(
                stringResource(R.string.ai_suggestions_selection_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        itemsIndexed(phase.foods, key = { _, food -> food.name }) { _, food ->
            SelectionRow(
                food = food,
                checked = food.name in phase.selected,
                onToggle = { onToggleFood(food.name) },
            )
        }
    }
}

@Composable
private fun SelectionRow(
    food: SuggestableFood,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val accessibility = stringResource(R.string.ai_suggestions_selection_item_accessibility, food.name)
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibility }
            .testTag(AiSuggestionsTestTags.selectionItem(food.name)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.sm, vertical = WarmSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag(AiSuggestionsTestTags.selectionToggle(food.name)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                food.quantity?.let { quantity ->
                    Text(
                        stringResource(R.string.pantry_quantity_value, quantity.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestBar(enabled: Boolean, onRequest: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            Button(
                onClick = onRequest,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.md)
                    .heightIn(min = 52.dp)
                    .testTag(AiSuggestionsTestTags.REQUEST),
            ) {
                Text(stringResource(R.string.ai_suggestions_request))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().testTag(AiSuggestionsTestTags.PROGRESS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.ai_suggestions_progress),
            modifier = Modifier.padding(top = WarmSpacing.md),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WarmSpacing.lg)
            .testTag(AiSuggestionsTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.ai_suggestions_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RetryButton(
            onRetry = onRetry,
            modifier = Modifier.padding(top = WarmSpacing.md),
        )
    }
}

@Composable
private fun NoticeBanner(
    notice: AiSuggestionsNotice,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm)
            .testTag(AiSuggestionsTestTags.NOTICE),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(WarmRadius.md),
    ) {
        Column(
            modifier = Modifier.padding(start = WarmSpacing.md, end = WarmSpacing.xs, top = WarmSpacing.sm, bottom = WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(notice.messageRes()),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag(AiSuggestionsTestTags.NOTICE_DISMISS),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_dismiss),
                    )
                }
            }
            if (notice == AiSuggestionsNotice.AI_KEY_MISSING) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(AiSuggestionsTestTags.OPEN_SETTINGS),
                ) {
                    Text(stringResource(R.string.ai_suggestions_open_settings))
                }
            } else if (notice.isFailure) {
                RetryButton(onRetry = onRetry)
            }
        }
    }
}

private val AiSuggestionsNotice.isFailure: Boolean
    get() = this == AiSuggestionsNotice.FAILED ||
        this == AiSuggestionsNotice.FAILED_NO_CONNECTION ||
        this == AiSuggestionsNotice.FAILED_PROVIDER_ERROR ||
        this == AiSuggestionsNotice.FAILED_INVALID_RESPONSE

private fun AiSuggestionsNotice.messageRes(): Int = when (this) {
    AiSuggestionsNotice.AI_KEY_MISSING -> R.string.ai_suggestions_key_missing
    AiSuggestionsNotice.FAILED -> R.string.ai_suggestions_failed
    AiSuggestionsNotice.FAILED_NO_CONNECTION -> R.string.ai_suggestions_failed_no_connection
    AiSuggestionsNotice.FAILED_PROVIDER_ERROR -> R.string.ai_suggestions_failed_provider
    AiSuggestionsNotice.FAILED_INVALID_RESPONSE -> R.string.ai_suggestions_failed_invalid_response
    AiSuggestionsNotice.SAVED -> R.string.ai_suggestions_accepted_notice
}

@Composable
private fun RetryButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onRetry,
        modifier = modifier.heightIn(min = 48.dp).testTag(AiSuggestionsTestTags.RETRY),
    ) {
        Text(stringResource(R.string.ai_suggestions_retry))
    }
}

@Composable
private fun ResultsContent(
    items: List<SuggestionWithMatch>,
    states: Map<String, SuggestionState>,
    onAccept: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AiSuggestionsTestTags.RESULT_LIST),
        contentPadding = PaddingValues(WarmSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        itemsIndexed(items) { index, item ->
            val id = SuggestionAcceptance.suggestionRecipeId(item.suggestion.title)
            SuggestionCard(
                item = item,
                index = index,
                locale = locale,
                state = states[id] ?: SuggestionState.PROPOSED,
                onAccept = { onAccept(id) },
                onDiscard = { onDiscard(id) },
            )
        }
    }
}

// Tarjeta de Sugerencia (D5 de `sugerir-recetas-con-ia`, RF-ASR1): el cuerpo sigue
// siendo inerte y el pie ofrece «Aceptar» y «Descartar», deshabilitados en cuanto la
// propuesta se acepta o se descarta (RF-ASR3, RF-ASR4).
@Composable
private fun SuggestionCard(
    item: SuggestionWithMatch,
    index: Int,
    locale: Locale,
    state: SuggestionState,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
) {
    val suggestion = item.suggestion
    val accessibility = stringResource(R.string.ai_suggestions_card_accessibility, suggestion.title)
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibility }
            .testTag(AiSuggestionsTestTags.suggestionCard(index)),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag(AiSuggestionsTestTags.SUGGESTION_TITLE),
                )
                if (state != SuggestionState.PROPOSED) {
                    Text(
                        stringResource(
                            if (state == SuggestionState.ACCEPTED) R.string.ai_suggestions_state_accepted
                            else R.string.ai_suggestions_state_discarded,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = WarmSpacing.sm)
                            .testTag(AiSuggestionsTestTags.suggestionState(index)),
                    )
                }
                item.match?.let { match ->
                    MatchBadge(
                        match = match,
                        testTag = AiSuggestionsTestTags.suggestionMatch(index),
                        modifier = Modifier.padding(start = WarmSpacing.sm),
                    )
                }
            }
            item.match?.let { match -> IngredientAvailability(match = match) }
            if (suggestion.story.isNotBlank()) {
                Text(
                    suggestion.story,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IngredientsSection(ingredients = suggestion.ingredients, locale = locale)
            StepsSection(steps = suggestion.steps)
            SuggestionActions(
                index = index,
                enabled = state == SuggestionState.PROPOSED,
                onAccept = onAccept,
                onDiscard = onDiscard,
            )
        }
    }
}

// Pie de acciones de la tarjeta: «Aceptar» primario y «Descartar» secundario, ambos
// de ≥ 48 dp y deshabilitados fuera del estado propuesta (RF-ASR1, RF-ASR3, RF-ASR4).
@Composable
private fun SuggestionActions(
    index: Int,
    enabled: Boolean,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Button(
            onClick = onAccept,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag(AiSuggestionsTestTags.suggestionAccept(index)),
        ) {
            Text(stringResource(R.string.ai_suggestions_accept))
        }
        Button(
            onClick = onDiscard,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag(AiSuggestionsTestTags.suggestionDiscard(index)),
        ) {
            Text(stringResource(R.string.ai_suggestions_discard))
        }
    }
}

@Composable
private fun IngredientAvailability(match: RecipeMatch) {
    Text(
        stringResource(
            R.string.ai_suggestions_card_ingredients,
            match.availableRequired,
            match.totalRequired,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IngredientsSection(ingredients: List<RecipeIngredient>, locale: Locale) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(AiSuggestionsTestTags.SUGGESTION_INGREDIENTS),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        Text(
            stringResource(R.string.ai_suggestions_ingredients_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ingredients.forEach { ingredient ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ingredientLine(ingredient, locale, unit = unitLabel(ingredient.unit)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepsSection(steps: List<RecipeStep>) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(AiSuggestionsTestTags.SUGGESTION_STEPS),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        Text(
            stringResource(R.string.ai_suggestions_steps_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        steps.forEachIndexed { stepIndex, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .heightIn(min = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            (stepIndex + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = WarmSpacing.sm)) {
                    Text(
                        step.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    step.timerMinutes?.let { minutes ->
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(top = WarmSpacing.xs),
                        ) {
                            Text(
                                stringResource(R.string.my_recipes_card_time, minutes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// «cantidad unidad nombre» con el formato decimal del idioma activo. `unit` llega
// presentado con `unitLabel` (unificar-unidades, D4).
private fun ingredientLine(ingredient: RecipeIngredient, locale: Locale, unit: String = ingredient.unit): String {
    val quantity = if (ingredient.quantity > 0.0) {
        NumberFormat.getNumberInstance(locale).format(ingredient.quantity)
    } else {
        ""
    }
    return listOf(quantity, unit, ingredient.name)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
