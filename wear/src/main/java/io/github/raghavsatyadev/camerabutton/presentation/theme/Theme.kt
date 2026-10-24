package io.github.raghavsatyadev.camerabutton.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/** Brand scheme used when the watch offers no dynamic colours: a warm shutter-orange primary. */
private val BrandColors =
  ColorScheme(
    primary = Color(0xFFFFB68A),
    primaryDim = Color(0xFFF2A273),
    primaryContainer = Color(0xFF7A3000),
    onPrimary = Color(0xFF522000),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE6BEAB),
    secondaryContainer = Color(0xFF5D4031),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFC9C98F),
    tertiaryContainer = Color(0xFF484A1E),
    onTertiaryContainer = Color(0xFFE6E6A9),
  )

@Composable
fun CameraButtonTheme(content: @Composable () -> Unit) {
  val colors = dynamicColorScheme(LocalContext.current) ?: BrandColors
  MaterialTheme(colorScheme = colors, content = content)
}
