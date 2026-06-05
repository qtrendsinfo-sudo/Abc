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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.PathEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: CdmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isMapInitError by remember { mutableStateOf(false) }
    var mapInitErrorMessage by remember { mutableStateOf("") }
    var isMapCrashOccurred by remember { mutableStateOf(false) }
    var mapCrashErrorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(context)
        } catch (e: Throwable) {
            isMapInitError = true
            mapInitErrorMessage = e.localizedMessage ?: "Google Maps SDK Isolated"
        }
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            locationPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            delay(1000)
        }
    }

    // ViewModel State Observers
    val riderLoc by viewModel.riderLocation.collectAsState()
    val riderSpeed by viewModel.riderSpeed.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val isNavigating by viewModel.isNavigating.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val selectedMachine by viewModel.selectedMachine.collectAsState()
    val searchQueryFromVm by viewModel.searchQuery.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(searchQueryFromVm) {
        if (searchQueryFromVm != searchQuery) {
            searchQuery = searchQueryFromVm
        }
    }
    val activeFilter by viewModel.activeFilter.collectAsState()
    val filteredMachines by viewModel.filteredMachines.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsState()
    val isBiometricsEnabledActive by viewModel.isBiometricsEnabled.collectAsState()
    val nearestMachine by viewModel.nearestMachine.collectAsState()

    val fusedLocationClient = remember {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    }

    var userLiveLocation by remember {
        mutableStateOf(LatLng(riderLoc.first, riderLoc.second))
    }

    var deviceHeading by remember { mutableStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val sensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ORIENTATION)
        val listener = object : android.hardware.SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationValues = FloatArray(3)
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                event ?: return
                if (event.sensor.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                    try {
                        android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        android.hardware.SensorManager.getOrientation(rotationMatrix, orientationValues)
                        val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        deviceHeading = (azimuthDegrees + 360f) % 360f
                    } catch (e: Throwable) {
                        // Safe fallback
                    }
                } else {
                    deviceHeading = event.values[0]
                }
            }
            override fun onAccuracyChanged(s: android.hardware.Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(riderLoc) {
        userLiveLocation = LatLng(riderLoc.first, riderLoc.second)
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    2000L
                )
                .setMinUpdateIntervalMillis(1000L)
                .build()

                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        val lastLocation = locationResult.lastLocation
                        if (lastLocation != null) {
                            val newLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                            userLiveLocation = newLatLng
                            viewModel.updateRiderLocation(lastLocation.latitude, lastLocation.longitude)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                // Safe fallthrough
            } catch (e: Throwable) {
                // Safe fallthrough
            }
        }
    }

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

    // Live Geocoding, Places & instant keyword panning/centering effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.trim().length >= 3) {
            val q = searchQuery.trim()
            // Prefer immediate exact/partial name match of CMD terminals
            val quickMatch = filteredMachines.firstOrNull {
                it.merchantName.lowercase().contains(q.lowercase()) ||
                it.branchName.lowercase().contains(q.lowercase())
            }
            if (quickMatch != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(quickMatch.latitude, quickMatch.longitude), 15f),
                    800
                )
            } else {
                // Fallback to on-device background system Geocoder for cities, highways or standard streets in Qatar
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                        val results = geocoder.getFromLocationName("$q, Qatar", 1)
                        if (!results.isNullOrEmpty()) {
                            val address = results[0]
                            val target = LatLng(address.latitude, address.longitude)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(target, 14f),
                                    1000
                                )
                            }
                        } else {
                            // Instant local geocode dictionary keywords mapping to optimize speed and offline use
                            val lowerQuery = q.lowercase()
                            val fallbackCoord = when {
                                "pearl" in lowerQuery -> LatLng(25.3700, 51.5500)
                                "lusail" in lowerQuery -> LatLng(25.4200, 51.5200)
                                "west bay" in lowerQuery || "westbay" in lowerQuery -> LatLng(25.3200, 51.5300)
                                "airport" in lowerQuery || "hamad" in lowerQuery -> LatLng(25.2600, 51.5600)
                                "al sadd" in lowerQuery || "sadd" in lowerQuery -> LatLng(25.2800, 51.5000)
                                "corniche" in lowerQuery -> LatLng(25.2900, 51.5300)
                                "sanniya" in lowerQuery || "industrial" in lowerQuery -> LatLng(25.1800, 51.4200)
                                "wakra" in lowerQuery -> LatLng(25.1700, 51.6000)
                                "khor" in lowerQuery -> LatLng(25.6800, 51.5000)
                                "rayyan" in lowerQuery -> LatLng(25.3000, 51.4500)
                                else -> null
                            }
                            fallbackCoord?.let { target ->
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(target, 14f),
                                        1000
                                    )
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        // Resilient geocoder failure bypass
                    }
                }
            }
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

        val mapProperties = remember(isDarkTheme, locationPermissionGranted) {
            MapProperties(
                isMyLocationEnabled = locationPermissionGranted
            )
        }

        if (!isMapInitError && !isMapCrashOccurred) {
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
                // Draw custom marker representing the rider's physical spot on the Google Map
                val markerIconRider = remember(isDarkTheme) { createCustomMarkerIcon(context, "RIDER", isDarkTheme) }
                val riderMarkerState = rememberMarkerState(position = userLiveLocation)
                LaunchedEffect(userLiveLocation) {
                    riderMarkerState.position = userLiveLocation
                }
                Marker(
                    state = riderMarkerState,
                    title = "My Position",
                    snippet = "Current Location",
                    icon = markerIconRider,
                    rotation = deviceHeading,
                    flat = true,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 500f
                )

                // Draw real-time dynamic polyline path to the selected CDM terminal when navigating
                if (isNavigating && selectedMachine != null) {
                    val destination = LatLng(selectedMachine!!.latitude, selectedMachine!!.longitude)
                    val routePoints = remember(userLiveLocation, destination) {
                        generateRoutePoints(userLiveLocation, destination)
                    }
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF00B0FF), // Sleek, modern neon-azure blue (#00B0FF)
                        width = 8f,
                        jointType = com.google.android.gms.maps.model.JointType.ROUND
                    )
                }

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
                            
                            // Smoothly center the camera over selected terminal marker location
                            scope.launch {
                                try {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLng(LatLng(machine.latitude, machine.longitude)),
                                        500
                                    )
                                } catch (e: Throwable) {
                                    // Ignore camera animation failure
                                }
                            }
                            
                            // Instantly draw the active neon navigation route line from rider's spot
                            viewModel.startNavigationSimulation()
                            true
                        }
                    )
                }
            }
        } else {
            CdmSimulatedMapFallback(
                isDarkTheme = isDarkTheme,
                filteredMachines = filteredMachines,
                riderLoc = riderLoc,
                selectedMachine = selectedMachine,
                nearestMachine = nearestMachine,
                onMachineSelect = { machine ->
                    viewModel.selectMachine(machine)
                    editNotesText = machine?.notes ?: ""
                    isEditingNotes = false
                    if (machine != null) {
                        viewModel.startNavigationSimulation()
                    }
                },
                errorMessage = if (isMapInitError) mapInitErrorMessage else mapCrashErrorMessage
            )
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
                        onValueChange = {
                            searchQuery = it
                            viewModel.search(it)
                            showDropdown = it.isNotBlank()
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        ),
                        placeholder = {
                            Text(
                                "Search Talabat positions in Qatar...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
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
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.search("")
                            showDropdown = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
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

            // ==========================================
            // SLEEK FLOATING DROPDOWN SUGGESTIONS MENU COUPLING
            // ==========================================
            Box(
                modifier = Modifier.fillMaxWidth().zIndex(999f),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showDropdown && searchQuery.trim().length >= 1 && filteredMachines.isNotEmpty(),
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .heightIn(max = 280.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredMachines) { machine ->
                                val dist = viewModel.calculateDistance(
                                    riderLoc.first, riderLoc.second,
                                    machine.latitude, machine.longitude
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectMachine(machine)
                                            editNotesText = machine.notes
                                            isEditingNotes = false
                                            
                                            // Hide dropdown on click
                                            showDropdown = false
                                            
                                            scope.launch {
                                                try {
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                            LatLng(machine.latitude, machine.longitude),
                                                            15f
                                                        ),
                                                        1000
                                                    )
                                                } catch (e: Throwable) {}
                                            }
                                            
                                            // Trigger navigation and path routing line drawing
                                            viewModel.startNavigationSimulation()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Suggested Machine",
                                        tint = Color(0xFFFF5E00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = machine.merchantName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = machine.branchName,
                                            fontSize = 11.sp,
                                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${"%.2f".format(dist)} km",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5E00)
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9)))
                            }
                        }
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
                .padding(bottom = if (selectedMachine != null) 420.dp else 24.dp)
                .zIndex(10f)
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnlineRider) Color(0xFF00C853) else Color(0xFF64748B)
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
                // Instantly trigger camera frame animation to current rider tracker dot on UI thread
                scope.launch {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLiveLocation, 16f),
                            800
                        )
                    } catch (e: Throwable) {
                        // Safe exception fallthrough
                    }
                }
                // Async fallback to fetch any freshly requested hardware GPS coordinates
                viewModel.moveToLiveLocation { location ->
                    scope.launch {
                        try {
                            val targetLatLng = LatLng(location.first, location.second)
                            userLiveLocation = targetLatLng
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(targetLatLng, 16f),
                                800
                            )
                        } catch (e: Throwable) {
                            // Safe fallback
                        }
                    }
                }
            },
            containerColor = Color.White,
            contentColor = Color(0xFFFF5E00),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = if (selectedMachine != null) 420.dp else 24.dp, end = 16.dp)
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
                        .height(410.dp)
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
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("map_navigation_button")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
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
                                            lineHeight = 13.sp,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time external navigation launcher
                        Button(
                            onClick = {
                                try {
                                    val uri = "waze://?ll=${machine.latitude},${machine.longitude}&navigate=yes"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webUri = "https://waze.com/ul?ll=${machine.latitude},${machine.longitude}&navigate=yes"
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                                    context.startActivity(webIntent)
                                    Toast.makeText(context, "Opening Waze Live Routing...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00C853), // Standard Talabat Green Match
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("waze_navigation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "OPEN IN WAZE ROUTING APP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
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

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch: Theme Switch Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.testTag("app_theme_dark_switch")
                            )
                        }

                        // Switch: Biometrics option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00C853),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.testTag("biometric_toggle_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

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
                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

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

fun createCustomMarkerIcon(context: Context, status: String, isDark: Boolean): BitmapDescriptor? {
    return try {
        // Explicitly initialize the Maps SDK to ensure BitmapDescriptorFactory is ready
        com.google.android.gms.maps.MapsInitializer.initialize(context)
        
        if (status == "RIDER") {
            // Highly-visible maps/gps blue location tracker with concentric outer glow & circular pulses
            val size = 120 // 120px canvas to accommodate the glowing directional compass beam/shield
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val centerX = size / 2f
            val centerY = size / 2f
            
            // 1. Semi-transparent Glowing Directional Beam Cone
            val conePath = android.graphics.Path().apply {
                moveTo(centerX, centerY)
                lineTo(centerX - 35f, 8f) // Left wavefront point
                quadTo(centerX, -4f, centerX + 35f, 8f) // Elegant custom round wave cone projection curvature
                close()
            }
            
            val conePaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
            }
            
            // Neon azure gradient that starts bright near the dot and fades with distance
            conePaint.shader = android.graphics.LinearGradient(
                centerX, centerY, centerX, 0f,
                intArrayOf(0xDA00B0FF.toInt(), 0x3300B0FF.toInt(), 0x0000B0FF.toInt()),
                floatArrayOf(0.0f, 0.6f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawPath(conePath, conePaint)
            
            // Subtle high-tech highlight stroke border around the directional beam
            val coneStrokePaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.STROKE)
                setStrokeWidth(1.5f)
            }
            coneStrokePaint.shader = android.graphics.LinearGradient(
                centerX, centerY, centerX, 0f,
                intArrayOf(0x5500B0FF.toInt(), 0x0000B0FF.toInt()),
                floatArrayOf(0.0f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawPath(conePath, coneStrokePaint)
            
            // 2. Double Concentric Pulsing Halos (Translucent neon-azure glow)
            val outerHaloPaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(0x2200B0FF.toInt()) // 13% opacity outer pulse
            }
            canvas.drawCircle(centerX, centerY, 40f, outerHaloPaint)
            
            val innerHaloPaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(0x4400B0FF.toInt()) // 26% opacity inner pulse
            }
            canvas.drawCircle(centerX, centerY, 26f, innerHaloPaint)
            
            // 3. Crisp White Inner Boundary Ring
            val whitePaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(0xFFFFFFFF.toInt())
            }
            canvas.drawCircle(centerX, centerY, 18f, whitePaint)
            
            // 4. Crisp Signature GPS Blue Core Dot
            val corePaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(0xFF007AFF.toInt()) // Elite GPS Navigation Blue Core
            }
            canvas.drawCircle(centerX, centerY, 13f, corePaint)
            
            BitmapDescriptorFactory.fromBitmap(bitmap)
        } else {
            val width = 72
            val height = 90
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val cx = width / 2f
            val cy = height * 0.4f
            val r = width * 0.35f
            
            val baseColor = when (status) {
                "DOWN" -> 0xFFEF4444.toInt() // Coral Red
                "CROWDED" -> 0xFFF59E0B.toInt() // Busy Amber
                else -> 0xFFFF5E00.toInt() // Signature Talabat Orange
            }
            
            // Draw a subtle soft shadow oval under the pin tip
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(0x33000000)
            }
            canvas.drawOval(cx - 14f, height - 12f, cx + 14f, height - 2f, shadowPaint)
            
            // Modern location pin path pointing directly down
            val path = android.graphics.Path()
            path.moveTo(cx, height * 0.9f)
            path.lineTo(cx - 18f, cy + 10f)
            path.arcTo(cx - r, cy - r, cx + r, cy + r, 145f, 250f, false)
            path.close()
            
            val pinPaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(baseColor)
            }
            canvas.drawPath(path, pinPaint)
            
            // Premium Crisp White border outline
            val borderPaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.STROKE)
                setStrokeWidth(3.5f)
                setColor(0xFFFFFFFF.toInt())
            }
            canvas.drawPath(path, borderPaint)
            
            // Teal Inner core container for ACTIVE machines to create the stylized orange/teal visual identity
            val innerCorePaint = Paint().apply {
                isAntiAlias = true
                setStyle(Paint.Style.FILL)
                setColor(if (status == "ACTIVE") 0xFF00B1A9.toInt() else 0xFFFFFFFF.toInt())
            }
            canvas.drawCircle(cx, cy, r * 0.6f, innerCorePaint)
            
            // Mini colored dot or stylish text inside
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = if (status == "ACTIVE") 0xFFFFFFFF.toInt() else baseColor
                textSize = 10f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("CMD", cx, cy + 3.5f, textPaint)
            
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    } catch (e: Throwable) {
        // Fallback safely to default marker if SDK or bitmap generation fails
        null
    }
}

@Composable
fun CdmSimulatedMapFallback(
    isDarkTheme: Boolean,
    filteredMachines: List<CdmMachine>,
    riderLoc: Pair<Double, Double>,
    selectedMachine: CdmMachine?,
    nearestMachine: CdmMachine?,
    onMachineSelect: (CdmMachine?) -> Unit,
    errorMessage: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    
    // Rotating sweep beam angle
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Pulsing halo scale around rider
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
    ) {
        // Safe graphic tracker
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(filteredMachines) {
                    detectTapGestures { offset ->
                        // Determine bounds
                        val currentMinLat = filteredMachines.minOfOrNull { it.latitude } ?: 25.20
                        val currentMaxLat = filteredMachines.maxOfOrNull { it.latitude } ?: 25.35
                        val currentMinLng = filteredMachines.minOfOrNull { it.longitude } ?: 51.45
                        val currentMaxLng = filteredMachines.maxOfOrNull { it.longitude } ?: 51.58

                        val latDelta = if ((currentMaxLat - currentMinLat) > 0.001) currentMaxLat - currentMinLat else 0.1
                        val lngDelta = if ((currentMaxLng - currentMinLng) > 0.001) currentMaxLng - currentMinLng else 0.1

                        val padX = size.width * 0.12f
                        val padY = size.height * 0.12f
                        val drawW = size.width - 2f * padX
                        val drawH = size.height - 2f * padY

                        var closestMachine: CdmMachine? = null
                        var minDistance = Float.MAX_VALUE

                        filteredMachines.forEach { machine ->
                            val pctY = 1f - ((machine.latitude - currentMinLat) / latDelta).toFloat()
                            val pctX = ((machine.longitude - currentMinLng) / lngDelta).toFloat()

                            val mX = padX + pctX * drawW
                            val mY = padY + pctY * drawH

                            val dist = kotlin.math.sqrt((offset.x - mX) * (offset.x - mX) + (offset.y - mY) * (offset.y - mY))
                            if (dist < minDistance && dist < 45f) { // 45px threshold for easy interactive tap
                                minDistance = dist
                                closestMachine = machine
                            }
                        }
                        
                        // If tapped far away, clear selection
                        if (closestMachine != null) {
                            onMachineSelect(closestMachine)
                        } else {
                            onMachineSelect(null)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Grid Lines
            val gridColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            val gridSpacing = 120.dp.toPx()
            
            var currentX = 0f
            while (currentX < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(currentX, 0f),
                    end = Offset(currentX, height),
                    strokeWidth = 1f
                )
                currentX += gridSpacing
            }
            
            var currentY = 0f
            while (currentY < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, currentY),
                    end = Offset(width, currentY),
                    strokeWidth = 1f
                )
                currentY += gridSpacing
            }

            // Define scale bounds based on active points
            val currentMinLat = filteredMachines.minOfOrNull { it.latitude } ?: 25.20
            val currentMaxLat = filteredMachines.maxOfOrNull { it.latitude } ?: 25.35
            val currentMinLng = filteredMachines.minOfOrNull { it.longitude } ?: 51.45
            val currentMaxLng = filteredMachines.maxOfOrNull { it.longitude } ?: 51.58

            val latDelta = if ((currentMaxLat - currentMinLat) > 0.001) currentMaxLat - currentMinLat else 0.1
            val lngDelta = if ((currentMaxLng - currentMinLng) > 0.001) currentMaxLng - currentMinLng else 0.1

            val padX = width * 0.12f
            val padY = height * 0.12f
            val drawW = width - 2f * padX
            val drawH = height - 2f * padY

            // 2. Map coordinates of rider
            val riderPctY = 1f - ((riderLoc.first - currentMinLat) / latDelta).toFloat()
            val riderPctX = ((riderLoc.second - currentMinLng) / lngDelta).toFloat()
            val rx = padX + riderPctX * drawW
            val ry = padY + riderPctY * drawH

            // 3. Draw concentric radar waves around rider
            val radarColor = Color(0xFFFF5E00).copy(alpha = 0.12f)
            drawCircle(color = radarColor, center = Offset(rx, ry), radius = 60.dp.toPx(), style = Stroke(width = 2f))
            drawCircle(color = radarColor, center = Offset(rx, ry), radius = 130.dp.toPx(), style = Stroke(width = 2f))
            drawCircle(color = radarColor, center = Offset(rx, ry), radius = 220.dp.toPx(), style = Stroke(width = 2f))

            // 4. Draw Rotating Sweep Line
            val sweepColor = Color(0xFFFF5E00).copy(alpha = 0.18f)
            val radiusSweep = 280.dp.toPx()
            val angleRad = Math.toRadians(sweepAngle.toDouble())
            val endX = rx + (radiusSweep * Math.cos(angleRad)).toFloat()
            val endY = ry + (radiusSweep * Math.sin(angleRad)).toFloat()
            drawLine(
                color = sweepColor,
                start = Offset(rx, ry),
                end = Offset(endX, endY),
                strokeWidth = 3f
            )

            // 5. Draw dashed path to selected machine
            if (selectedMachine != null) {
                val selPctY = 1f - ((selectedMachine.latitude - currentMinLat) / latDelta).toFloat()
                val selPctX = ((selectedMachine.longitude - currentMinLng) / lngDelta).toFloat()
                val sx = padX + selPctX * drawW
                val sy = padY + selPctY * drawH

                drawLine(
                    color = Color(0xFFFF5E00),
                    start = Offset(rx, ry),
                    end = Offset(sx, sy),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )

                // Highlight boundary lock
                drawCircle(
                    color = Color(0xFFFF5E00).copy(alpha = 0.3f),
                    center = Offset(sx, sy),
                    radius = 20.dp.toPx(),
                    style = Stroke(width = 3f)
                )
            }

            // 6. Plot all machine nodes
            filteredMachines.forEach { machine ->
                val pctY = 1f - ((machine.latitude - currentMinLat) / latDelta).toFloat()
                val pctX = ((machine.longitude - currentMinLng) / lngDelta).toFloat()
                val mx = padX + pctX * drawW
                val my = padY + pctY * drawH

                val primaryColor = when (machine.status) {
                    "DOWN" -> Color(0xFFEF4444)
                    "CROWDED" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }

                // Core Dot
                drawCircle(
                    color = Color.White,
                    center = Offset(mx, my),
                    radius = 8.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    center = Offset(mx, my),
                    radius = 5.5.dp.toPx()
                )
            }

            // 7. Draw Active Rider Spot with pulsing aura shadow
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.3f - (pulseScale - 0.8f) * 0.15f),
                center = Offset(rx, ry),
                radius = 24.dp.toPx() * pulseScale
            )
            drawCircle(
                color = Color.White,
                center = Offset(rx, ry),
                radius = 9.dp.toPx()
            )
            drawCircle(
                color = Color(0xFF3B82F6),
                center = Offset(rx, ry),
                radius = 6.dp.toPx()
            )
        }

        // 8. Warning Callout Ribbon / Info Alert Banner at Bottom Center
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 96.dp) // Leave clean spacing above lower detail panels
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFF5E00).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Safe Map Mode Indicator",
                        tint = Color(0xFFFF5E00)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Tactical CDM Safe Tracker Enabled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Google Maps initializing on physical hardware is sandboxed cleanly. System coordinates are projected dynamically.",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

fun generateRoutePoints(start: LatLng, end: LatLng): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(start)
    
    val latDiff = end.latitude - start.latitude
    val lngDiff = end.longitude - start.longitude
    
    val p1 = LatLng(start.latitude + latDiff * 0.35, start.longitude)
    val p2 = LatLng(start.latitude + latDiff * 0.35, start.longitude + lngDiff * 0.5)
    val p3 = LatLng(start.latitude + latDiff * 0.70, start.longitude + lngDiff * 0.5)
    val p4 = LatLng(start.latitude + latDiff * 0.70, end.longitude)
    
    points.add(p1)
    points.add(p2)
    points.add(p3)
    points.add(p4)
    points.add(end)
    return points
}

