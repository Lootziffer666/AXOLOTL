package app.axolotl

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import app.axolotl.data.DockEdge
import app.axolotl.data.ClipboardCaptureState
import app.axolotl.ui.DockViewModel
import app.axolotl.ui.SettingsViewModel
import app.axolotl.ui.theme.MyApplicationTheme
import app.axolotl.utils.HandoffHelper

class BorderlineActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val dockViewModel: DockViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()

                val appsState by dockViewModel.installedAppsState.collectAsState()
                val featuresState by dockViewModel.featuresState.collectAsState()
                val snippetsState by dockViewModel.snippetsState.collectAsState()
                val clipsState by dockViewModel.clipsState.collectAsState()
                val settings by settingsViewModel.settingsState.collectAsState()

                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                var selectedTab by remember { mutableIntStateOf(0) } // 0: Dock Setup, 1: Snippet Capsule, 2: Clipboard+, 3: Provisioning

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasOverlayPermission = Settings.canDrawOverlays(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0E17), Color(0xFF1E1B2E), Color(0xFF12111C))
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundGradient)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 44.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Borderline",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF8FAFC)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFA855F7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("4 Edge Menus", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Text(
                                    "Anthracite & Electric Violet Overlay System",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFC084FC)
                                )
                            }

                            if (settings.emergencyOff) {
                                Text("EMERGENCY OFF", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                        }

                        // Navigation Tabs
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFA855F7),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("1. Dock Setup", modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 0) Color(0xFFC084FC) else Color(0xFFA1A1AA))
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("2. Snippets", modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 1) Color(0xFFC084FC) else Color(0xFFA1A1AA))
                            }
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                                Text("3. Clipboard+", modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 2) Color(0xFFC084FC) else Color(0xFFA1A1AA))
                            }
                            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                                Text("4. Provisioning", modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 3) Color(0xFFC084FC) else Color(0xFFA1A1AA))
                            }
                            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }) {
                                Text("5. Logcat Reader", modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 4) Color(0xFFC084FC) else Color(0xFFA1A1AA))
                            }
                        }

                        // Tab Contents
                        when (selectedTab) {
                            0 -> DockSetupTab(appsState, featuresState, settings, hasOverlayPermission, dockViewModel, settingsViewModel)
                            1 -> SnippetsManagerTab(snippetsState, dockViewModel)
                            2 -> ClipboardManagerTab(clipsState, settings, settingsViewModel, dockViewModel)
                            3 -> ProvisioningTab(settings, settingsViewModel, dockViewModel)
                            4 -> LogcatReaderTab(settingsManager = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockSetupTab(
    appsState: List<app.axolotl.ui.AppInfo>,
    featuresState: List<app.axolotl.ui.FeatureInfo>,
    settings: app.axolotl.data.DockSettings,
    hasOverlayPermission: Boolean,
    dockViewModel: DockViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overlay Status Card
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Overlay Permission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                            Text("Required to draw the 4 edge menus over apps", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                        }
                        Spacer(Modifier.weight(1f))
                        if (hasOverlayPermission) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = Color(0xFF22C55E))
                        } else {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (hasOverlayPermission) {
                                context.startForegroundService(Intent(context, DockOverlayService::class.java))
                                Toast.makeText(context, "Borderline 4 Edge Menus Active!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = hasOverlayPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Text("Start / Restart 4-Menu Service")
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("4 Edge Handles Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(4.dp))
                    Text("Tapping any of the 4 floating buttons on screen edge summons its dedicated menu directly!", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF))
                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F0E17))
                            .border(BorderStroke(1.dp, Color(0x33A855F7)), RoundedCornerShape(16.dp))
                    ) {
                        val isLeft = settings.edge == DockEdge.LEFT
                        val handleAlign = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

                        Column(
                            modifier = Modifier
                                .align(handleAlign)
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreviewHandle(Icons.Filled.Apps, "1. Dock", Color(0xFFA855F7))
                            PreviewHandle(Icons.AutoMirrored.Filled.Assignment, "2. Snippets", Color(0xFFC084FC))
                            PreviewHandle(Icons.AutoMirrored.Filled.FormatListBulleted, "3. Clipboard", Color(0xFFE9D5FF))
                            PreviewHandle(Icons.Filled.FlashOn, "4. Actions", Color(0xFF7E22CE))
                        }
                    }
                }
            }
        }

        // Appearance Controls
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Screen Edge Position & Scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Right Edge", color = Color(0xFFF8FAFC))
                        Switch(
                            checked = settings.edge == DockEdge.LEFT,
                            onCheckedChange = { isLeft ->
                                settingsViewModel.updateSettings(settings.copy(
                                    edge = if (isLeft) DockEdge.LEFT else DockEdge.RIGHT
                                ))
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFA855F7),
                                checkedTrackColor = Color(0xFF381F52)
                            )
                        )
                        Text("Left Edge", color = Color(0xFFF8FAFC))
                    }

                    Spacer(Modifier.height(12.dp))
                    SettingsSlider("Handle / Trigger Size", settings.dockSize, 0.6f..1.6f) {
                        settingsViewModel.updateSettings(settings.copy(dockSize = it))
                    }
                    SettingsSlider("Glass Panel Opacity", settings.dockOpacity, 0.3f..1.0f) {
                        settingsViewModel.updateSettings(settings.copy(dockOpacity = it))
                    }
                }
            }
        }

        // System Features Selection
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Quick Actions for Menu 1 (Apps Dock)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(4.dp))
                    Text("Pin system functions directly into your primary overlay dock alongside apps", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                    Spacer(Modifier.height(12.dp))

                    featuresState.forEach { feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(feature.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                                Text(feature.description, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                            }
                            Switch(
                                checked = feature.isSelected,
                                onCheckedChange = { dockViewModel.toggleFeature(feature) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                            )
                        }
                    }
                }
            }
        }

        // Installed Apps Selection
        item {
            Text(
                "Select Apps for Menu 1 (${appsState.count { it.isSelected }} active)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
        }

        items(appsState) { app ->
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pm = context.packageManager
                    val icon = try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { null }

                    if (icon != null) {
                        AsyncImage(model = icon, contentDescription = app.name, modifier = Modifier.size(38.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = app.packageName, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Switch(
                        checked = app.isSelected,
                        onCheckedChange = { dockViewModel.toggleApp(app) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewHandle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1B2E))
                .border(BorderStroke(1.dp, color), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SnippetsManagerTab(
    snippetsState: List<app.axolotl.data.SnippetEntity>,
    dockViewModel: DockViewModel
) {
    val context = LocalContext.current
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Prompt") }

    val categories = listOf("Prompt", "Markdown", "Rule", "Template", "Recovery")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add New Snippet Capsule (Menu 2)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Snippet Title", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("Prompt / Code / Markdown Text Block", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Category:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF8FAFC))
                        categories.forEach { cat ->
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == cat) Color(0xFFC084FC) else Color(0xFFA1A1AA),
                                modifier = Modifier
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (contentInput.isNotBlank()) {
                                dockViewModel.addSnippet(titleInput, contentInput, selectedCategory)
                                titleInput = ""
                                contentInput = ""
                                Toast.makeText(context, "Snippet added to Menu 2!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                        Spacer(Modifier.width(8.dp))
                        Text("Save Snippet Capsule")
                    }
                }
            }
        }

        item {
            Text("Existing Snippet Capsules (${snippetsState.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
        }

        items(snippetsState) { snippet ->
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(snippet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), modifier = Modifier.weight(1f))
                        Text(snippet.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(snippet.content, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE9D5FF), maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = {
                            HandoffHelper.copyToClipboard(context, snippet.title, snippet.content)
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFFC084FC))
                        }
                        IconButton(onClick = {
                            HandoffHelper.sendToAI(context, snippet.content)
                        }) {
                            Icon(Icons.Filled.Psychology, contentDescription = "Send to AI", tint = Color(0xFFC084FC))
                        }
                        IconButton(onClick = {
                            dockViewModel.deleteSnippet(snippet)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFA1A1AA))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardManagerTab(
    clipsState: List<app.axolotl.data.ClipEntity>,
    settings: app.axolotl.data.DockSettings,
    settingsViewModel: SettingsViewModel,
    dockViewModel: DockViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appendix Clipboard Card
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Appendix Clipboard Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                            Text("Auto-appends every copied snippet into a single research list", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                        }
                        Switch(
                            checked = settings.appendixMode,
                            onCheckedChange = { active ->
                                settingsViewModel.updateSettings(settings.copy(appendixMode = active))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                        )
                    }

                    if (settings.appendixDraft.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Active Research List:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF262338), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(settings.appendixDraft, style = MaterialTheme.typography.bodySmall, color = Color(0xFFF8FAFC))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    HandoffHelper.copyToClipboard(context, "Appendix List", settings.appendixDraft)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Full List")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    settingsViewModel.updateSettings(settings.copy(appendixDraft = ""))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381F52))
                            ) {
                                Text("Clear List")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clipboard History (${clipsState.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                if (clipsState.isNotEmpty()) {
                    Button(
                        onClick = { dockViewModel.clearUnpinnedClips() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381F52))
                    ) {
                        Text("Clear History")
                    }
                }
            }
        }

        items(clipsState) { clip ->
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(clip.contentType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC), modifier = Modifier.weight(1f))
                        Text("${clip.charCount} chars", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(clip.content, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE9D5FF), maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = {
                            HandoffHelper.copyToClipboard(context, "Clip", clip.content)
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFFC084FC))
                        }
                        IconButton(onClick = {
                            HandoffHelper.shareText(context, clip.content)
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color(0xFFC084FC))
                        }
                        IconButton(onClick = {
                            dockViewModel.deleteClip(clip)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFA1A1AA))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProvisioningTab(
    settings: app.axolotl.data.DockSettings,
    settingsViewModel: SettingsViewModel,
    dockViewModel: DockViewModel
) {
    val context = LocalContext.current
    val clipboardCaptureStatus by settingsViewModel.clipboardCaptureStatus.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Emergency Off", tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Emergency Off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                            Text("Instantly stops all overlay edge handles and background clipboard monitoring", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                        }
                        Switch(
                            checked = settings.emergencyOff,
                            onCheckedChange = { off ->
                                settingsViewModel.updateSettings(settings.copy(emergencyOff = off))
                                Toast.makeText(context, if (off) "Emergency Off Active" else "Borderline Active", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444))
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Clipboard History", tint = Color(0xFFC084FC), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clipboard History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                            Text(
                                "Off by default · keeps up to 50 clips for 30 days",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                            Text(
                                clipboardCaptureStatus.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (clipboardCaptureStatus.state) {
                                    ClipboardCaptureState.BLOCKED -> Color(0xFFEF4444)
                                    ClipboardCaptureState.FAILED -> Color(0xFFEF4444)
                                    ClipboardCaptureState.CAPTURED -> Color(0xFF4ADE80)
                                    ClipboardCaptureState.IDLE -> Color(0xFFA1A1AA)
                                }
                            )
                        }
                        Switch(
                            checked = settings.clipboardHistoryEnabled,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateSettings(
                                    settings.copy(clipboardHistoryEnabled = enabled)
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = "Private Mode", tint = Color(0xFFA855F7), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Private Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                            Text("Pause recording newly copied text into clipboard history", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                        }
                        Switch(
                            checked = settings.privateMode,
                            onCheckedChange = { privateActive ->
                                settingsViewModel.updateSettings(settings.copy(privateMode = privateActive))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Integration Code Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(4.dp))
                    Text("Use this code snippet in your other apps to invoke Borderline menus, send snippets or append research items directly:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF))
                    Spacer(Modifier.height(10.dp))

                    val snippetCode = """// --- Borderline Integration Helper ---
object BorderlineSDK {
    // 1. Open specific Borderline Menu (0: Dock, 1: Snippets, 2: Clipboard, 3: Actions)
    fun openMenu(context: Context, menuIndex: Int = 0) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("borderline://open?menu=" + menuIndex))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 2. Add Snippet directly to Borderline Snippet Capsule
    fun addSnippet(context: Context, title: String, content: String, category: String = "Prompt") {
        val uri = Uri.Builder()
            .scheme("borderline")
            .authority("add-snippet")
            .appendQueryParameter("title", title)
            .appendQueryParameter("content", content)
            .appendQueryParameter("category", category)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    // 3. Append text directly to Borderline Appendix Research List
    fun appendToAppendix(context: Context, text: String) {
        val uri = Uri.Builder()
            .scheme("borderline")
            .authority("appendix")
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}"""

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0E17), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(snippetCode, fontSize = 11.sp, color = Color(0xFFC084FC), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            HandoffHelper.copyToClipboard(context, "Borderline Integration SDK", snippetCode)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy SDK")
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Integration Code")
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Maintenance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(8.dp))
                    Text("All data is strictly offline and stored locally inside Room SQLite databases.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            dockViewModel.clearUnpinnedClips()
                            settingsViewModel.updateSettings(settings.copy(appendixDraft = ""))
                            Toast.makeText(context, "Clipboard history cleared!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381F52))
                    ) {
                        Text("Delete Clipboard History")
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B2A)),
        border = BorderStroke(1.dp, Color(0x33A855F7))
    ) {
        content()
    }
}

@Composable
fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF8FAFC))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFA855F7),
                activeTrackColor = Color(0xFFC084FC),
                inactiveTrackColor = Color(0xFF262338)
            )
        )
    }
}

@Composable
fun LogcatReaderTab(settingsManager: SettingsViewModel) {
    val context = LocalContext.current
    var tagFilter by remember { mutableStateOf("") }
    var logsText by remember { mutableStateOf("") }
    var isGranted by remember { mutableStateOf(app.axolotl.utils.LogReaderHelper.hasReadLogsPermission(context)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("READ_LOGS Permission & ADB Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(4.dp))
                    Text("On Android, READ_LOGS requires a 1-time grant via ADB command:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA))
                    Spacer(Modifier.height(8.dp))

                    val adbCmd = "adb shell pm grant ${context.packageName} android.permission.READ_LOGS"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0E17), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(adbCmd, fontSize = 11.sp, color = Color(0xFFC084FC), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                HandoffHelper.copyToClipboard(context, "ADB Command", adbCmd)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                            Spacer(Modifier.width(6.dp))
                            Text("Copy ADB Command")
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Logcat Inspector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tagFilter,
                        onValueChange = { tagFilter = it },
                        label = { Text("Filter Tag / Substring (e.g. Error, MyApp)", color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0x33A855F7),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                logsText = app.axolotl.utils.LogReaderHelper.readRecentLogs(tagFilter, 150)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                        ) {
                            Text("Fetch Recent Logs")
                        }

                        Button(
                            onClick = {
                                app.axolotl.utils.LogReaderHelper.clearLogcat()
                                logsText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381F52))
                        ) {
                            Text("Clear")
                        }
                    }

                    if (logsText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF0F0E17), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = logsText,
                                        fontSize = 10.sp,
                                        color = Color(0xFFE9D5FF),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                HandoffHelper.copyToClipboard(context, "Logcat Output", logsText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262338))
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Logs")
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Logs to Clipboard")
                        }
                    }
                }
            }
        }
    }
}
