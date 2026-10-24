package com.fedoratube.org.ui.screens.movies

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import com.fedoratube.org.data.movies.MovieRepository
import com.fedoratube.org.data.movies.StremioAddon
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonsSettingsScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { MovieRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val installedAddons by repository.installedAddons.collectAsState()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var addonUrlInput by rememberSaveable { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }

    fun installAddon(url: String) {
        if (url.isBlank()) return
        isInstalling = true
        scope.launch {
            val result = repository.addAddon(url.trim())
            isInstalling = false
            result.fold(
                onSuccess = { addon ->
                    Toast.makeText(context, "Installed: ${addon.name}", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                    addonUrlInput = ""
                },
                onFailure = { error ->
                    Toast.makeText(context, error.message ?: "Failed to install addon", Toast.LENGTH_LONG).show()
                },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.settings_addons_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Add Addon Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Media Addons & Plugins",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = "Add media catalogs, stream scrapers, or subtitle addons using any manifest URL or link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.addons_add))
                        }
                    }
                }
            }

            // Installed Addons Header
            item {
                Text(
                    text = "${stringResource(R.string.addons_installed)} (${installedAddons.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Installed Addons List
            items(installedAddons, key = { it.id }) { addon ->
                InstalledAddonCard(
                    addon = addon,
                    onToggle = { enabled -> repository.toggleAddon(addon.id, enabled) },
                    onDelete = { repository.removeAddon(addon.id) },
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isInstalling) showAddDialog = false
            },
            title = { Text(stringResource(R.string.addons_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the addon's manifest.json URL:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = addonUrlInput,
                        onValueChange = { addonUrlInput = it },
                        placeholder = { Text("https://.../manifest.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isInstalling,
                    )
                    if (isInstalling) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { installAddon(addonUrlInput) },
                    enabled = addonUrlInput.isNotBlank() && !isInstalling,
                ) {
                    Text(stringResource(R.string.addons_install))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isInstalling,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun InstalledAddonCard(
    addon: StremioAddon,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = addon.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "v${addon.version}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                if (addon.description.isNotBlank()) {
                    Text(
                        text = addon.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                    )
                }
            }

            Switch(
                checked = addon.enabled,
                onCheckedChange = onToggle,
            )

            if (addon.isDeletable) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete addon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

