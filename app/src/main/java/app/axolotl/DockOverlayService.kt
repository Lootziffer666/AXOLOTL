package app.axolotl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import app.axolotl.data.ClipEntity
import app.axolotl.data.ControlMode
import app.axolotl.data.DockDatabase
import app.axolotl.data.DockEdge
import app.axolotl.data.DockRepository
import app.axolotl.data.SettingsManager
import app.axolotl.data.SnippetEntity
import app.axolotl.ui.theme.MyApplicationTheme
import app.axolotl.utils.HandoffHelper
import app.axolotl.utils.QuickActionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DockOverlayService : Service() {

    companion object {
        const val ACTION_OPEN_MENU = "app.axolotl.borderline.OPEN_MENU"
        const val EXTRA_MENU_INDEX = "menu_index"
    }

    private val _requestedMenu = kotlinx.coroutines.flow.MutableStateFlow(-1)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_OPEN_MENU) {
            val index = intent.getIntExtra(EXTRA_MENU_INDEX, 0)
            _requestedMenu.value = index
        }
        return START_STICKY
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    @Suppress("DEPRECATION")
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ServiceLifecycleOwner()

        val database = DockDatabase.getDatabase(this)
        val repository = DockRepository(
            database.dockAppDao(),
            database.dockItemDao(),
            database.snippetDao(),
            database.clipDao()
        )
        val settingsManager = SettingsManager.getInstance(this)

        // Clipboard Listener for Clipboard+ & Appendix Clipboard
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            val settings = settingsManager.settings.value
            if (settings.emergencyOff || settings.privateMode) return@OnPrimaryClipChangedListener

            try {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString()
                    if (!text.isNullOrBlank()) {
                        serviceScope.launch(Dispatchers.IO) {
                            val contentType = if (text.startsWith("http://") || text.startsWith("https://")) "LINK"
                            else if (text.contains("#") || text.contains("```") || text.contains("- ")) "MARKDOWN"
                            else "TEXT"

                            repository.insertClip(
                                ClipEntity(
                                    content = text,
                                    contentType = contentType,
                                    charCount = text.length
                                )
                            )

                            if (settings.appendixMode) {
                                settingsManager.appendToAppendix(text)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Background clipboard restriction on newer Android versions
            }
        }
        clipboard.addPrimaryClipChangedListener(clipboardListener)

        startForegroundService()

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    val settings by settingsManager.settings.collectAsState()
                    if (settings.emergencyOff) return@MyApplicationTheme

                    val dockApps by repository.allDockApps
                        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())
                        .collectAsState()

                    val dockItems by repository.allDockItems
                        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())
                        .collectAsState()

                    val snippets by repository.allSnippets
                        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())
                        .collectAsState()

                    val clips by repository.allClips
                        .stateIn(serviceScope, SharingStarted.Eagerly, emptyList())
                        .collectAsState()

                    var activeMenu by remember { mutableIntStateOf(-1) } // -1: Closed, 0: Apps Dock, 1: Snippets, 2: Clipboard+, 3: QuickActions & Capture

                    val requestedMenu by _requestedMenu.collectAsState()
                    androidx.compose.runtime.LaunchedEffect(requestedMenu) {
                        if (requestedMenu != -1) {
                            activeMenu = requestedMenu
                            _requestedMenu.value = -1
                        }
                    }

                    val isLeft = settings.edge == DockEdge.LEFT

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 4 Floating Dock Edge Handles (Individual Callables)
                        val edgeAlign = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

                        Column(
                            modifier = Modifier
                                .align(edgeAlign)
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End
                        ) {
                            // Menu 1 Handle: Apps Dock
                            EdgeMenuHandle(
                                icon = Icons.Filled.Apps,
                                label = "Dock",
                                color = Color(0xFFA855F7),
                                sizeMultiplier = settings.dockSize,
                                isLeft = isLeft
                            ) {
                                activeMenu = if (activeMenu == 0) -1 else 0
                            }

                            // Menu 2 Handle: Snippet Capsule
                            EdgeMenuHandle(
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                label = "Snippets",
                                color = Color(0xFFC084FC),
                                sizeMultiplier = settings.dockSize,
                                isLeft = isLeft
                            ) {
                                activeMenu = if (activeMenu == 1) -1 else 1
                            }

                            // Menu 3 Handle: Clipboard+
                            EdgeMenuHandle(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                label = "Clipboard",
                                color = Color(0xFFE9D5FF),
                                sizeMultiplier = settings.dockSize,
                                isLeft = isLeft
                            ) {
                                activeMenu = if (activeMenu == 2) -1 else 2
                            }

                            // Menu 4 Handle: QuickActions & Capture
                            EdgeMenuHandle(
                                icon = Icons.Filled.FlashOn,
                                label = "Actions",
                                color = Color(0xFF7E22CE),
                                sizeMultiplier = settings.dockSize,
                                isLeft = isLeft
                            ) {
                                activeMenu = if (activeMenu == 3) -1 else 3
                            }
                        }

                        // Floating Active Menu Surface Panel (Anthracite + Violet Glass)
                        AnimatedVisibility(
                            visible = activeMenu != -1,
                            enter = slideInHorizontally(animationSpec = tween(280)) { if (isLeft) -it else it },
                            exit = slideOutHorizontally(animationSpec = tween(280)) { if (isLeft) -it else it },
                            modifier = Modifier.align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(
                                        start = if (isLeft) (60.dp * settings.dockSize) else 12.dp,
                                        end = if (!isLeft) (60.dp * settings.dockSize) else 12.dp,
                                        top = 28.dp,
                                        bottom = 28.dp
                                    )
                                    .width((340 * settings.dockSize).dp)
                                    .heightIn(max = 660.dp)
                                    .alpha(settings.dockOpacity),
                                color = Color(0xFF161424), // Elegant Dark Anthracite
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 16.dp,
                                border = BorderStroke(1.5.dp, Color(0x66A855F7)) // Glowing Violet Border
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Menu Title Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFA855F7))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = when (activeMenu) {
                                                    0 -> "1. Apps & Features Dock"
                                                    1 -> "2. Snippet Capsule"
                                                    2 -> "3. Clipboard+ History"
                                                    else -> "4. QuickActions & Note Capture"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF8FAFC)
                                            )
                                        }

                                        IconButton(onClick = { activeMenu = -1 }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFFC084FC))
                                        }
                                    }

                                    // Switcher Pills for the 4 Menus
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        MenuPillButton("Apps", activeMenu == 0) { activeMenu = 0 }
                                        MenuPillButton("Snippets", activeMenu == 1) { activeMenu = 1 }
                                        MenuPillButton("Clipboard+", activeMenu == 2) { activeMenu = 2 }
                                        MenuPillButton("Actions", activeMenu == 3) { activeMenu = 3 }
                                    }

                                    // Menu Content View
                                    when (activeMenu) {
                                        0 -> DockItemsTab(dockApps, dockItems, settingsManager) { activeMenu = -1 }
                                        1 -> SnippetCapsuleTab(snippets) { activeMenu = -1 }
                                        2 -> ClipboardTab(clips, settings, settingsManager, repository) { activeMenu = -1 }
                                        3 -> QuickActionsAndCaptureTab(clips, settings, settingsManager, repository) { activeMenu = -1 }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        params.gravity = Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL
        windowManager.addView(composeView, params)
    }

    private fun startForegroundService() {
        val channelId = "dock_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dock Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Borderline Overlay Active")
            .setContentText("4 Floating Edge Menus Ready")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (clipboardListener != null) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.removePrimaryClipChangedListener(clipboardListener)
        }
        serviceScope.cancel()
        lifecycleOwner.destroy()
        if (composeView != null) {
            windowManager.removeView(composeView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun EdgeMenuHandle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    sizeMultiplier: Float,
    isLeft: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size((44 * sizeMultiplier).dp)
            .clip(CircleShape)
            .clickable { onClick() },
        color = Color(0xFF1E1B2E),
        border = BorderStroke(1.dp, color.copy(alpha = 0.8f)),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size((22 * sizeMultiplier).dp)
            )
        }
    }
}

@Composable
fun MenuPillButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFA855F7) else Color(0xFF262338))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFFA1A1AA)
        )
    }
}

@Composable
fun DockItemsTab(
    dockApps: List<app.axolotl.data.DockAppEntity>,
    dockItems: List<app.axolotl.data.DockItemEntity>,
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (dockItems.isEmpty() && dockApps.isEmpty()) {
            item {
                Text(
                    "No dock items or apps selected. Open Borderline app to select your favorites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(dockItems) { item ->
            if (item.itemType == "FEATURE") {
                FeatureDockCard(item) {
                    when (item.itemKey) {
                        "feature:appendix" -> {
                            val current = settingsManager.settings.value
                            settingsManager.updateSettings(current.copy(appendixMode = !current.appendixMode))
                            Toast.makeText(context, "Appendix Mode: ${if (!current.appendixMode) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(context, "Activated ${item.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    onDismiss()
                }
            }
        }

        items(dockApps) { app ->
            val icon = try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                app.packageName
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        context.startActivity(intent)
                        onDismiss()
                    }
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232034)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x33A855F7))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        AsyncImage(model = icon, contentDescription = appLabel, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(text = appLabel, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF8FAFC), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun FeatureDockCard(item: app.axolotl.data.DockItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2642)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x66A855F7))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (item.itemKey) {
                "feature:snippets" -> Icons.AutoMirrored.Filled.Assignment
                "feature:clipboard" -> Icons.AutoMirrored.Filled.FormatListBulleted
                "feature:appendix" -> Icons.Filled.ContentCopy
                else -> Icons.AutoMirrored.Filled.Send
            }
            Icon(icon, contentDescription = item.title, tint = Color(0xFFC084FC), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))
                Text(text = "Quick Action", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE9D5FF))
            }
        }
    }
}

@Composable
fun SnippetCapsuleTab(snippets: List<SnippetEntity>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Prompt", "Markdown", "Rule", "Template", "Recovery")

    val filteredSnippets = snippets.filter {
        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
                (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true))
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search snippet capsules...", color = Color(0xFFA1A1AA)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color(0x33A855F7),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFA855F7),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF262338),
                        labelColor = Color(0xFFA1A1AA)
                    )
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filteredSnippets) { snippet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232034)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x33A855F7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(snippet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), modifier = Modifier.weight(1f))
                            Text(snippet.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(snippet.content, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF), maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                HandoffHelper.copyToClipboard(context, snippet.title, snippet.content)
                                onDismiss()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFFC084FC))
                            }
                            IconButton(onClick = {
                                HandoffHelper.sendToAI(context, snippet.content)
                                onDismiss()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Psychology, contentDescription = "Send to AI", tint = Color(0xFFC084FC))
                            }
                            IconButton(onClick = {
                                HandoffHelper.shareText(context, snippet.content)
                                onDismiss()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color(0xFFC084FC))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardTab(
    clips: List<ClipEntity>,
    settings: app.axolotl.data.DockSettings,
    settingsManager: SettingsManager,
    repository: DockRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val serviceScope = remember { CoroutineScope(Dispatchers.IO) }

    Column {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = if (settings.appendixMode) Color(0xFF381F52) else Color(0xFF232034)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x66A855F7))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Appendix Clipboard", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), modifier = Modifier.weight(1f))
                    Text(if (settings.appendixMode) "ACTIVE" else "OFF", style = MaterialTheme.typography.labelSmall, color = if (settings.appendixMode) Color(0xFFC084FC) else Color(0xFFA1A1AA), fontWeight = FontWeight.Bold)
                }
                if (settings.appendixDraft.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(settings.appendixDraft, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF), maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = {
                            HandoffHelper.copyToClipboard(context, "Appendix List", settings.appendixDraft)
                            onDismiss()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Appendix", tint = Color(0xFFC084FC))
                        }
                        IconButton(onClick = {
                            settingsManager.clearAppendix()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear Appendix", tint = Color(0xFFA1A1AA))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clipboard History (${clips.size})", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF8FAFC))
            if (clips.isNotEmpty()) {
                IconButton(onClick = {
                    serviceScope.launch { repository.clearUnpinnedClips() }
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear History", tint = Color(0xFFA1A1AA))
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(clips) { clip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232034)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x33A855F7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(clip.contentType, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${clip.charCount} chars", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(clip.content, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF), maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                HandoffHelper.copyToClipboard(context, "Clip", clip.content)
                                onDismiss()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFFC084FC))
                            }
                            IconButton(onClick = {
                                HandoffHelper.shareText(context, clip.content)
                                onDismiss()
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color(0xFFC084FC))
                            }
                            IconButton(onClick = {
                                serviceScope.launch { repository.deleteClip(clip) }
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFA1A1AA))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsAndCaptureTab(
    clips: List<ClipEntity>,
    settings: app.axolotl.data.DockSettings,
    settingsManager: SettingsManager,
    repository: DockRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ioScope = remember { CoroutineScope(Dispatchers.IO) }
    var noteText by remember { mutableStateOf(settings.captureDraft) }

    val latestClip = clips.firstOrNull()?.content ?: ""
    val detected = remember(latestClip) { QuickActionHelper.analyzeText(latestClip) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Quick Actions Section
        Text("Contextual QuickActions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2642)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x66A855F7))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Detected: ${detected.primaryType.name}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC084FC))
                Spacer(Modifier.height(4.dp))
                Text(detected.matchedText.ifBlank { "No text on clipboard" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE9D5FF), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        detected.actionSuggestions.forEach { action ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    action.execute(context)
                    onDismiss()
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF232034)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x33A855F7))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (action.iconName) {
                        "web", "search" -> Icons.Filled.Search
                        "call" -> Icons.Filled.Call
                        "email" -> Icons.Filled.Email
                        "map" -> Icons.Filled.Map
                        "ai" -> Icons.Filled.Psychology
                        "share" -> Icons.Filled.Share
                        else -> Icons.Filled.ContentCopy
                    }
                    Icon(icon, contentDescription = action.label, tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(action.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFF8FAFC))
                }
            }
        }

        // Logcat Inspection Section (READ_LOGS)
        Text("READ_LOGS Logcat Inspector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))

        var logText by remember { mutableStateOf("") }
        var isReadingLogs by remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    isReadingLogs = true
                    logText = app.axolotl.utils.LogReaderHelper.readRecentLogs(maxLines = 80)
                    isReadingLogs = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381F52))
            ) {
                Text(if (isReadingLogs) "Reading..." else "Fetch Logcat", fontSize = 11.sp, color = Color(0xFFE9D5FF))
            }

            if (logText.isNotBlank()) {
                Button(
                    onClick = {
                        settingsManager.appendToAppendix("--- LOGCAT SNAPSHOT ---\n" + logText)
                        Toast.makeText(context, "Logcat appended to Appendix!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text("To Appendix", fontSize = 11.sp)
                }
            }
        }

        if (logText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF0F0E17), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = logText,
                            fontSize = 10.sp,
                            color = Color(0xFFC084FC),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Capture Section
        Text("Instant Note Capture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC))

        OutlinedTextField(
            value = noteText,
            onValueChange = {
                noteText = it
                settingsManager.saveCaptureDraft(it)
            },
            placeholder = { Text("Park thoughts, links, code or instructions...", color = Color(0xFFA1A1AA)) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color(0x33A855F7),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (noteText.isNotBlank()) {
                        ioScope.launch {
                            repository.insertSnippet(
                                SnippetEntity(
                                    title = "Captured Note",
                                    content = noteText,
                                    category = "Prompt"
                                )
                            )
                        }
                        noteText = ""
                        settingsManager.saveCaptureDraft("")
                        Toast.makeText(context, "Saved to Snippet Capsule!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Save Snippet", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Capsule", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    if (noteText.isNotBlank()) {
                        settingsManager.appendToAppendix(noteText)
                        noteText = ""
                        settingsManager.saveCaptureDraft("")
                        Toast.makeText(context, "Appended to Research List!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262338))
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Append List", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("To Appendix", fontSize = 11.sp)
            }
        }
    }
}
