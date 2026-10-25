package com.surajdev.spendwise.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.surajdev.spendwise.resources.Res
import com.surajdev.spendwise.resources.fraunces_medium
import com.surajdev.spendwise.resources.fraunces_regular
import com.surajdev.spendwise.resources.fraunces_semibold
import com.surajdev.spendwise.resources.inter_medium
import com.surajdev.spendwise.resources.inter_regular
import com.surajdev.spendwise.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/** Fraunces: soft serif for display text and hero amounts. */
@Composable
internal fun frauncesFamily() = FontFamily(
    Font(Res.font.fraunces_regular, FontWeight.Normal),
    Font(Res.font.fraunces_medium, FontWeight.Medium),
    Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
)

/** Inter: the UI face. Supports tabular figures for aligned amounts. */
@Composable
internal fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

@Composable
internal fun spendWiseTypography(): Typography {
    val serif = frauncesFamily()
    val sans = interFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.01).em),
        displayMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.01).em),
        displaySmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
        labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em),
        labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.em),
    )
}

/** Digits of equal width, so amounts line up in lists. Inter supports this; Fraunces ignores it. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Small-caps-style section label: uppercase with wide tracking. */
fun TextStyle.overline(): TextStyle = copy(letterSpacing = 0.12.em)
