package app.axolotl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.axolotl.evolver.AxolotlModule
import app.axolotl.evolver.ModuleActionDispatcher
import app.axolotl.evolver.ModuleAvailability
import app.axolotl.evolver.ModuleIcon
import app.axolotl.modules.createCoreModuleRegistry
import app.axolotl.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val dispatcher = remember {
                    ModuleActionDispatcher(coreModuleRegistry).apply {
                        bind("borderline.open") {
                            startActivity(Intent(this@MainActivity, BorderlineActivity::class.java))
                        }
                    }
                }
                AxolotlFrame {
                    dispatcher.dispatch("borderline", "borderline.open")
                }
            }
        }
    }
}

internal val coreModuleRegistry = createCoreModuleRegistry()

@Composable
internal fun AxolotlFrame(onOpenBorderline: () -> Unit) {
    val background = Brush.verticalGradient(
        listOf(Color(0xFF0F0E17), Color(0xFF1E1B2E), Color(0xFF12111C)),
    )
    Box(Modifier.fillMaxSize().background(background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 56.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AxolotlHeader() }
            item {
                BorderlineCard(onOpenBorderline)
                Spacer(Modifier.height(12.dp))
                Text(
                    "WORKSPACES",
                    color = Color(0xFFC084FC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                )
            }
            items(
                coreModuleRegistry.all().filterNot { it.manifest.id == "borderline" },
                key = { it.manifest.id },
            ) { WorkspaceCard(it) }
        }
    }
}

@Composable
private fun AxolotlHeader() {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SpaceDashboard, null, tint = Color(0xFFA855F7))
            Text(
                "  AXOLOTL",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
        Text("One edge. Every tool.", color = Color(0xFFA1A1AA), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun BorderlineCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFA855F7)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("BORDERLINE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(
                "The shared frame · Dock, clipboard, snippets and handoffs",
                color = Color(0xFFF3E8FF),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "OPEN CONTROL CENTER  →",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun WorkspaceCard(module: AxolotlModule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242132)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF3B3152)) {
                Icon(module.manifest.icon.imageVector(), null, tint = Color(0xFFC084FC), modifier = Modifier.padding(12.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(module.manifest.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(module.manifest.description, color = Color(0xFFA1A1AA), fontSize = 12.sp)
            }
            Text(
                if (module.availability == ModuleAvailability.AVAILABLE) "OPEN" else "NEXT",
                color = if (module.availability == ModuleAvailability.AVAILABLE) Color(0xFFC084FC) else Color(0xFF71717A),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
    }
}

private fun ModuleIcon.imageVector(): ImageVector = when (this) {
    ModuleIcon.BORDERLINE -> Icons.Default.SpaceDashboard
    ModuleIcon.APPS -> Icons.Default.Apps
    ModuleIcon.FILES -> Icons.Default.Description
    ModuleIcon.BROWSER -> Icons.Default.Language
    ModuleIcon.AUTOMATE -> Icons.Default.AutoAwesome
    ModuleIcon.AI -> Icons.Default.Memory
}
