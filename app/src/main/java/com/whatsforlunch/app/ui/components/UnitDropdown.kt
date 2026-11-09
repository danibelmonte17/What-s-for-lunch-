package com.whatsforlunch.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.IngredientUnit
import com.whatsforlunch.app.ui.theme.FieldShape
import com.whatsforlunch.app.ui.theme.warmFieldColors

/**
 * Desplegable de unidades de lista cerrada (D1) compartido por la fila de
 * Ingrediente del formulario y el diálogo de ítem de la Lista de la Compra.
 *
 * Las opciones son siempre los cinco valores del enum (ud, g, kg, ml, l) y las
 * filas nuevas nacen con «ud». Una unidad legada fuera de la lista —incluida la
 * vacía, rotulada «(sin unidad)»— se añade como **opción extra** seleccionada
 * para poder conservarla al editar (D1/D5); no existe entrada vacía nueva.
 *
 * Estética de los campos existentes (FieldShape + warmFieldColors, mínimo 48 dp)
 * y `testTag` estable en el campo; cada opción expone `"<testTag>_option_<valor>"`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    shape: Shape = FieldShape,
    colors: TextFieldColors = warmFieldColors(),
) {
    var expanded by remember(testTag) { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.unit_none)
    val options = remember(value) {
        IngredientUnit.entries.map { it.code } +
            listOfNotNull(value.takeIf { IngredientUnit.fromCode(it) == null })
    }
    val optionLabels = options.map { raw -> unitLabel(raw).ifBlank { noneLabel } }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            // La unidad vacía solo existe en filas legadas: se rotula para que el
            // desplegable nunca muestre un campo sin valor (D1). `singleLine` evita
            // que un valor largo (p. ej. «(sin unidad)») se parta en varias líneas y
            // estire el campo en la fila estrecha del formulario.
            value = unitLabel(value).ifBlank { noneLabel },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = label,
            trailingIcon = {
                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
            shape = shape,
            colors = colors,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(testTag),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // El menú hereda el ancho del ancla (estrecho en la fila de Ingrediente):
            // un mínimo evita que las opciones se partan en varias líneas.
            modifier = Modifier.widthIn(min = 160.dp),
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(optionLabels[index]) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag(optionTag(testTag, option)),
                )
            }
        }
    }
}

/**
 * Tag de una opción del desplegable: estable y sin ambigüedad aunque el campo
 * muestre el mismo texto (D1).
 */
fun optionTag(testTag: String, option: String): String =
    "${testTag}_option_${option.ifBlank { "none" }}"

/**
 * Presentación de la unidad en cualquier superficie (D4): el código del enum se
 * muestra con su clave es/en (`unit_ud`…`unit_l`) y la unidad legada queda como
 * texto tal cual (D5). La vacía se devuelve en blanco para que las líneas
 * «cantidad unidad nombre» la omitan (RD19); el desplegable la rotula aparte.
 */
@Composable
fun unitLabel(raw: String): String {
    val unit = IngredientUnit.fromCode(raw) ?: return raw.trim()
    return stringResource(unit.labelRes)
}

private val IngredientUnit.labelRes: Int
    get() = when (this) {
        IngredientUnit.UD -> R.string.unit_ud
        IngredientUnit.G -> R.string.unit_g
        IngredientUnit.KG -> R.string.unit_kg
        IngredientUnit.ML -> R.string.unit_ml
        IngredientUnit.L -> R.string.unit_l
    }
