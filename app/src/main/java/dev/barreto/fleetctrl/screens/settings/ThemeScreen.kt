package dev.barreto.fleetctrl.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.PaddingValues
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme
import dev.barreto.fleetctrl.ui.theme.ColorPalette
import dev.barreto.fleetctrl.ui.theme.getAvailablePalettes
import dev.barreto.fleetctrl.ui.theme.getAvailablePalettesDark
import dev.barreto.fleetctrl.viewmodels.ThemeViewModel

@Composable
fun ThemeScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTheme = themeViewModel.selectedTheme
    val selectedPalette = themeViewModel.selectedPalette

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val contentBottom = navBottomPadding + if (screenHeight < 640) 56.dp else 96.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = contentBottom)
    ) {
        item {
            Text(
                text = stringResource(R.string.theme_choose_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.theme_choose_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(getThemeOptions()) { themeOption ->
            ThemeOptionItem(
                option = themeOption,
                isSelected = selectedTheme == themeOption.id,
                onSelect = { themeViewModel.setTheme(themeOption.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = stringResource(R.string.theme_color_palette_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.theme_color_palette_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(getAvailablePalettes()) { palette ->
            ColorPaletteItem(
                palette = palette,
                isSelected = selectedPalette?.type == palette.type,
                onSelect = { themeViewModel.setPalette(palette) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Informação sobre aplicação do tema
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_applied_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.theme_applied_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    option: ThemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun ColorPaletteItem(
    palette: ColorPalette,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // Retângulo "picotado" com as cores da paleta (estilo Coolors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() },
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Para paleta do sistema, mostrar ícone especial
        if (palette.type.name == "SYSTEM") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = stringResource(R.string.cd_palette_system),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Mostrar todas as 12 cores da paleta
            val colors = listOf(
                palette.primary,
                palette.primaryVariant,
                palette.secondary,
                palette.secondaryVariant ?: palette.secondary,
                palette.tertiary,
                palette.tertiaryVariant ?: palette.tertiary,
                palette.surface,
                palette.surfaceVariant ?: palette.surface,
                palette.outline ?: palette.outlineVariant ?: palette.primary,
                palette.outlineVariant ?: palette.outline ?: palette.secondary,
                palette.background,
                palette.onPrimary
            )
            
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}


private data class ThemeOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun getThemeOptions(): List<ThemeOption> = listOf(
    ThemeOption(
        id = "light",
        title = "Claro",
        description = "Tema claro para uso durante o dia",
        icon = Icons.Default.LightMode
    ),
    ThemeOption(
        id = "dark",
        title = "Escuro",
        description = "Tema escuro para uso à noite",
        icon = Icons.Default.DarkMode
    ),
    ThemeOption(
        id = "system",
        title = "Automático",
        description = "Segue as configurações do sistema",
        icon = Icons.Default.Settings
    )
)

@Preview(showBackground = true)
@Composable
private fun ThemeScreenPreview() {
    FleetCtrlTheme {
        // Preview sem ThemeViewModel para evitar erros
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.theme_choose_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Text(
                    text = stringResource(R.string.theme_choose_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(getThemeOptions()) { themeOption ->
                ThemeOptionItem(
                    option = themeOption,
                    isSelected = themeOption.id == "system",
                    onSelect = { }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(
                    text = stringResource(R.string.theme_color_palette_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(getAvailablePalettes()) { palette ->
                ColorPaletteItem(
                    palette = palette,
                    isSelected = palette.type.name == "SYSTEM",
                    onSelect = { }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeScreenWithBluePalettePreview() {
    FleetCtrlTheme(
        colorPalette = getAvailablePalettes().find { it.type.name == "BLUE" }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.preview_blue_palette),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(getThemeOptions()) { themeOption ->
                ThemeOptionItem(
                    option = themeOption,
                    isSelected = themeOption.id == "system",
                    onSelect = { }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeScreenWithNeutralPalettePreview() {
    FleetCtrlTheme(
        colorPalette = getAvailablePalettes().find { it.type.name == "NEUTRAL" }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.preview_neutral_palette),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(getThemeOptions()) { themeOption ->
                ThemeOptionItem(
                    option = themeOption,
                    isSelected = themeOption.id == "system",
                    onSelect = { }
                )
            }
        }
    }
}

// Função de extensão para converter Color para hexadecimal
private fun Color.toHex(): String {
    val argb = this.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}
