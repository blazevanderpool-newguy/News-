package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val PulseWireDarkColorScheme =
  darkColorScheme(
    primary = EditorialNavyDark,
    onPrimary = EditorialNavy,
    primaryContainer = EditorialNavyContainerDark,
    onPrimaryContainer = EditorialNavyLight,
    secondary = EditorialCoralDark,
    onSecondary = EditorialCrimson,
    secondaryContainer = EditorialCoralContainerDark,
    onSecondaryContainer = EditorialCrimsonSoft,
    tertiary = LocalEmerald,
    background = NewsroomObsidian,
    onBackground = AlabasterInk,
    surface = NewsroomSurface,
    onSurface = AlabasterInk,
    surfaceVariant = NewsroomSurfaceElevated,
    onSurfaceVariant = SlateInkSecondary,
    outline = NewsroomBorder,
    outlineVariant = NewsroomBorder,
  )

private val PulseWireLightColorScheme =
  lightColorScheme(
    primary = EditorialNavy,
    onPrimary = NewsprintCard,
    primaryContainer = EditorialNavyLight,
    onPrimaryContainer = EditorialNavy,
    secondary = EditorialCrimson,
    onSecondary = NewsprintCard,
    secondaryContainer = EditorialCrimsonSoft,
    onSecondaryContainer = EditorialCrimson,
    tertiary = LocalEmerald,
    background = NewsprintIvory,
    onBackground = InkPrimary,
    surface = NewsprintCard,
    onSurface = InkPrimary,
    surfaceVariant = NewsprintWarmSurface,
    onSurfaceVariant = InkSecondary,
    outline = NewsprintBorder,
    outlineVariant = NewsprintBorder,
  )

val PulseWireShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun PulseWireTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> PulseWireDarkColorScheme
      else -> PulseWireLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = PulseWireTypography,
    shapes = PulseWireShapes,
    content = content,
  )
}
