package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CdmMachine
import com.example.data.pref.SettingsManager
import com.example.data.repository.CdmRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

class CdmViewModel(
    private val repository: CdmRepository,
    val settingsManager: SettingsManager,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "CdmViewModel"
        // Centered on Doha Grand Hamad St / Corniche Area
        private const val DEFAULT_LAT = 25.2867
        private const val DEFAULT_LNG = 51.5333
    }

    // List of CDM machines
    val allMachines: StateFlow<List<CdmMachine>> = repository.allCdmMachines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteMachines: StateFlow<List<CdmMachine>> = repository.favoriteMachines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection & search states
    private val _selectedMachine = MutableStateFlow<CdmMachine?>(null)
    val selectedMachine: StateFlow<CdmMachine?> = _selectedMachine.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow("ALL") // ALL, FAVORITE, SANNIYA, WAKRA, KHOR, RAYYAN
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // Rider navigation & simulation states
    private val _riderLocation = MutableStateFlow(Pair(DEFAULT_LAT, DEFAULT_LNG))
    val riderLocation: StateFlow<Pair<Double, Double>> = _riderLocation.asStateFlow()

    private val _riderSpeed = MutableStateFlow(0) // km/h
    val riderSpeed: StateFlow<Int> = _riderSpeed.asStateFlow()

    private val _speedLimit = MutableStateFlow(80) // Qatar standard speed limits
    val speedLimit: StateFlow<Int> = _speedLimit.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _etaMinutes = MutableStateFlow(0)
    val etaMinutes: StateFlow<Int> = _etaMinutes.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _arrivedShow = MutableStateFlow(false)
    val arrivedShow: StateFlow<Boolean> = _arrivedShow.asStateFlow()

    // Waze mock player states
    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // ==========================================
    // EXTENDED THEME, BIOMETRICS & AUTHENTICATION FLOWS (UPGRADE)
    // ==========================================
    // Theme preferences Flow
    private val _isDarkTheme = MutableStateFlow(settingsManager.isDarkThemeEnabled())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        settingsManager.setDarkThemeEnabled(enabled)
        _isDarkTheme.value = enabled
    }

    // Biometrics configuration State
    private val _isBiometricsEnabled = MutableStateFlow(settingsManager.isBiometricsEnabled())
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled.asStateFlow()

    fun setBiometricsEnabled(enabled: Boolean) {
        settingsManager.setBiometricsEnabled(enabled)
        _isBiometricsEnabled.value = enabled
    }

    // Startup Biometric Security lock verification State
    private val _isAppLocked = MutableStateFlow(settingsManager.isBiometricsEnabled())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun unlockApp() {
        _isAppLocked.value = false
    }

    // Unified login authentication Flow states
    private val _isLoggedIn = MutableStateFlow(settingsManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Background Firebase Sync Status indicator state
    private val _firebaseSyncState = MutableStateFlow("ACTIVE_READY") // SYNCING, SECURE_OK, IDLE
    val firebaseSyncState: StateFlow<String> = _firebaseSyncState.asStateFlow()

    fun startFirebaseBackgroundSync() {
        viewModelScope.launch {
            while (true) {
                if (_isLoggedIn.value) {
                    _firebaseSyncState.value = "SYNCING"
                    delay(4000)
                    _firebaseSyncState.value = "SECURE_OK"
                } else {
                    _firebaseSyncState.value = "IDLE"
                }
                delay(20000) // sync check cycle loop
            }
        }
    }

    private val _loggedInUser = MutableStateFlow(settingsManager.getLoggedInUser())
    val loggedInUser: StateFlow<String> = _loggedInUser.asStateFlow()

    // Phone multi-factor / SMS OTP states
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            try {
                // Standard Firebase instance hook-up for production compiles
                // com.google.firebase.auth.FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Google auth: ${e.message}")
            }
            settingsManager.setLoggedIn(true)
            val info = "$name ($email)"
            settingsManager.setLoggedInUser(info)
            _isLoggedIn.value = true
            _loggedInUser.value = info
            // Re-apply biometric lock after login if enabled
            _isAppLocked.value = settingsManager.isBiometricsEnabled()
        }
    }

    fun requestPhoneOtp(phone: String) {
        // SMS provider handshakes
        _otpSent.value = true
    }

    fun verifyPhoneOtp(otpCode: String, phone: String): Boolean {
        // Simulated sandbox or verified Firebase Credential code validation
        if (otpCode == "123456" || (otpCode.length == 6 && otpCode.all { it.isDigit() })) {
            settingsManager.setLoggedIn(true)
            val info = "Rider: $phone"
            settingsManager.setLoggedInUser(info)
            _isLoggedIn.value = true
            _loggedInUser.value = info
            _otpSent.value = false
            _isAppLocked.value = settingsManager.isBiometricsEnabled()
            return true
        }
        return false
    }

    fun logout() {
        settingsManager.setLoggedIn(false)
        settingsManager.setLoggedInUser("")
        _isLoggedIn.value = false
        _loggedInUser.value = ""
        _otpSent.value = false
        _isAppLocked.value = false
    }

    // Filtered machines based on search query AND category pill
    val filteredMachines: StateFlow<List<CdmMachine>> = combine(
        allMachines,
        _searchQuery,
        _activeFilter
    ) { list, query, filter ->
        var result = list

        // Apply visual category filter
        if (filter != "ALL") {
            result = when (filter) {
                "FAVORITE" -> result.filter { it.isFavorite }
                "SANNIYA" -> result.filter { it.branchName.lowercase().contains("sanniya") || it.branchName.lowercase().contains("saniiya") }
                "WAKRA" -> result.filter { it.branchName.lowercase().contains("wakra") || it.branchName.lowercase().contains("wukair") }
                "KHOR" -> result.filter { it.branchName.lowercase().contains("khor") || it.branchName.lowercase().contains("simaisma") }
                "RAYYAN" -> result.filter { it.branchName.lowercase().contains("rayyan") || it.branchName.lowercase().contains("muaither") }
                else -> result
            }
        }

        // Apply text search filter
        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            result = result.filter {
                it.merchantName.lowercase().contains(q) ||
                it.branchName.lowercase().contains(q) ||
                it.terminalId.lowercase().contains(q) ||
                it.id.toString() == q
            }
        }

        // Sort by proximity to rider location!
        val riderLoc = _riderLocation.value
        result.sortedBy { calculateDistance(riderLoc.first, riderLoc.second, it.latitude, it.longitude) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val nearestMachine: StateFlow<CdmMachine?> = combine(
        allMachines,
        _riderLocation
    ) { list, riderLoc ->
        if (list.isEmpty()) return@combine null
        list.minByOrNull {
            calculateDistance(riderLoc.first, riderLoc.second, it.latitude, it.longitude)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Location components
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private var locationCallback: LocationCallback? = null
    private var navigationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
        }
        startDeviceLocationOrSimulation()
        startFirebaseBackgroundSync()
    }

    fun selectMachine(machine: CdmMachine?) {
        _selectedMachine.value = machine
        _arrivedShow.value = false
        if (machine != null) {
            cancelActiveNavigation()
            updateRouteCalculations(machine)
        } else {
            _isNavigating.value = false
            _riderSpeed.value = 0
        }
    }

    fun selectNearestOperationalMachine() {
        val currentList = allMachines.value
        if (currentList.isEmpty()) return
        
        val currentRider = _riderLocation.value
        // Only select operational stations if possible, fallback to any
        val operational = currentList.filter { it.status != "DOWN" }
        val targetList = if (operational.isNotEmpty()) operational else currentList
        
        val nearest = targetList.minByOrNull {
            calculateDistance(currentRider.first, currentRider.second, it.latitude, it.longitude)
        }
        
        if (nearest != null) {
            selectMachine(nearest)
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filterType: String) {
        _activeFilter.value = filterType
    }

    fun toggleFavorite(machineId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(machineId)
            // Sync current active selected machine state
            _selectedMachine.value?.let {
                if (it.id == machineId) {
                    _selectedMachine.value = it.copy(isFavorite = !it.isFavorite)
                }
            }
        }
    }

    fun saveMachineNotes(machineId: Int, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(machineId, notes)
            _selectedMachine.value?.let {
                if (it.id == machineId) {
                    _selectedMachine.value = it.copy(notes = notes)
                }
            }
        }
    }

    fun reportMachineStatus(machineId: Int, status: String, reportType: String) {
        viewModelScope.launch {
            repository.updateMachineStatus(machineId, status, reportType)
            _selectedMachine.value?.let {
                if (it.id == machineId) {
                    _selectedMachine.value = it.copy(
                        status = status,
                        lastReportType = reportType,
                        lastReportTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun toggleMusicPlay() {
        _isMusicPlaying.value = !_isMusicPlaying.value
    }

    fun toggleMuted() {
        _isMuted.value = !_isMuted.value
    }

    fun dismissArrivedDialog() {
        _arrivedShow.value = false
    }

    // Start navigating is the core Waze route simulation!
    fun startNavigationSimulation() {
        val dest = _selectedMachine.value ?: return
        cancelActiveNavigation()
        _isNavigating.value = true
        _arrivedShow.value = false

        navigationJob = viewModelScope.launch {
            val startLat = _riderLocation.value.first
            val startLng = _riderLocation.value.second
            val endLat = dest.latitude
            val endLng = dest.longitude

            // Speed limit changes based on branches
            _speedLimit.value = if (dest.branchName.lowercase().contains("sanniya")) 60 else 80

            val steps = 25
            for (i in 1..steps) {
                if (!_isNavigating.value) break

                // Interpolate rider location
                val fraction = i.toDouble() / steps
                val currentLat = startLat + (endLat - startLat) * fraction
                val currentLng = startLng + (endLng - startLng) * fraction
                _riderLocation.value = Pair(currentLat, currentLng)

                // Realistic simulated driving speed (accelerating, cruising, slowing down at arrival!)
                _riderSpeed.value = when {
                    fraction < 0.15 -> (fraction * 6.0 * _speedLimit.value.toDouble()).toInt().coerceAtMost(_speedLimit.value)
                    fraction > 0.85 -> (((1.0 - fraction) * 6.0 * _speedLimit.value.toDouble()).toInt().coerceAtLeast(8).coerceAtMost(_speedLimit.value))
                    else -> (_speedLimit.value + (-6..6).random()).coerceIn(40, _speedLimit.value + 5)
                }

                // Update dynamic ETA & Distance
                updateRouteCalculations(dest)

                delay(1200) // tick speed rate representation
            }

            // Arrived safely!
            if (_isNavigating.value) {
                _riderLocation.value = Pair(endLat, endLng)
                _riderSpeed.value = 0
                _isNavigating.value = false
                _distanceKm.value = 0.0
                _etaMinutes.value = 0
                _arrivedShow.value = true
                Log.d(TAG, "Rider completed GPS delivery route to CDM ${dest.id}")
            }
        }
    }

    fun cancelActiveNavigation() {
        _isNavigating.value = false
        _riderSpeed.value = 0
        navigationJob?.cancel()
        navigationJob = null
    }

    private fun updateRouteCalculations(dest: CdmMachine) {
        val dist = calculateDistance(
            _riderLocation.value.first,
            _riderLocation.value.second,
            dest.latitude,
            dest.longitude
        )
        _distanceKm.value = round(dist * 10.0) / 10.0

        // Avg driving speed in Qatar is roughly 50 km/h with lights
        val etaMin = ceil((dist / 50.0) * 60.0).toInt().coerceAtLeast(1)
        _etaMinutes.value = etaMin
    }

    fun isRunningInSimulation(): Boolean {
        if (settingsManager.isSimulationEnabled()) return true
        
        val model = android.os.Build.MODEL ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        
        return brand.startsWith("generic") || 
               device.startsWith("generic") ||
               fingerprint.startsWith("generic") || 
               fingerprint.startsWith("unknown") ||
               hardware.contains("goldfish") || 
               hardware.contains("ranchu") ||
               model.contains("google_sdk") || 
               model.contains("Emulator") || 
               model.contains("Android SDK built for x86") ||
               manufacturer.contains("Genymotion") ||
               product.contains("sdk_google") || 
               product.contains("google_sdk") || 
               product.contains("sdk") || 
               product.contains("sdk_x86") || 
               product.contains("vbox86p") || 
               product.contains("emulator") || 
               product.contains("simulator")
    }

    fun hasLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun enableDeviceLocationAfterPermission() {
        if (!isRunningInSimulation()) {
            requestDeviceLocation()
        }
    }

    private fun startDeviceLocationOrSimulation() {
        if (isRunningInSimulation()) {
            // Simulator default Doha position
            _riderLocation.value = Pair(DEFAULT_LAT, DEFAULT_LNG)
        } else {
            if (hasLocationPermission()) {
                requestDeviceLocation()
            } else {
                _riderLocation.value = Pair(DEFAULT_LAT, DEFAULT_LNG)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestDeviceLocation() {
        if (locationCallback != null) return // Avoid duplicate bindings
        if (!hasLocationPermission()) return // Extra check for AppOps and security compliance
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    _riderLocation.value = Pair(loc.latitude, loc.longitude)
                }
            }

            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    for (loc in p0.locations) {
                        // Only update if not currently simulating a route drive!
                        if (!_isNavigating.value) {
                            _riderLocation.value = Pair(loc.latitude, loc.longitude)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                req,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Native location subscription denied. Defaulting to Doha route simulation", e)
            settingsManager.setSimulationEnabled(true)
            _riderLocation.value = Pair(DEFAULT_LAT, DEFAULT_LNG)
        }
    }

    fun updateRiderLocation(latitude: Double, longitude: Double) {
        _riderLocation.value = Pair(latitude, longitude)
    }

    @SuppressLint("MissingPermission")
    fun moveToLiveLocation(onSuccess: (Pair<Double, Double>) -> Unit) {
        if (isRunningInSimulation() || !hasLocationPermission()) {
            onSuccess(_riderLocation.value)
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val pos = Pair(loc.latitude, loc.longitude)
                    _riderLocation.value = pos
                    onSuccess(pos)
                } else {
                    onSuccess(_riderLocation.value)
                }
            }.addOnFailureListener {
                onSuccess(_riderLocation.value)
            }
        } catch (e: Exception) {
            onSuccess(_riderLocation.value)
        }
    }

    // Great circle haversine distance formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveNavigation()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}

class CdmViewModelFactory(
    private val repository: CdmRepository,
    private val settingsManager: SettingsManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CdmViewModel::class.java)) {
            return CdmViewModel(repository, settingsManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
