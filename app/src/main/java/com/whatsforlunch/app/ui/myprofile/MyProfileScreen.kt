package com.whatsforlunch.app.ui.myprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Identificadores estables de la pantalla «Mi perfil» (E2E).
object MyProfileTestTags {
    const val SCREEN = "my_profile_screen"
    const val SECTION_ALLERGIES = "my_profile_section_allergies"
    const val SECTION_INTOLERANCES = "my_profile_section_intolerances"
    const val SECTION_DIET = "my_profile_section_diet"
    const val INPUT_ALLERGIES = "my_profile_input_allergies"
    const val ADD_ALLERGIES = "my_profile_add_allergies"
    const val NONE_ALLERGIES = "my_profile_none_allergies"
    const val EDIT_ALLERGIES = "my_profile_edit_allergies"
    const val INPUT_INTOLERANCES = "my_profile_input_intolerances"
    const val ADD_INTOLERANCES = "my_profile_add_intolerances"
    const val NONE_INTOLERANCES = "my_profile_none_intolerances"
    const val EDIT_INTOLERANCES = "my_profile_edit_intolerances"

    fun chip(section: ProfileSection, index: Int): String =
        "my_profile_chip_${section.name.lowercase()}_$index"

    fun removeChip(section: ProfileSection, index: Int): String =
        "my_profile_remove_chip_${section.name.lowercase()}_$index"

    fun dietOption(dietType: DietType): String =
        "my_profile_diet_${dietType.name.lowercase()}"
}

// Pantalla «Mi perfil» (`crear-mi-perfil`, S1-S6): dos secciones de restricciones
// con chips, alta por texto, borrado solo en modo edición y «No tengo ninguna»
// exclusiva (RD25), más el tipo de alimentación de selección única (D2). Sigue el
// patrón de Ajustes: tarjetas por sección sobre `Surface` con tokens Warm*.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyProfileScreen(
    uiState: MyProfileUiState,
    onAllergyInputChange: (String) -> Unit,
    onAddAllergy: () -> Unit,
    onRemoveAllergy: (String) -> Unit,
    onAllergyNoneChange: (Boolean) -> Unit,
    onIntoleranceInputChange: (String) -> Unit,
    onAddIntolerance: () -> Unit,
    onRemoveIntolerance: (String) -> Unit,
    onIntoleranceNoneChange: (Boolean) -> Unit,
    onDietTypeSelected: (DietType) -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopTick: Int = 0,
) {
    // RF-MOB6: tocar la pestaña activa devuelve el contenido a su primera posición.
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) scrollState.scrollTo(0)
    }

    // El modo edición es UI pura: habilita el borrado por chip y se pierde al
    // vaciarse la sección (vuelve a requerir «Editar»).
    var editingAllergies by remember { mutableStateOf(false) }
    var editingIntolerances by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag(MyProfileTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            RestrictionSectionCard(
                section = ProfileSection.ALLERGY,
                title = stringResource(R.string.my_profile_allergies_title),
                hint = stringResource(R.string.my_profile_allergies_hint),
                chips = uiState.profile.allergies,
                none = uiState.profile.allergiesNone,
                input = uiState.allergyInput,
                editing = editingAllergies && uiState.profile.allergies.isNotEmpty(),
                onInputChange = onAllergyInputChange,
                onAdd = onAddAllergy,
                onRemove = onRemoveAllergy,
                onNoneChange = onAllergyNoneChange,
                onEditingChange = { editingAllergies = it },
            )
            RestrictionSectionCard(
                section = ProfileSection.INTOLERANCE,
                title = stringResource(R.string.my_profile_intolerances_title),
                hint = stringResource(R.string.my_profile_intolerances_hint),
                chips = uiState.profile.intolerances,
                none = uiState.profile.intolerancesNone,
                input = uiState.intoleranceInput,
                editing = editingIntolerances && uiState.profile.intolerances.isNotEmpty(),
                onInputChange = onIntoleranceInputChange,
                onAdd = onAddIntolerance,
                onRemove = onRemoveIntolerance,
                onNoneChange = onIntoleranceNoneChange,
                onEditingChange = { editingIntolerances = it },
            )
            DietTypeSectionCard(
                selected = uiState.profile.dietType,
                onSelected = onDietTypeSelected,
            )
        }
    }
}

// Tarjeta de una sección de restricciones: chips píldora (con borrado solo en
// modo edición), fila «No tengo ninguna» y alta por texto con «Añadir» (acción
// IME). Con «No tengo ninguna» marcada el alta queda bloqueada (RD25/D3).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestrictionSectionCard(
    section: ProfileSection,
    title: String,
    hint: String,
    chips: List<String>,
    none: Boolean,
    input: String,
    editing: Boolean,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onNoneChange: (Boolean) -> Unit,
    onEditingChange: (Boolean) -> Unit,
) {
    val inputTag = if (section == ProfileSection.ALLERGY) {
        MyProfileTestTags.INPUT_ALLERGIES
    } else {
        MyProfileTestTags.INPUT_INTOLERANCES
    }
    val addTag = if (section == ProfileSection.ALLERGY) {
        MyProfileTestTags.ADD_ALLERGIES
    } else {
        MyProfileTestTags.ADD_INTOLERANCES
    }
    val noneTag = if (section == ProfileSection.ALLERGY) {
        MyProfileTestTags.NONE_ALLERGIES
    } else {
        MyProfileTestTags.NONE_INTOLERANCES
    }
    val editTag = if (section == ProfileSection.ALLERGY) {
        MyProfileTestTags.EDIT_ALLERGIES
    } else {
        MyProfileTestTags.EDIT_INTOLERANCES
    }
    val sectionTag = if (section == ProfileSection.ALLERGY) {
        MyProfileTestTags.SECTION_ALLERGIES
    } else {
        MyProfileTestTags.SECTION_INTOLERANCES
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(WarmSpacing.md)
                .testTag(sectionTag),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                ) {
                    chips.forEachIndexed { index, chip ->
                        ProfileChip(
                            section = section,
                            index = index,
                            label = chip,
                            editing = editing,
                            onRemove = { onRemove(chip) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onEditingChange(!editing) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag(editTag),
                    ) {
                        Text(
                            stringResource(
                                if (editing) R.string.my_profile_edit_done else R.string.my_profile_edit,
                            ),
                        )
                    }
                }
            }
            NoneRow(
                checked = none,
                tag = noneTag,
                onCheckedChange = onNoneChange,
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                enabled = !none,
                placeholder = { Text(hint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (!none && input.isNotBlank()) onAdd() },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(inputTag),
            )
            Button(
                onClick = onAdd,
                enabled = !none && input.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(addTag),
            ) {
                Text(stringResource(R.string.my_profile_add))
            }
        }
    }
}

// Chip píldora de una restricción. Solo en modo edición muestra el borrado
// (icono terracota, patrón del Escáner) tras habilitar «Editar» (S1).
@Composable
private fun ProfileChip(
    section: ProfileSection,
    index: Int,
    label: String,
    editing: Boolean,
    onRemove: () -> Unit,
) {
    AssistChip(
        onClick = { if (editing) onRemove() },
        label = { Text(label) },
        trailingIcon = if (editing) {
            {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.my_profile_remove_chip, label),
                    modifier = Modifier
                        .size(18.dp)
                        .testTag(MyProfileTestTags.removeChip(section, index)),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        modifier = Modifier.testTag(MyProfileTestTags.chip(section, index)),
    )
}

// Fila «No tengo ninguna» (RD25/D3): al marcarla se limpian las chips de la
// sección y el alta queda bloqueada; desmarcarla devuelve el alta.
@Composable
private fun NoneRow(
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = stringResource(R.string.my_profile_none),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = WarmSpacing.sm),
        )
    }
}

// Tarjeta «Tipo de alimentación»: cinco chips de selección única sin
// preselección inicial (D2); tocar uno lo fija (la nueva elección sustituye).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietTypeSectionCard(
    selected: DietType?,
    onSelected: (DietType) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(WarmSpacing.md)
                .testTag(MyProfileTestTags.SECTION_DIET),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.my_profile_diet_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
            ) {
                DietType.entries.forEach { dietType ->
                    FilterChip(
                        selected = selected == dietType,
                        onClick = { onSelected(dietType) },
                        label = { Text(stringResource(dietType.labelRes())) },
                        modifier = Modifier.testTag(MyProfileTestTags.dietOption(dietType)),
                    )
                }
            }
        }
    }
}

// Etiquetas visibles de los tipos de alimentación (convención de `MealMoment`:
// `labelRes()` privado en la pantalla, textos en strings.xml con paridad es/en).
private fun DietType.labelRes(): Int = when (this) {
    DietType.OMNIVORE -> R.string.my_profile_diet_omnivore
    DietType.PESCETARIAN -> R.string.my_profile_diet_pescetarian
    DietType.VEGETARIAN -> R.string.my_profile_diet_vegetarian
    DietType.VEGAN -> R.string.my_profile_diet_vegan
    DietType.KETOGENIC -> R.string.my_profile_diet_ketogenic
}
