package dev.barreto.fleetctrl.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.barreto.fleetctrl.ui.theme.ColorPalette
import dev.barreto.fleetctrl.ui.theme.getAdaptedPaletteForDarkMode

private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  secondary = PurpleGrey80,
  tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
  primary = Purple40,
  secondary = PurpleGrey40,
  tertiary = Pink40

  /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun FleetCtrlTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  colorPalette: ColorPalette? = null,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    // Prioridade 1: Paleta customizada (se selecionada)
    colorPalette != null -> {
      // Se for paleta do sistema, usar dynamic colors se disponível
      if (colorPalette.type.name == "SYSTEM" && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        // Usar paleta customizada com cores adaptadas para modo escuro
        val adaptedPalette = if (darkTheme) {
          getAdaptedPaletteForDarkMode(colorPalette)
        } else {
          colorPalette
        }
        
        if (darkTheme) {
          darkColorScheme(
            primary = adaptedPalette.primary,
            primaryContainer = adaptedPalette.primaryVariant,
            secondary = adaptedPalette.secondary,
            secondaryContainer = adaptedPalette.secondaryVariant ?: adaptedPalette.secondary,
            tertiary = adaptedPalette.tertiary,
            tertiaryContainer = adaptedPalette.tertiaryVariant ?: adaptedPalette.tertiary,
            surface = adaptedPalette.surface,
            surfaceVariant = adaptedPalette.surfaceVariant ?: adaptedPalette.surface,
            background = adaptedPalette.background,
            outline = adaptedPalette.outline ?: adaptedPalette.primary,
            outlineVariant = adaptedPalette.outlineVariant ?: adaptedPalette.secondary,
            onPrimary = adaptedPalette.onPrimary,
            onPrimaryContainer = adaptedPalette.onPrimary,
            onSecondary = adaptedPalette.onSecondary,
            onSecondaryContainer = adaptedPalette.onSecondary,
            onTertiary = adaptedPalette.onTertiary ?: adaptedPalette.onPrimary,
            onTertiaryContainer = adaptedPalette.onTertiary ?: adaptedPalette.onPrimary,
            onSurface = adaptedPalette.onSurface,
            onSurfaceVariant = adaptedPalette.onSurfaceVariant ?: adaptedPalette.onSurface,
            onBackground = adaptedPalette.onBackground
          )
        } else {
          lightColorScheme(
            primary = adaptedPalette.primary,
            primaryContainer = adaptedPalette.primaryVariant,
            secondary = adaptedPalette.secondary,
            secondaryContainer = adaptedPalette.secondaryVariant ?: adaptedPalette.secondary,
            tertiary = adaptedPalette.tertiary,
            tertiaryContainer = adaptedPalette.tertiaryVariant ?: adaptedPalette.tertiary,
            surface = adaptedPalette.surface,
            surfaceVariant = adaptedPalette.surfaceVariant ?: adaptedPalette.surface,
            background = adaptedPalette.background,
            outline = adaptedPalette.outline ?: adaptedPalette.primary,
            outlineVariant = adaptedPalette.outlineVariant ?: adaptedPalette.secondary,
            onPrimary = adaptedPalette.onPrimary,
            onPrimaryContainer = adaptedPalette.onPrimary,
            onSecondary = adaptedPalette.onSecondary,
            onSecondaryContainer = adaptedPalette.onSecondary,
            onTertiary = adaptedPalette.onTertiary ?: adaptedPalette.onPrimary,
            onTertiaryContainer = adaptedPalette.onTertiary ?: adaptedPalette.onPrimary,
            onSurface = adaptedPalette.onSurface,
            onSurfaceVariant = adaptedPalette.onSurfaceVariant ?: adaptedPalette.onSurface,
            onBackground = adaptedPalette.onBackground
          )
        }
      }
    }
    
    // Prioridade 2: Dynamic color (Android 12+)
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    // Prioridade 3: Tema padrão baseado no darkTheme
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      val insetsController = WindowCompat.getInsetsController(window, view)
      
      // Usar a API moderna para cores das barras do sistema
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}