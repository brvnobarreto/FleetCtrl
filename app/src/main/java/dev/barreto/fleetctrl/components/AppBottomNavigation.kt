package dev.barreto.fleetctrl.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.navigation.Screen
import dev.barreto.fleetctrl.ui.theme.FleetCtrlTheme

@Composable
fun AppBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(340.dp)
            .height(72.dp)
            .shadow(12.dp, RoundedCornerShape(64.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(64.dp),
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                screen = Screen.DIARY,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(R.string.nav_diary),
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                modifier = Modifier.size(52.dp)
            )
            FloatingNavItem(
                screen = Screen.FUEL,
                icon = Icons.Default.LocalGasStation,
                label = stringResource(R.string.nav_fuel),
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                modifier = Modifier.size(52.dp)
            )
            FloatingNavItem(
                screen = Screen.HOME,
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                modifier = Modifier.size(52.dp)
            )
            FloatingNavItem(
                screen = Screen.MAINTENANCE,
                icon = Icons.Default.Build,
                label = stringResource(R.string.nav_maintenance),
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                modifier = Modifier.size(52.dp)
            )
            FloatingNavItem(
                screen = Screen.FLEET,
                icon = Icons.Default.DirectionsCar,
                label = stringResource(R.string.nav_fleet),
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    screen: Screen,
    icon: ImageVector,
    label: String,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentScreen == screen

    IconButton(
        onClick = { onNavigate(screen) },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomNavigationPreview() {
    FleetCtrlTheme {
        AppBottomNavigation(
            currentScreen = Screen.DIARY,
            onNavigate = { }
        )
    }
}
