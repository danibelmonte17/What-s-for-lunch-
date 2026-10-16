package com.whatsforlunch.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.whatsforlunch.app.R

// Tipografías del sistema «Warm Culinary Craft»: Plus Jakarta Sans (títulos y labels)
// e Inter (cuerpo). Se empaquetan como fuentes variables en res/font/ (sin red).

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

val WarmTypography = Typography(
    // display / display-mobile
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.02).em,
    ),
    // headline-lg / headline-md / headline-sm
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.015).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // body-lg / body-md / body-sm
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // label-lg / label-md / label-sm
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.02.em,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.em,
    ),
)
