package com.example.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.screen.PrivacyPolicyDialog
import com.example.ui.screen.TermsConditionsDialog
import com.example.data.model.CdmDataProvider
import com.example.data.model.CdmMachine
import com.example.viewmodel.CdmViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: CdmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel State Observers
    val riderLoc by viewModel.riderLocation.collectAsState()
    val riderSpeed by viewModel.riderSpeed.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val selectedMachine by viewModel.selectedMachine.collectAsState()
    val arrivedShow by viewModel.arrivedShow.collectAsState()
    val musicPlaying by viewModel.isMusicPlaying.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val filteredMachines by viewModel.filteredMachines.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsState()

    // Local UI control states
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf<CdmMachine?>(null) }
    var showNoteDialog by remember { mutableStateOf<CdmMachine?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }

    // Map view parameters for Canvas zooming/dragging
    var mapScale by remember { mutableFloatStateOf(1.2f) }
    var mapOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Center map initially
    LaunchedEffect(key1 = true) {
        mapOffset = Offset(0f, 0f)
        mapScale = 1.3f
    }

    // Auto-pan map during route navigation simulation to keep rider centered
    LaunchedEffect(riderLoc) {
        if (isNavigating) {
            mapOffset = Offset(0f, 0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0B1220) else Color(0xFFF8FAFC))
    ) {
        // ==========================================
        // 1. HIGH-PERFORMANCE NATIVE CANVAS DEPOSIT FINDER MAP
        // ==========================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        mapScale = (mapScale * zoom).coerceIn(0.5f, 3.5f)
                        mapOffset = mapOffset + pan
                    }
                }
                .testTag("interactive_canvas_map")
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f + mapOffset.x
            val centerY = canvasHeight / 2f + mapOffset.y

            // Coordinate projection
            fun projectCoordinates(lat: Double, lng: Double): Offset {
                val scaleFactor = 4500f * mapScale
                val x = centerX + ((lng - 51.5333) * scaleFactor).toFloat()
                val y = centerY - ((lat - 25.2867) * scaleFactor).toFloat()
                return Offset(x, y)
            }

            val isDark = isDarkTheme
            val mapBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF4F3F0)
            val waterColor = if (isDark) Color(0xFF1E293B) else Color(0xFFC0E8FF)
            val parkColor = if (isDark) Color(0xFF14532D).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
            val streetGridColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
            val highwayShellColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
            val highwayCoreColor = if (isDark) Color(0xFFFF9100) else Color(0xFFFFD180)

            // Draw daylight map background canvas
            drawRect(color = mapBgColor)

            // ==========================================
            // WATER SYSTEM: Persian Gulf / Doha Bay (East Side)
            // ==========================================
            val seaPath = Path().apply {
                moveTo(canvasWidth, 0f)
                val pNorth = projectCoordinates(25.35, 51.535)
                lineTo(pNorth.x, pNorth.y)
                
                // Tracing the beautiful crescent line of Doha Corniche bay
                val pBay1 = projectCoordinates(25.32, 51.537)
                val pBay2 = projectCoordinates(25.298, 51.545)
                val pBay3 = projectCoordinates(25.282, 51.554)
                
                quadraticTo(pBay1.x, pBay1.y, pBay2.x, pBay2.y)
                quadraticTo(pBay2.x, pBay2.y, pBay3.x, pBay3.y)
                
                lineTo(canvasWidth, canvasHeight)
                close()
            }
            drawPath(path = seaPath, color = waterColor) // Clean cyan sea water bay

            // ==========================================
            // PARKS SYSTEM: Soft Green Zones
            // ==========================================
            // Al Bidda park near waterfront
            val pAlBidda = projectCoordinates(25.302, 51.522)
            drawCircle(
                color = parkColor,
                radius = 70f * mapScale,
                center = pAlBidda
            )
            
            // Aspire Zone Greenery (West Doha)
            val pAspire = projectCoordinates(25.26, 51.44)
            drawCircle(
                color = parkColor,
                radius = 90f * mapScale,
                center = pAspire
            )

            // Draw elegant standard grid for general city block streets
            val gridSpacing = 160f * mapScale
            for (i in -10..15) {
                // Vertical secondary streets
                val lx = centerX + i * gridSpacing
                drawLine(
                    color = streetGridColor,
                    start = Offset(lx, 0f),
                    end = Offset(lx, canvasHeight),
                    strokeWidth = 1.5f * mapScale
                )
                // Horizontal secondary streets
                val ly = centerY + i * gridSpacing
                drawLine(
                    color = streetGridColor,
                    start = Offset(0f, ly),
                    end = Offset(canvasWidth, ly),
                    strokeWidth = 1.5f * mapScale
                )
            }

            // ==========================================
            // HIGHWAY SYSTEM: Realistic Waze Expressway Arterials
            // ==========================================

            // Ring roads (C-Ring, D-Ring)
            val ringRadius1 = 180f * mapScale
            val ringRadius2 = 380f * mapScale
            val ringRadius3 = 580f * mapScale

            // Draw outer gray shell, and white overlay core for Ring roads
            listOf(ringRadius1, ringRadius2, ringRadius3).forEach { r ->
                drawCircle(color = highwayShellColor, radius = r, center = Offset(centerX, centerY), style = Stroke(width = 8f * mapScale))
                drawCircle(color = if (isDark) Color(0xFF1E293B) else Color.White, radius = r, center = Offset(centerX, centerY), style = Stroke(width = 5f * mapScale))
            }

            // 1. Salwa Road Expressway (West-Southwest to Trade center)
            val pSalwaStart = projectCoordinates(25.26, 51.35)
            val pSalwaEnd = projectCoordinates(25.2867, 51.5333)
            drawLine(color = highwayShellColor, start = pSalwaStart, end = pSalwaEnd, strokeWidth = 14f * mapScale, cap = StrokeCap.Round)
            drawLine(color = highwayCoreColor, start = pSalwaStart, end = pSalwaEnd, strokeWidth = 8f * mapScale, cap = StrokeCap.Round) // Peach/Orange Highway Core

            // 2. Al Shamal Rd Highway (Northward corridor)
            val pShamalStart = projectCoordinates(25.2867, 51.5333)
            val pShamalEnd = projectCoordinates(25.42, 51.50)
            drawLine(color = highwayShellColor, start = pShamalStart, end = pShamalEnd, strokeWidth = 14f * mapScale, cap = StrokeCap.Round)
            drawLine(color = highwayCoreColor, start = pShamalStart, end = pShamalEnd, strokeWidth = 8f * mapScale, cap = StrokeCap.Round)

            // 3. Doha Corniche Crescent Boulevard
            val cornichePath = Path().apply {
                val p1 = projectCoordinates(25.282, 51.554)
                val p2 = projectCoordinates(25.298, 51.545)
                val p3 = projectCoordinates(25.32, 51.537)
                val p4 = projectCoordinates(25.35, 51.535)
                moveTo(p1.x, p1.y)
                quadraticTo(p2.x, p2.y, p3.x, p3.y)
                quadraticTo(p3.x, p3.y, p4.x, p4.y)
            }
            drawPath(path = cornichePath, color = highwayShellColor, style = Stroke(width = 10f * mapScale, cap = StrokeCap.Round))
            drawPath(path = cornichePath, color = if (isDark) Color(0xFF1E293B) else Color.White, style = Stroke(width = 6f * mapScale, cap = StrokeCap.Round))

            // 4. Industrial Area Grid (Sanniya street blocks on South-West)
            val pInd = projectCoordinates(25.21, 51.42)
            for (step in -3..3) {
                val offsetDist = step * 35f * mapScale
                // Verticals
                drawLine(color = streetGridColor, start = Offset(pInd.x + offsetDist, pInd.y - 120f * mapScale), end = Offset(pInd.x + offsetDist, pInd.y + 120f * mapScale), strokeWidth = 5f * mapScale)
                // Horizontals
                drawLine(color = streetGridColor, start = Offset(pInd.x - 120f * mapScale, pInd.y + offsetDist), end = Offset(pInd.x + 120f * mapScale, pInd.y + offsetDist), strokeWidth = 5f * mapScale)
            }

            // DRAW PLOTTING ROUTE PATH line
            selectedMachine?.let { dest ->
                val startOffset = projectCoordinates(riderLoc.first, riderLoc.second)
                val destOffset = projectCoordinates(dest.latitude, dest.longitude)

                // Beautiful thick Waze neon blue routing line
                val dashLength = 30f
                val gapLength = 15f
                val routePathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)

                drawLine(
                    color = Color(0x330284C7),
                    start = startOffset,
                    end = destOffset,
                    strokeWidth = 24f,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF00AAFF),
                    start = startOffset,
                    end = destOffset,
                    strokeWidth = 10f,
                    pathEffect = routePathEffect,
                    cap = StrokeCap.Round
                )
            }

            // DRAW ALL 131 DEPOSIT MACHINES USING VECTOR PINMARKERS
            filteredMachines.forEach { machine ->
                val offset = projectCoordinates(machine.latitude, machine.longitude)

                if (offset.x in -100f..(canvasWidth + 100f) && offset.y in -100f..(canvasHeight + 100f)) {
                    val statusColor = when (machine.status) {
                        "DOWN" -> Color(0xFFEF4444) // Red: Out of cash/Down
                        "CROWDED" -> Color(0xFFF59E0B) // Amber: Backlog/Crowded
                        else -> Color(0xFF10B981) // Neon Green: Active/Operational
                    }

                    // 1. Shadow underneath the pin base for three-dimensional elevation
                    drawCircle(
                        color = Color(0x33090D16),
                        radius = 6f * mapScale,
                        center = Offset(offset.x, offset.y + 1f)
                    )

                    // 2. Custom mathematical vector Teardrop shape
                    val pinPath = Path().apply {
                        moveTo(offset.x, offset.y)
                        // Left side curve bulging up out
                        cubicTo(
                            offset.x - 11f * mapScale, offset.y - 10f * mapScale,
                            offset.x - 11f * mapScale, offset.y - 28f * mapScale,
                            offset.x, offset.y - 28f * mapScale
                        )
                        // Right side curve bulging up out
                        cubicTo(
                            offset.x + 11f * mapScale, offset.y - 28f * mapScale,
                            offset.x + 11f * mapScale, offset.y - 10f * mapScale,
                            offset.x, offset.y
                        )
                        close()
                    }

                    // 3. Draw pin color and white border
                    drawPath(path = pinPath, color = statusColor)
                    drawPath(path = pinPath, color = Color.White, style = Stroke(width = 2.2f * mapScale))

                    // 4. Inner white core to represent cash box
                    drawCircle(
                        color = Color.White,
                        radius = 4.2f * mapScale,
                        center = Offset(offset.x, offset.y - 18.5f * mapScale)
                    )

                    // 5. Draw simple star insignia for saved favorites
                    if (machine.isFavorite) {
                        drawCircle(
                            color = Color(0xFFEAB308),
                            radius = 4f * mapScale,
                            center = Offset(offset.x + 11f * mapScale, offset.y - 24f * mapScale)
                        )
                    }
                }
            }

            // DRAW RIDER GPS PULSING LOCATION
            val riderOffset = projectCoordinates(riderLoc.first, riderLoc.second)
            
            drawCircle(
                color = Color(0x330284C7),
                radius = 45f + (System.currentTimeMillis() % 1000f) / 15f,
                center = riderOffset
            )

            drawCircle(
                color = Color(0xFF0284C7),
                radius = 18f,
                center = riderOffset
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = riderOffset
            )
        }

        // ==========================================
        // 2. TOP PANEL: LIVE STATUS BAR & AUDIO WIDGETS
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-left brand and settings panel
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { showAboutDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "App info menu",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TALABAT CDM",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // Theme Quick Toggle Card (Light / Dark Mode shortcut)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { viewModel.setDarkTheme(!isDarkTheme) }
                        .testTag("map_theme_quick_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Toggle Daylight & Midnight Neon Night Modes",
                            tint = Color(0xFFFF5E00), // Talabat orange
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDarkTheme) "Midnight Neon" else "Waze Daylight",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Top-right dashboard sound & voice settings
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        // Microphone Trigger
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Voice command listening offline in Sanniya-Doha...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Voice assistant search",
                                tint = Color(0xFF0284C7)
                            )
                        }

                        VerticalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                        // Music Controls Overlay
                        IconButton(
                            onClick = { viewModel.toggleMusicPlay() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (musicPlaying) Icons.Filled.PlayArrow else Icons.Default.Refresh,
                                contentDescription = "Simulate road beats music",
                                tint = if (musicPlaying) Color(0xFF10B981) else Color(0xFF64748B)
                            )
                        }

                        VerticalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                        // Speaker indicator mute/unmute
                        IconButton(
                            onClick = { viewModel.toggleMuted() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.Close else Icons.Filled.CheckCircle,
                                contentDescription = "Mute driving voice",
                                tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Firebase Live Synchronization Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (firebaseSyncState == "SYNCING") Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (firebaseSyncState == "SYNCING") Color(0xFFBFDBFE) else Color(0xFFBBF7D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (firebaseSyncState == "SYNCING") Color(0xFF3B82F6) else Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (firebaseSyncState == "SYNCING") 
                            "Cloud Sync: Backup live notes and crowd status with Firebase..." 
                        else 
                            "Cloud Sync: Active background Firebase Database sync secure ✔",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (firebaseSyncState == "SYNCING") Color(0xFF1E40AF) else Color(0xFF166534),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Live scrolling simulation news ticker (Waze Alert style)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Live reports channel",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE QATAR: 131 deposit stations updated instantly by other riders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ==========================================
        // 3. MID-LEFT OVERLAYS: SPEEDOMETER & WAZERS Ticker
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speedometer (Crisp high-contrast white dome)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, if (riderSpeed > speedLimit) Color(0xFFEF4444) else Color(0xFF0284C7), CircleShape)
                    .shadow(3.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$riderSpeed",
                        color = if (riderSpeed > speedLimit) Color(0xFFEF4444) else Color(0xFF0F172A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "km/h",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Speed limit tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIMIT $speedLimit",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Map Reset Center Button
            IconButton(
                onClick = {
                    mapOffset = Offset(0f, 0f)
                    mapScale = 1.3f
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .shadow(2.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aim center rider",
                    tint = Color(0xFF0284C7)
                )
            }
        }

        // ==========================================
        // 4. MID-RIGHT WARNING BUTTONS
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .clickable {
                        Toast.makeText(context, "Scanning Doha: 4,740 Talabat riders online near Industrial area!", Toast.LENGTH_LONG).show()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Nearby riders count",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "4740 Riders",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ALERT HAZARDS PANEL
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAB308))
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(4.dp, CircleShape)
                    .clickable { showAlertDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Submit road alerts",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "+ Alert",
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // ==========================================
        // 5. HUD NAVIGATION CARD
        // ==========================================
        if (isNavigating) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF0284C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Driving step arrow",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proceeding to ${selectedMachine?.shortBranchName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row {
                            Text(
                                text = "Estimated: $distanceKm km • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "$etaMinutes min left",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.cancelActiveNavigation() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Exit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 5.5 FLOATING ACTION BUTTON: NEAREST CDM GPS DETECTOR
        // ==========================================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isSearchExpanded) 490.dp else 240.dp) // Auto-adjust position dynamically
                .clickable {
                    viewModel.selectNearestOperationalMachine()
                    Toast.makeText(context, "Nearest Active CDM mapped! Tap 'Preview Drive' to roll.", Toast.LENGTH_LONG).show()
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Find nearest operational CDM",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nearest CDM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // ==========================================
        // 6. BOTTOM SECTOR: PERSISTENT SLIDING PANEL
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSearchExpanded) 480.dp else 225.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag notch
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                            .clickable { isSearchExpanded = !isSearchExpanded }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // "Where to?" Search Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { isSearchExpanded = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon magnifying glass",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Where to? Search Terminal ID or Depot Name...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextFieldWithQuery(
                                query = searchQuery,
                                onQueryChange = {
                                    viewModel.search(it)
                                    isSearchExpanded = true
                                }
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear query trigger",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        viewModel.search("")
                                    }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Query triggers",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isSearchExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShortcutWidget(
                                icon = Icons.Default.Home,
                                label = "Main Depot",
                                color = Color(0xFF2563EB),
                                onClick = {
                                    viewModel.search("KeyBS")
                                    isSearchExpanded = true
                                }
                            )
                            ShortcutWidget(
                                icon = Icons.Default.Menu,
                                label = "Sanniya",
                                color = Color(0xFFD97706),
                                onClick = {
                                    viewModel.search("Sanniya")
                                    isSearchExpanded = true
                                }
                            )
                            ShortcutWidget(
                                icon = Icons.Default.LocationOn,
                                label = "Wakra",
                                color = Color(0xFF059669),
                                onClick = {
                                    viewModel.search("Wakra")
                                    isSearchExpanded = true
                                }
                            )
                            ShortcutWidget(
                                icon = Icons.Default.Star,
                                label = "Favorites",
                                color = Color(0xFFCA8A04),
                                onClick = {
                                    viewModel.setFilter("FAVORITE")
                                    isSearchExpanded = true
                                }
                            )
                        }

                        selectedMachine?.let { machine ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(8.dp))
                            ActiveMachineMiniCard(
                                machine = machine,
                                distance = distanceKm,
                                eta = etaMinutes,
                                onDriveClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(machine.mapsUrl))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Redirecting to Map navigation...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSimulateClick = { viewModel.startNavigationSimulation() },
                                onFavoriteToggle = { viewModel.toggleFavorite(machine.id) },
                                onReportStatus = { showReportDialog = machine },
                                onAddNote = { showNoteDialog = machine }
                            )
                        } ?: run {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Swipe up to filter & explore all 131 deposit machines.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    } else {
                        // Expanded Search Panel: Filter Chips and List
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                "ALL" to "All List",
                                "FAVORITE" to "⭐ Favorites",
                                "SANNIYA" to "🏭 Sanniya",
                                "WAKRA" to "🏖️ Wakra",
                                "KHOR" to "🏰 Al Khor",
                                "RAYYAN" to "🕌 Al Rayyan"
                            )

                            filters.take(4).forEach { (type, title) ->
                                FilterChip(
                                    selected = activeFilter == type,
                                    onClick = { viewModel.setFilter(type) },
                                    label = title
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredMachines) { machine ->
                                val dist = viewModel.calculateDistance(
                                    riderLoc.first,
                                    riderLoc.second,
                                    machine.latitude,
                                    machine.longitude
                                )
                                val roundedDist = round(dist * 10.0) / 10.0

                                CdmMachineRowItem(
                                    machine = machine,
                                    distance = roundedDist,
                                    onItemClick = {
                                        viewModel.selectMachine(machine)
                                        isSearchExpanded = false
                                    }
                                )
                            }

                            if (filteredMachines.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No compatible deposit machines found.",
                                            color = Color(0xFF64748B),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Try typing another terminal ID or area.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { isSearchExpanded = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Collapse Search View", color = Color(0xFF475569), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. ARRIVED SUCCESS BANNER
        // ==========================================
        if (arrivedShow) {
            Dialog(onDismissRequest = { viewModel.dismissArrivedDialog() }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success tick",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Arrived Safely!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You reached ${selectedMachine?.merchantName} (Terminal: ${selectedMachine?.terminalId}) successfully.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Please deposit your cash collections now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.dismissArrivedDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finish Ride", color = Color.White)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. HAZARDS ALERT MAKER
        // ==========================================
        if (showAlertDialog) {
            Dialog(onDismissRequest = { showAlertDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Report Hazard Alert",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HazardChoiceButton("🚧 Road Block", Modifier.weight(1f)) {
                                Toast.makeText(context, "Published Road block near Doha industrial Area!", Toast.LENGTH_SHORT).show()
                                showAlertDialog = false
                            }
                            HazardChoiceButton("🚔 Police", Modifier.weight(1f)) {
                                Toast.makeText(context, "Speed check trap mapped near Salwa Expressway!", Toast.LENGTH_SHORT).show()
                                showAlertDialog = false
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HazardChoiceButton("⚠️ Accident", Modifier.weight(1f)) {
                                Toast.makeText(context, "Accident delay reported on D-Ring road directions", Toast.LENGTH_SHORT).show()
                                showAlertDialog = false
                            }
                            HazardChoiceButton("🚗 Traffic", Modifier.weight(1f)) {
                                Toast.makeText(context, "Slow-moving traffic flagged on Corniche lane!", Toast.LENGTH_SHORT).show()
                                showAlertDialog = false
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        TextButton(onClick = { showAlertDialog = false }) {
                            Text("Dismiss", color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 9. STATUS & CASH BOX LEVEL REPORT SYSTEM
        // ==========================================
        showReportDialog?.let { machine ->
            Dialog(onDismissRequest = { showReportDialog = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Report Machine State",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Help other riders check cash box levels!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        ReportChoiceButton(
                            text = "🟢 Operational / Fully Working",
                            desc = "No Queue. Cash box accepting banknotes.",
                            borderColor = Color(0xFF10B981)
                        ) {
                            viewModel.reportMachineStatus(machine.id, "ACTIVE", "Operational / Working Perfectly")
                            Toast.makeText(context, "Thank you! Operational status saved.", Toast.LENGTH_SHORT).show()
                            showReportDialog = null
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportChoiceButton(
                            text = "🟡 Crowded / Cash box full soon",
                            desc = "Long queue or slow processing backlog.",
                            borderColor = Color(0xFFF59E0B)
                        ) {
                            viewModel.reportMachineStatus(machine.id, "CROWDED", "Crowded / 12+ Min Wait")
                            Toast.makeText(context, "Crowd alert submitted to nearby Talabat riders.", Toast.LENGTH_SHORT).show()
                            showReportDialog = null
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportChoiceButton(
                            text = "🔴 Down / Cash box full / Locked",
                            desc = "Rejecting cash, out of service, or branch closed.",
                            borderColor = Color(0xFFEF4444)
                        ) {
                            viewModel.reportMachineStatus(machine.id, "DOWN", "Offline / Out of order")
                            Toast.makeText(context, "Out of cash alert published immediately.", Toast.LENGTH_SHORT).show()
                            showReportDialog = null
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showReportDialog = null }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 10. TASK NOTES DIALOG
        // ==========================================
        showNoteDialog?.let { machine ->
            var currentNotes by remember { mutableStateOf(machine.notes) }
            Dialog(onDismissRequest = { showNoteDialog = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Personal Navigation Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = currentNotes,
                            onValueChange = { currentNotes = it },
                            placeholder = { Text("E.g. Inside lulu supermarket, requires exact change, no line after 10 PM...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Color(0xFF0284C7),
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(onClick = { showNoteDialog = null }) {
                                Text("Discard", color = Color(0xFF64748B))
                            }
                            Button(
                                onClick = {
                                    viewModel.saveMachineNotes(machine.id, currentNotes)
                                    showNoteDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Save Note", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 11. INFORMATIONAL ABOUT SYSTEM DIALOG (SETTINGS & COMPLIANCE PORTAL)
        // ==========================================
        var showPrivacyPolicySub by remember { mutableStateOf(false) }
        var showTermsConditionsSub by remember { mutableStateOf(false) }

        if (showPrivacyPolicySub) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyPolicySub = false })
        }
        if (showTermsConditionsSub) {
            TermsConditionsDialog(onDismiss = { showTermsConditionsSub = false })
        }

        if (showAboutDialog) {
            val isDarkThemeActive by viewModel.isDarkTheme.collectAsState()
            val isBiometricsEnabledActive by viewModel.isBiometricsEnabled.collectAsState()
            val loggedInUserActive by viewModel.loggedInUser.collectAsState()

            Dialog(onDismissRequest = { showAboutDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(16.dp).fillMaxWidth().widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration Hub icon",
                            tint = Color(0xFFFF5E00), // Talabat orange color
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rider Terminal Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Qatar Delivery Hub • Authorized Session",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // User profile badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Active Rider Profile Avatar icon",
                                tint = Color(0xFFFF5E00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Active Session",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = if (loggedInUserActive.isEmpty()) "Demonstration Rider Profile" else loggedInUserActive,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch: Theme selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Active Theme icon toggle representing state",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Neon Night Dark Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Optimized for night-shift road visibility",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = isDarkThemeActive,
                                onCheckedChange = { viewModel.setDarkTheme(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5E00)),
                                modifier = Modifier.testTag("theme_toggle_switch")
                            )
                        }

                        // Divider
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                        // Switch: Biometrics option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Fingerprint biological locking option toggle icon",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Lock Screen",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Require fingerprint/Face Lock upon start",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = isBiometricsEnabledActive,
                                onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5E00)),
                                modifier = Modifier.testTag("biometric_toggle_switch")
                            )
                        }

                        // Divider
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                        // Legal Center Text title
                        Text(
                            "LEGAL & COMPLIANCE SECTION",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Legal triggers: Privacy and Terms
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPrivacyPolicySub = true },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFF5E00))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Privacy Policy", fontSize = 11.sp, color = Color(0xFF334155))
                            }

                            OutlinedButton(
                                onClick = { showTermsConditionsSub = true },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.List, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFF5E00))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Terms of Use", fontSize = 11.sp, color = Color(0xFF334155))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Support contact detail
                        Text(
                            text = "This application matches 131 Cash Deposit points across Qatar including Wakra, Sanniya, Khor, and Rayyan. For telemetry adjustments, contact central dispatching.",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Log Out button and Dismiss
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    showAboutDialog = false
                                    viewModel.logout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.ExitToApp, "Sign Out application", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Out", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { showAboutDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
            .border(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF475569),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ShortcutWidget(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF334155),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CdmMachineRowItem(
    machine: CdmMachine,
    distance: Double,
    onItemClick: () -> Unit
) {
    val statusText = when (machine.status) {
        "DOWN" -> "🔴 Offline"
        "CROWDED" -> "🟡 Crowded"
        else -> "🟢 Working"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onItemClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (machine.status == "DOWN") Color(0x1AEF4444)
                    else if (machine.status == "CROWDED") Color(0x1AF59E0B)
                    else Color(0x1A10B981),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "ATM safe icon",
                tint = if (machine.status == "DOWN") Color(0xFFEF4444)
                else if (machine.status == "CROWDED") Color(0xFFF59E0B)
                else Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${machine.id}. ${machine.merchantName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = machine.shortBranchName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$distance km",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0284C7)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun ActiveMachineMiniCard(
    machine: CdmMachine,
    distance: Double,
    eta: Int,
    onDriveClick: () -> Unit,
    onSimulateClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportStatus: () -> Unit,
    onAddNote: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = machine.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Text(
                        text = "ID: ${machine.terminalId} • ${machine.shortBranchName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        maxLines = 1
                    )
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Bookmark favoritism",
                        tint = if (machine.isFavorite) Color(0xFFEAB308) else Color(0xFF94A3B8)
                    )
                }
            }

            if (machine.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, "Note icon", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = machine.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDriveClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AAFF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Search, "Navigate icon", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GPS Guide", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onSimulateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Home, "Simulate icon", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Drive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${machine.lastReportType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (machine.status) {
                        "DOWN" -> Color(0xFFEF4444)
                        "CROWDED" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    },
                    fontWeight = FontWeight.Bold
                )

                Row {
                    TextButton(onClick = onAddNote, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Edit, "Add note key", modifier = Modifier.size(14.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Note", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onReportStatus, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Warning, "Report ATM down key", modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report Level", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BasicTextFieldWithQuery(
    query: String,
    onQueryChange: (String) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(
            color = Color(0xFF0F172A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("basic_search_textarea")
    )
}

@Composable
fun HazardChoiceButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ReportChoiceButton(
    text: String,
    desc: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(text, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(desc, color = Color(0xFF475569), fontSize = 10.sp)
    }
}
