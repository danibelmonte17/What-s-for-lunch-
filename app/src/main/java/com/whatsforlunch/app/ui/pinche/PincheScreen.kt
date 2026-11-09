package com.whatsforlunch.app.ui.pinche

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.pinche.PincheAuthor
import com.whatsforlunch.app.domain.pinche.PincheProposal
import com.whatsforlunch.app.domain.pinche.PincheProposalState
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow
import java.text.NumberFormat
import java.util.Locale

// Identificadores estables de la pantalla del Pinche (E2E).
object PincheTestTags {
    const val SCREEN = "pinche_screen"
    const val MESSAGE_LIST = "pinche_message_list"
    const val QUICK_SUGGESTION = "pinche_quick_suggestion"
    const val COMPOSER = "pinche_composer"
    const val SEND = "pinche_send"
    const val PROGRESS = "pinche_progress"
    const val NOTICE = "pinche_notice"
    const val RETRY = "pinche_retry"
    const val OPEN_SETTINGS = "pinche_open_settings"
    const val PROPOSAL_TITLE = "pinche_proposal_title"
    const val PROPOSAL_INGREDIENTS = "pinche_proposal_ingredients"
    const val PROPOSAL_STEPS = "pinche_proposal_steps"
    const val PROPOSAL_MISSING = "pinche_proposal_missing"
    const val SAVE = "pinche_save"
    const val SAVED_LABEL = "pinche_saved_label"
    const val ADD_MISSING = "pinche_add_missing"
    const val ADDED_MISSING_LABEL = "pinche_added_missing_label"
    const val OPEN_PROFILE = "pinche_open_profile"

    fun userMessage(index: Int) = "pinche_user_message_$index"

    fun assistantMessage(index: Int) = "pinche_assistant_message_$index"

    fun proposalCard(index: Int) = "pinche_proposal_card_$index"

    fun quickSuggestion(index: Int) = "pinche_quick_suggestion_$index"
}

private val BubbleShape = RoundedCornerShape(WarmRadius.md)
private val CardShape = RoundedCornerShape(WarmRadius.lg)
private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PincheScreen(
    uiState: PincheUiState,
    onOpenSettings: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickSuggestion: (String) -> Unit,
    onRetry: () -> Unit,
    onSaveProposal: (String) -> Unit,
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
    onAddMissingToShoppingList: (String) -> Unit = {},
    // `crear-mi-perfil` (D8): acción del mensaje que pide el perfil pendiente.
    onOpenProfile: () -> Unit = {},
    scrollToTopTick: Int = 0,
) {
    // RF-MOB6: tocar la pestaña activa devuelve el contenido a su primera posición.
    val messageListState = rememberLazyListState()

    // RF-MOB6: el trigger lo eleva la barra (extraída a WhatsForLunchApp); el
    // valor inicial no desplaza nada.
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) messageListState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = modifier.testTag(PincheTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // RF-MOB5: el Pinche es destino de primer nivel y ya no muestra «Volver».
            TopAppBar(
                title = { Text(stringResource(R.string.pinche_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            uiState.notice?.let { notice ->
                NoticeBanner(
                    notice = notice,
                    onOpenSettings = onOpenSettings,
                    onRetry = onRetry,
                    onDismiss = onClearNotice,
                )
            }
            MessageList(
                uiState = uiState,
                onQuickSuggestion = onQuickSuggestion,
                onSaveProposal = onSaveProposal,
                onAddMissingToShoppingList = onAddMissingToShoppingList,
                onOpenProfile = onOpenProfile,
                listState = messageListState,
                modifier = Modifier.weight(1f),
            )
            if (uiState.isSending) {
                ThinkingRow()
            }
            Composer(
                input = uiState.input,
                enabled = !uiState.isSending,
                onInputChange = onInputChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun MessageList(
    uiState: PincheUiState,
    onQuickSuggestion: (String) -> Unit,
    onSaveProposal: (String) -> Unit,
    onAddMissingToShoppingList: (String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val quickSuggestions = listOf(
        stringResource(R.string.pinche_quick_1),
        stringResource(R.string.pinche_quick_2),
        stringResource(R.string.pinche_quick_3),
    )
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().testTag(PincheTestTags.MESSAGE_LIST),
        contentPadding = PaddingValues(WarmSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        if (uiState.messages.isEmpty()) {
            item(key = "greeting") {
                Greeting()
            }
            item(key = "quick_suggestions") {
                Column(
                    modifier = Modifier.testTag(PincheTestTags.QUICK_SUGGESTION),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    quickSuggestions.forEachIndexed { index, suggestion ->
                        QuickSuggestionChip(
                            index = index,
                            text = suggestion,
                            onClick = { onQuickSuggestion(suggestion) },
                        )
                    }
                }
            }
        } else {
            itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                val authorLabel = if (message.author == PincheAuthor.USER) {
                    stringResource(R.string.pinche_you)
                } else {
                    stringResource(R.string.pinche_title)
                }
                val accessibility = stringResource(R.string.pinche_message_accessibility, authorLabel)
                val isUser = message.author == PincheAuthor.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = BubbleShape,
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .semantics { contentDescription = accessibility }
                            .testTag(
                                if (isUser) PincheTestTags.userMessage(index)
                                else PincheTestTags.assistantMessage(index),
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(WarmSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                        ) {
                            when {
                                // Mensaje de petición del perfil (RD26/D5): texto
                                // localizado con las partes pendientes y acceso a
                                // «Mi perfil»; nunca viaja al modelo.
                                message.profileRequest.isNotEmpty() -> ProfileRequestContent(
                                    pending = message.profileRequest,
                                    onOpenProfile = onOpenProfile,
                                )

                                message.text.isNotBlank() -> Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            message.proposal?.let { proposal ->
                                ProposalCard(
                                    index = index,
                                    proposal = proposal,
                                    state = message.proposalState,
                                    addedToList = message.addedToList,
                                    locale = locale,
                                    onSave = { onSaveProposal(message.id) },
                                    onAddMissing = { onAddMissingToShoppingList(message.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting() {
    Text(
        text = stringResource(R.string.pinche_greeting),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Mensaje de petición del perfil (`crear-mi-perfil`, D8): lista solo las partes
// pendientes del perfil y ofrece la acción «Abrir Mi perfil», que navega a la
// pestaña del perfil sin responder todavía (RD26).
@Composable
private fun ProfileRequestContent(
    pending: List<PendingProfilePart>,
    onOpenProfile: () -> Unit,
) {
    val names = pending.map { part -> pendingLabel(part) }.joinToString(", ")
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        Text(
            text = stringResource(R.string.pinche_profile_request, names),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(
            onClick = onOpenProfile,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(PincheTestTags.OPEN_PROFILE),
        ) {
            Text(stringResource(R.string.pinche_profile_open))
        }
    }
}

@Composable
private fun pendingLabel(part: PendingProfilePart): String = when (part) {
    PendingProfilePart.ALLERGIES -> stringResource(R.string.my_profile_allergies_title)
    PendingProfilePart.INTOLERANCES -> stringResource(R.string.my_profile_intolerances_title)
    PendingProfilePart.DIET_TYPE -> stringResource(R.string.my_profile_diet_title)
}

@Composable
private fun QuickSuggestionChip(index: Int, text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = text) },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(PincheTestTags.quickSuggestion(index)),
    )
}

@Composable
private fun ThinkingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm)
            .testTag(PincheTestTags.PROGRESS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        LinearProgressIndicator(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.pinche_thinking),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Composer(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                enabled = enabled,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.pinche_input_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (enabled && input.isNotBlank()) onSend() },
                ),
                modifier = Modifier.weight(1f).testTag(PincheTestTags.COMPOSER),
            )
            Button(
                onClick = onSend,
                enabled = enabled && input.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag(PincheTestTags.SEND),
            ) {
                Text(stringResource(R.string.pinche_send))
            }
        }
    }
}

@Composable
private fun NoticeBanner(
    notice: PincheNotice,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm)
            .testTag(PincheTestTags.NOTICE),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(WarmRadius.md),
    ) {
        Row(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(
                    when (notice) {
                        PincheNotice.AI_KEY_MISSING -> R.string.pinche_key_missing
                        PincheNotice.FAILED -> R.string.pinche_failed
                        PincheNotice.SAVE_FAILED -> R.string.pinche_save_failed
                        PincheNotice.SHOPPING_LIST_FAILED -> R.string.pinche_shopping_list_failed
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            when (notice) {
                PincheNotice.AI_KEY_MISSING -> Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(PincheTestTags.OPEN_SETTINGS),
                ) {
                    Text(stringResource(R.string.pinche_open_settings))
                }

                PincheNotice.FAILED -> Button(
                    onClick = onRetry,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(PincheTestTags.RETRY),
                ) {
                    Text(stringResource(R.string.pinche_retry))
                }

                PincheNotice.SAVE_FAILED -> Unit

                PincheNotice.SHOPPING_LIST_FAILED -> Unit
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.pinche_notice_dismiss),
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    index: Int,
    proposal: PincheProposal,
    state: PincheProposalState,
    addedToList: Boolean,
    locale: Locale,
    onSave: () -> Unit,
    onAddMissing: () -> Unit,
) {
    val recipe = proposal.recipe
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(PincheTestTags.proposalCard(index)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(topStart = WarmRadius.lg, topEnd = WarmRadius.lg),
            ) {
                Text(
                    text = stringResource(R.string.pinche_proposal_header),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(WarmSpacing.sm),
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = WarmSpacing.md,
                    end = WarmSpacing.md,
                    bottom = WarmSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(PincheTestTags.PROPOSAL_TITLE),
                )
                Text(
                    text = listOf(
                        stringResource(recipe.mealMoment.labelRes()),
                        stringResource(
                            R.string.my_recipes_card_time,
                            recipe.prepMinutes + recipe.cookMinutes,
                        ),
                        stringResource(R.string.my_recipes_card_servings, recipe.baseServings),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IngredientsSection(
                    ingredients = recipe.ingredients,
                    locale = locale,
                    modifier = Modifier.testTag(PincheTestTags.PROPOSAL_INGREDIENTS),
                )
                StepsSection(
                    steps = recipe.steps,
                    modifier = Modifier.testTag(PincheTestTags.PROPOSAL_STEPS),
                )
                if (proposal.missingIngredients.isNotEmpty()) {
                    MissingSection(missing = proposal.missingIngredients)
                    if (addedToList) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag(PincheTestTags.ADDED_MISSING_LABEL),
                        ) {
                            Text(stringResource(R.string.pinche_added_missing))
                        }
                    } else {
                        OutlinedButton(
                            onClick = onAddMissing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag(PincheTestTags.ADD_MISSING),
                        ) {
                            Text(stringResource(R.string.pinche_add_missing))
                        }
                    }
                }
                if (state == PincheProposalState.SAVED) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(PincheTestTags.SAVED_LABEL),
                    ) {
                        Text(stringResource(R.string.pinche_saved))
                    }
                } else {
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(PincheTestTags.SAVE),
                    ) {
                        Text(stringResource(R.string.pinche_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientsSection(
    ingredients: List<RecipeIngredient>,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.pinche_proposal_ingredients),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ingredients.forEach { ingredient ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ingredientLine(ingredient, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (ingredient.optional) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.pinche_proposal_optional),
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

@Composable
private fun StepsSection(steps: List<RecipeStep>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.pinche_proposal_steps),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = WarmSpacing.sm)) {
                    Text(
                        text = step.text,
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
                                text = stringResource(R.string.pinche_timer_minutes, minutes),
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

@Composable
private fun MissingSection(missing: List<String>) {
    Surface(
        shape = RoundedCornerShape(WarmRadius.md),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth().testTag(PincheTestTags.PROPOSAL_MISSING),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Text(
                text = stringResource(R.string.pinche_proposal_missing),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            missing.forEach { name ->
                Text(
                    text = "• $name",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun ingredientLine(ingredient: RecipeIngredient, locale: Locale): String {
    val quantity = if (ingredient.quantity > 0.0) {
        NumberFormat.getNumberInstance(locale).format(ingredient.quantity)
    } else {
        ""
    }
    return listOf(quantity, ingredient.unit, ingredient.name)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun MealMoment.labelRes(): Int = when (this) {
    MealMoment.BREAKFAST -> R.string.meal_moment_breakfast
    MealMoment.LUNCH -> R.string.meal_moment_lunch
    MealMoment.DINNER -> R.string.meal_moment_dinner
    MealMoment.DESSERT -> R.string.meal_moment_dessert
}
