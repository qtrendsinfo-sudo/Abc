package com.example.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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

// Google Maps Compose Imports
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*

// Premium Custom Marker Drawing Graph imports
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val filteredMachines by viewModel.filteredMachines.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsState()
    val isBiometricsEnabledActive by viewModel.isBiometricsEnabled.collectAsState()
    val nearestMachine by viewModel.nearestMachine.collectAsState()

    // Premium micro-marker icon caching of bitmap descriptors
    val markerIconActive = remember(isDarkTheme) { createCustomMarkerIcon(context, "ACTIVE", isDarkTheme) }
    val markerIconCrowded = remember(isDarkTheme) { createCustomMarkerIcon(context, "CROWDED", isDarkTheme) }
    val markerIconDown = remember(isDarkTheme) { createCustomMarkerIcon(context, "DOWN", isDarkTheme) }

    // Local UI control states
    var isOnlineRider by remember { mutableStateOf(true) }
    var showHamburgerMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf<CdmMachine?>(null) }
    var editNotesText by remember { mutableStateOf("") }
    var isEditingNotes by remember { mutableStateOf(false) }

    // Legal dialogues control states
    var showPrivacyPolicySub by remember { mutableStateOf(false) }
    var showTermsConditionsSub by remember { mutableStateOf(false) }

    // Map Camera settings centered on Doha Qatar
    val dohaCenter = LatLng(25.2854, 51.5310)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dohaCenter, 11f)
    }

    // Effect: Keep camera centered on moving simulator rider during navigation
    LaunchedEffect(riderLoc) {
        if (isNavigating) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(riderLoc.first, riderLoc.second)),
                800
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
    ) {
        // ==========================================
        // 1. INTEGRATED REAL GOOGLE MAPS COMPOSE EDGE-TO-EDGE
        // ==========================================
        val mapUiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        }

        val mapProperties = remember(isDarkTheme) {
            MapProperties(
                isMyLocationEnabled = true
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties,
            onMapClick = {
                viewModel.selectMachine(null)
                isEditingNotes = false
            }
        ) {
            // Display all 131 positions on Google Map with custom minimalist micro-markers
            filteredMachines.forEach { machine ->
                val statusTextStr = when (machine.status) {
                    "DOWN" -> "Offline"
                    "CROWDED" -> "Crowded"
                    else -> "Working"
                }

                val iconDescriptor = when (machine.status) {
                    "DOWN" -> markerIconDown
                    "CROWDED" -> markerIconCrowded
                    else -> markerIconActive
                }
                
                Marker(
                    state = rememberMarkerState(position = LatLng(machine.latitude, machine.longitude)),
                    title = machine.merchantName,
                    snippet = "${machine.shortBranchName} • $statusTextStr",
                    icon = iconDescriptor,
                    onClick = {
                        viewModel.selectMachine(machine)
                        editNotesText = machine.notes
                        isEditingNotes = false
                        // Camera scroll transitions cleanly on marker selection
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLng(LatLng(machine.latitude, machine.longitude)),
                                500
                            )
                        }
                        true
                    }
                )
            }
        }

        // ==========================================
        // 2. FLOATING TOP CONTROLLER / SEARCH BAR WITH SHADOW
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = { showHamburgerMenu = true },
                        modifier = Modifier.testTag("map_hamburger_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Hamburger menu for rider legal and security configurations",
                            tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.search(it) },
                        placeholder = {
                            Text(
                                "Search 131 positions in Qatar...",
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("map_search_field"),
                        singleLine = true
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear inquiry keyword parameters",
                                tint = Color(0xFFFF5E00)
                            )
                        }
                    }

                    // Theme Quick Mode toggle button
                    IconButton(
                        onClick = { viewModel.setDarkTheme(!isDarkTheme) },
                        modifier = Modifier.testTag("quick_theme_trigger_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Switch between Waze Day Mode and Midnight Neon Night Mode",
                            tint = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. HORIZONTAL DYNAMIC REGIONAL CHIPS
            // ==========================================
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "All Positions",
                    "FAVORITE" to "⭐ Favorites"
                )
                items(filters) { (type, title) ->
                    val isSelected = activeFilter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFFFF5E00) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFF8F40) else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setFilter(type) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else (if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ==========================================
            // PREMIER DYNAMIC CLOSEST TERMINAL OVERLAY
            // ==========================================
            AnimatedVisibility(
                visible = nearestMachine != null && selectedMachine == null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                nearestMachine?.let { nearest ->
                    val dist = viewModel.calculateDistance(
                        riderLoc.first, riderLoc.second,
                        nearest.latitude, nearest.longitude
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDarkTheme) Color(0xFFFF5E00).copy(alpha = 0.4f) else Color(0xFFFF5E00).copy(alpha = 0.2f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("nearest_machine_overlay")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5E00).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5E00),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CLOSEST CDM TERMINAL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5E00)
                                )
                                Text(
                                    text = nearest.merchantName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${"%.2f".format(dist)} km away • ${nearest.shortBranchName}",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.selectMachine(nearest)
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(nearest.latitude, nearest.longitude), 15f),
                                            1000
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. DUTY STATUS TOGGLE (GO ONLINE / GO OFFLINE)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (selectedMachine != null) 360.dp else 24.dp)
                .zIndex(10f)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnlineRider) Color(0xFF10B981) else Color(0xFF64748B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .clickable { 
                        isOnlineRider = !isOnlineRider
                        Toast.makeText(context, if (isOnlineRider) "Rider Duty Status: Ready and Online" else "Offline: System standby", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("rider_duty_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnlineRider) Color.White else Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isOnlineRider) "YOU ARE ONLINE" else "YOU ARE OFFLINE",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // ==========================================
        // 5. RELOCATION (GPS CENTER) FAB
        // ==========================================
        FloatingActionButton(
            onClick = {
                viewModel.moveToLiveLocation { location ->
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(location.first, location.second), 16f),
                            1000
                        )
                    }
                }
                Toast.makeText(context, "Centered map on your live GPS location", Toast.LENGTH_SHORT).show()
            },
            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
            contentColor = Color(0xFFFF5E00),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = if (selectedMachine != null) 360.dp else 24.dp, end = 16.dp)
                .size(54.dp)
                .testTag("gps_center_fab")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Map snap back to simulator rider coordinate dot",
                modifier = Modifier.size(24.dp)
            )
        }

        // ==========================================
        // 6. MODERN BOTTOM SLIDER POPUP (REPLACES OUTDATED PERMANENT BAR)
        // ==========================================
        AnimatedVisibility(
            visible = selectedMachine != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .zIndex(15f)
        ) {
            selectedMachine?.let { machine ->
                Card(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .testTag("selected_machine_slider_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Grab bar indicator styling
                        Box(
                            modifier = Modifier
                                .size(40.dp, 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDarkTheme) Color(0xFF475569) else Color(0xFFE2E8F0))
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Header with Title & Dismiss and Favorite Toggles
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = machine.merchantName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = machine.branchName,
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Favorite heart toggle
                            IconButton(onClick = { viewModel.toggleFavorite(machine.id) }) {
                                Icon(
                                    imageVector = if (machine.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle favorite status",
                                    tint = if (machine.isFavorite) Color(0xFFEF4444) else (if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8))
                                )
                            }

                            // Dismiss close trigger button
                            IconButton(onClick = { viewModel.selectMachine(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close detailed machine panel slider",
                                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }

                        // Operational Status & Cloud Sync Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (machine.status) {
                                            "DOWN" -> Color(0xFFFEE2E2)
                                            "CROWDED" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFD1FAE5)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (machine.status) {
                                        "DOWN" -> "🔴 Offline: ${machine.lastReportType}"
                                        "CROWDED" -> "🟡 Crowded: ${machine.lastReportType}"
                                        else -> "🟢 Working: ${machine.lastReportType}"
                                    },
                                    color = when (machine.status) {
                                        "DOWN" -> Color(0xFF991B1B)
                                        "CROWDED" -> Color(0xFF92400E)
                                        else -> Color(0xFF065F46)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "ID: ${machine.terminalId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Distances and ETA Row
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RIDER DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8))
                                    Text("${"%.2f".format(distanceKm)} km", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                }

                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ESTIMATED ETA", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8))
                                    Text("$etaMinutes Mins", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5E00))
                                }

                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ROAD SPEED", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8))
                                    Text("$riderSpeed / $speedLimit kmh", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (riderSpeed > speedLimit) Color.Red else (if (isDarkTheme) Color.White else Color(0xFF0F172A)))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Collaborative Dispatcher Notes Text Box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAlt,
                                contentDescription = null,
                                tint = Color(0xFFFF5E00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (!isEditingNotes) {
                                Text(
                                    text = if (machine.notes.isEmpty()) "Tap to write note: e.g. Near Entry gate, cash limits..." else machine.notes,
                                    fontSize = 12.sp,
                                    color = if (machine.notes.isEmpty()) (if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)) else (if (isDarkTheme) Color.White else Color(0xFF334155)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isEditingNotes = true }
                                )
                                IconButton(onClick = { isEditingNotes = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit notes",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = editNotesText,
                                    onValueChange = { editNotesText = it },
                                    placeholder = { Text("Save rider updates...", fontSize = 11.sp) },
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF5E00),
                                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        viewModel.saveMachineNotes(machine.id, editNotesText)
                                        isEditingNotes = false
                                        Toast.makeText(context, "Local dispatch notes saved successfully ✔", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Save", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Action Controls: Simulation start/stop, status report
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status report button
                            OutlinedButton(
                                onClick = { showReportDialog = machine },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Report Crowd / Crash",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)
                                )
                            }

                            // Navigation button
                            Button(
                                onClick = {
                                    if (isNavigating) {
                                        viewModel.cancelActiveNavigation()
                                        Toast.makeText(context, "Navigation route simulation stopped.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.startNavigationSimulation()
                                        Toast.makeText(context, "Initiating GPS route simulation to terminal...", Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isNavigating) Color(0xFFEF4444) else Color(0xFFFF5E00)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("map_navigation_button")
                            ) {
                                Icon(
                                    imageVector = if (isNavigating) Icons.Default.Navigation else Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNavigating) "STOP NAVIGATION" else "GO NAVIGATE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. HAMBURGER SLIDE PANEL / SETTINGS COMPLIANCE MODAL
        // ==========================================
        if (showHamburgerMenu) {
            Dialog(onDismissRequest = { showHamburgerMenu = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("hamburger_dialog_view")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rider profile info section
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5E00).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏍",
                                fontSize = 32.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = viewModel.loggedInUser.collectAsState().value,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        )

                        Text(
                            text = "Central Qatar Dispatcher Authorized",
                            fontSize = 10.sp,
                            color = Color(0xFFFF5E00),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Device / System Sync status badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (firebaseSyncState == "SYNCING") Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (firebaseSyncState == "SYNCING") Color(0xFF3B82F6) else Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (firebaseSyncState == "SYNCING") "Firebase database actively syncing..." else "Firebase DB Sync Online ✔",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (firebaseSyncState == "SYNCING") Color(0xFF1D4ED8) else Color(0xFF15803D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Switch: Theme Switch Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Map Color Theme",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isDarkTheme) "Midnight Neon Mode" else "Waze Daylight Mode",
                                    fontSize = 10.sp,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5E00)),
                                modifier = Modifier.testTag("app_theme_dark_switch")
                            )
                        }

                        // Switch: Biometrics option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFFFF5E00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Lock Screen",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Require fingerprint lock upon start",
                                    fontSize = 10.sp,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = isBiometricsEnabledActive,
                                onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5E00)),
                                modifier = Modifier.testTag("biometric_toggle_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Legal Compliance Section (Highly Requested)
                        Text(
                            text = "PLAY STORE LEGAL COMPLIANCE",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPrivacyPolicySub = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFFF5E00)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Privacy Policy",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF334155)
                                )
                            }

                            OutlinedButton(
                                onClick = { showTermsConditionsSub = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFFF5E00)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Terms of Use",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF334155)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Panel Buttons (Dismiss / Logout)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showHamburgerMenu = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    "Close Settings",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF475569)
                                )
                            }

                            Button(
                                onClick = {
                                    showHamburgerMenu = false
                                    viewModel.logout()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Logout", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. POPUP REPORT MACHINE STATUS DIALOG
        // ==========================================
        showReportDialog?.let { machine ->
            Dialog(onDismissRequest = { showReportDialog = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Report Terminal Status Alert",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "Terminal: #${machine.terminalId} at ${machine.shortBranchName}",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Options
                        listOf(
                            Triple("ACTIVE", "Fully Operational", "🟢 System Working cleanly"),
                            Triple("CROWDED", "Long Waiting Queue", "🟡 Busy: 10+ Mins Backlog"),
                            Triple("DOWN", "Cash Deposit Full", "🔴 Offline: Screen crash or full box")
                        ).forEach { (stat, label, desc) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.reportMachineStatus(machine.id, stat, label)
                                        showReportDialog = null
                                        Toast.makeText(context, "Status submitted successfully. Thank you for notifying other riders!", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(desc, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { showReportDialog = null },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFFF5E00))
                        ) {
                            Text("Cancel", color = Color(0xFFFF5E00))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 9. SUB LEGAL PAGES DISPLAY TRIGGERS
        // ==========================================
        if (showPrivacyPolicySub) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyPolicySub = false })
        }

        if (showTermsConditionsSub) {
            TermsConditionsDialog(onDismiss = { showTermsConditionsSub = false })
        }
    }
}

fun createCustomMarkerIcon(context: Context, status: String, isDark: Boolean): BitmapDescriptor {
    val size = 32 // 32px diameter, sharp and compact
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Select color based on machine status
    val color = when (status) {
        "DOWN" -> if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt() // Red
        "CROWDED" -> if (isDark) 0xFFF59E0B.toInt() else 0xFFD97706.toInt() // Amber
        else -> if (isDark) 0xFF10B981.toInt() else 0xFF059669.toInt() // Emerald Green
    }
    
    // Draw white outer halo
    val borderPaint = Paint().apply {
        isAntiAlias = true
        setStyle(Paint.Style.FILL)
        setColor(0xFFFFFFFF.toInt())
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint)
    
    // Draw inner colored status dot
    val fillPaint = Paint().apply {
        isAntiAlias = true
        setStyle(Paint.Style.FILL)
        setColor(color)
    }
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 3f, fillPaint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
