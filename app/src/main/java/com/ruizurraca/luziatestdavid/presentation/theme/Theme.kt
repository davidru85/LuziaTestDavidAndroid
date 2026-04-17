package com.ruizurraca.luziatestdavid.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LuziaLightColorScheme = lightColorScheme(
    primary = LuziaPrimaryLight,
    onPrimary = LuziaOnPrimaryLight,
    primaryContainer = LuziaPrimaryContainerLight,
    onPrimaryContainer = LuziaOnPrimaryContainerLight,
    secondary = LuziaSecondaryLight,
    onSecondary = LuziaOnSecondaryLight,
    secondaryContainer = LuziaSecondaryContainerLight,
    onSecondaryContainer = LuziaOnSecondaryContainerLight,
    tertiary = LuziaTertiaryLight,
    onTertiary = LuziaOnTertiaryLight,
    tertiaryContainer = LuziaTertiaryContainerLight,
    onTertiaryContainer = LuziaOnTertiaryContainerLight,
    background = LuziaBackgroundLight,
    onBackground = LuziaOnBackgroundLight,
    surface = LuziaSurfaceLight,
    onSurface = LuziaOnSurfaceLight,
    surfaceVariant = LuziaSurfaceVariantLight,
    onSurfaceVariant = LuziaOnSurfaceVariantLight,
    error = LuziaErrorLight,
    onError = LuziaOnErrorLight
)

private val LuziaDarkColorScheme = darkColorScheme(
    primary = LuziaPrimaryDark,
    onPrimary = LuziaOnPrimaryDark,
    primaryContainer = LuziaPrimaryContainerDark,
    onPrimaryContainer = LuziaOnPrimaryContainerDark,
    secondary = LuziaSecondaryDark,
    onSecondary = LuziaOnSecondaryDark,
    secondaryContainer = LuziaSecondaryContainerDark,
    onSecondaryContainer = LuziaOnSecondaryContainerDark,
    tertiary = LuziaTertiaryDark,
    onTertiary = LuziaOnTertiaryDark,
    tertiaryContainer = LuziaTertiaryContainerDark,
    onTertiaryContainer = LuziaOnTertiaryContainerDark,
    background = LuziaBackgroundDark,
    onBackground = LuziaOnBackgroundDark,
    surface = LuziaSurfaceDark,
    onSurface = LuziaOnSurfaceDark,
    surfaceVariant = LuziaSurfaceVariantDark,
    onSurfaceVariant = LuziaOnSurfaceVariantDark,
    error = LuziaErrorDark,
    onError = LuziaOnErrorDark
)

@Composable
fun LuziaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LuziaDarkColorScheme
        else -> LuziaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LuziaTypography,
        content = content
    )
}
