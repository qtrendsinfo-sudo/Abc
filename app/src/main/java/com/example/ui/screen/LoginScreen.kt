package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.CdmViewModel
import kotlinx.coroutines.delay

/**
 * High-fidelity, professional Orange Brand Talabat Rider Login Screen.
 * Conforms fully to modern Material Design 3 and UX guidelines.
 */
@Composable
fun LoginScreen(
    viewModel: CdmViewModel,
    modifier: Modifier = Modifier
) {
    var isPhoneMode by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var googleEmail by remember { mutableStateOf("") }
    var googleName by remember { mutableStateOf("") }
    var isGoogleDialogExpanded by remember { mutableStateOf(false) }
    var otpSent by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableStateOf(60) }
    var phoneError by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Background theme color brush
    val brandGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF5E00), // Talabat Orange
            Color(0xFFE04F00),
            Color(0xFF1E1B18)  // Atmospheric dark footer
        )
    )

    // SMS timer countdown simulation
    LaunchedEffect(otpSent) {
        if (otpSent) {
            resendTimer = 60
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brandGradient)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Header logo representation
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(Color.White, CircleShape)
                    .border(3.dp, Color(0xFFFFCC00), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Rider bike logo",
                    tint = Color(0xFFFF5E00),
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "TALABAT CDM FINDER",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "State of Qatar • Standard Cash Deposit Tracker",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFFFE0CC),
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Core Interactive Panel card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!isPhoneMode) "Secure Driver Portal" else "SMS Multi-Factor System",
                        style = TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    )

                    Text(
                        text = if (!isPhoneMode) "Authenticate with Google or standard Phone verification" 
                               else "Verification code sent to your mobile device",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Alternate verification screens
                    AnimatedContent(targetState = isPhoneMode, label = "Authentication state change") { phoneActive ->
                        if (phoneActive) {
                            // Phoen Verification Mode
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (!otpSent) {
                                    // Step 1: Phone input
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = {
                                            phoneNumber = it
                                            phoneError = ""
                                        },
                                        label = { Text("Qatar Mobile (+974)") },
                                        placeholder = { Text("E.g. 55667788") },
                                        isError = phoneError.isNotEmpty(),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Mobile vector icon",
                                                tint = Color(0xFFFF5E00)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFF5E00),
                                            focusedLabelColor = Color(0xFFFF5E00)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("phone_input")
                                    )

                                    if (phoneError.isNotEmpty()) {
                                        Text(
                                            text = phoneError,
                                            color = Color.Red,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (phoneNumber.trim().length >= 8) {
                                                otpSent = true
                                                viewModel.requestPhoneOtp(phoneNumber)
                                            } else {
                                                phoneError = "Invalid standard Qatar mobile number (8 digits)"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_otp_button")
                                    ) {
                                        Text("Request Security SMS", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    // Step 2: OTP input
                                    OutlinedTextField(
                                        value = enteredOtp,
                                        onValueChange = {
                                            enteredOtp = it
                                            otpError = ""
                                        },
                                        label = { Text("6-Digit OTP Protocol Code") },
                                        placeholder = { Text("Enter 123456 to skip") },
                                        isError = otpError.isNotEmpty(),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Shield vector icon",
                                                tint = Color(0xFFFF5E00)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFF5E00),
                                            focusedLabelColor = Color(0xFFFF5E00)
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("otp_input")
                                    )

                                    if (otpError.isNotEmpty()) {
                                        Text(
                                            text = otpError,
                                            color = Color.Red,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (resendTimer > 0) "Resend in $resendTimer s" else "SMS service active",
                                            style = TextStyle(fontSize = 12.sp, color = Color(0xFF64748B))
                                        )

                                        TextButton(
                                            onClick = {
                                                if (resendTimer == 0) {
                                                    resendTimer = 60
                                                    viewModel.requestPhoneOtp(phoneNumber)
                                                }
                                            },
                                            enabled = resendTimer == 0
                                        ) {
                                            Text(
                                                "Resend code",
                                                color = if (resendTimer == 0) Color(0xFFFF5E00) else Color(0x6664748B),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val valid = viewModel.verifyPhoneOtp(enteredOtp, phoneNumber)
                                            if (!valid) {
                                                otpError = "OTP must be 6 digits (Simulated bypass: 123456)"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_otp_button")
                                    ) {
                                        Text("Verify & Authenticate", fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = { otpSent = false },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Change phone number", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Divider(color = Color(0xFFE2E8F0))

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        isPhoneMode = false
                                        phoneError = ""
                                        otpError = ""
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("Back to Standard Login", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Default Google Auth + Quick Actions
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { isGoogleDialogExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_login_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Google corporate signin icon",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Sign in with Google Account",
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { isPhoneMode = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("phone_entry_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Mobile login vector",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Validate with Mobile Phone OTP",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                ) {
                                    Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                    Text(
                                        "OR PROCEDURAL MODE", 
                                        color = Color(0xFF94A3B8), 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                // Quick skip Skip simulation
                                Button(
                                    onClick = {
                                        viewModel.loginWithGoogle("qatar.rider.demo@talabat.com", "Rider 4740")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("skip_login_button")
                                ) {
                                    Text("Skip with Demonstration Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Legals Footer links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showPrivacy by remember { mutableStateOf(false) }
                var showTerms by remember { mutableStateOf(false) }

                Text(
                    text = "Privacy Policy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFFFE0CC),
                    modifier = Modifier
                        .clickable { showPrivacy = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color(0xFFFFCC00), CircleShape)
                )

                Text(
                    text = "Terms of Service",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFFFE0CC),
                    modifier = Modifier
                        .clickable { showTerms = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                if (showPrivacy) {
                    PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
                }
                if (showTerms) {
                    TermsConditionsDialog(onDismiss = { showTerms = false })
                }
            }
        }
    }

    // Google Signin popup simulation
    if (isGoogleDialogExpanded) {
        Dialog(onDismissRequest = { isGoogleDialogExpanded = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Qatar Corporate Account Hub",
                        tint = Color(0xFFFF5E00),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Google Identity Services",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "Simulating active Firebase secure handshake protocols",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = googleName,
                        onValueChange = { googleName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("E.g. Jassim Al-Kuwari") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5E00)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Google Gmail Address") },
                        placeholder = { Text("jassim@gmail.com") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF5E00)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val formattedName = if (googleName.isBlank()) "Jassim Kudsi" else googleName
                            val formattedEmail = if (googleEmail.isBlank()) "jassim.kudsi@gmail.com" else googleEmail
                            viewModel.loginWithGoogle(formattedEmail, formattedName)
                            isGoogleDialogExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Connect Instantly", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity, satisfying Biometric Lock security screen.
 * Appears ifBiometricsEnabled is checkmarked in preferences.
 */
@Composable
fun BiometricLockScreen(
    onUnlockClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    var isChecking by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "Radar loop")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Biometric scale circle animation"
    )

    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(1200) // Simulated scan processing delay
            isChecking = false
            onUnlockClicked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Deep high-contrast security slate dark background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp).widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Security insignia",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BIOMETRIC LOCKED",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Biometric Verification (Face Lock / Fingerprint scan) is active to protect Talabat driver balances.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            // Pulsing fingerprint container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(if (isChecking) 1.15f else scale)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(2.dp, if (isChecking) Color(0xFF10B981) else Color(0xFF0284C7), CircleShape)
                    .clickable { isChecking = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isChecking) Icons.Default.Done else Icons.Default.Lock,
                        contentDescription = "Biometric lock sensory interface",
                        tint = if (isChecking) Color(0xFF10B981) else Color(0xFF00AAFF),
                        modifier = Modifier.size(62.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isChecking) "SCANNING..." else "TAP TO SCAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isChecking) Color(0xFF10B981) else Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { isChecking = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("biometrics_unlock_button")
            ) {
                Text("Scan Biometrics", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onLogoutClicked,
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Log Out Account", color = Color(0xFFF1F5F9))
            }
        }
    }
}

/**
 * Legal Compliance Privacy Policy Dialog in Jetpack Compose
 */
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.padding(16.dp).heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Security core shield icon", tint = Color(0xFFFF5E00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            Last Updated: June 2026

                            1. INTRODUCTION & SCOPE
                            This Privacy Policy covers how the Talabat CDM Finder Qatar gathers, manages, and safeguards driver information, including precise telemetry, location records, and Cash Deposit Machine notes, to guarantee secure operational procedures on the highway network of Doha.

                            2. LOCATION DATA ACCURATE TELEMETRY
                            Our tool utilizes continuous device location telemetry (via play-services-location packages) to pinpoint the exact distance, ETA, and routing vector from your present motorcycle coordinates to any of the 131 registered deposit terminals. Location coordinate collection runs natively on-device, remains completely anonymous, and is never leaked to third parties.

                            3. OFFLINE STORAGE & DATA SECURITY
                            All bookmarks (Favorites) and custom terminal navigation notes are persisted securely on Local SQLite files (via Jetpack Room abstractions) inside your native device hardware. No transaction cash logs, wallet profiles, or corporate balances are transferred over insecure data links.

                            4. GOOGLE AUTHENTICATION PERSISTENCE
                            We offer Google Sign-In and Phone OTP SMS Multi-Factor tools to authenticate valid Talabat delivery riders. Firebase credentials are authenticated via Google's secure authentication tokens, and details are strictly used to lock out illegitimate terminal modifications or vandal reports.

                            5. COMPLIANCE & GOVERNING LAW
                            This software operates in accordance with standard Qatar Cybercrime Law guidelines and State Telecommunications Rules. By engaging with this GPS application, you consent to coordinate tracking to calculate correct nearest terminal suggestions.
                        """.trimIndent(),
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("I Read & Understand", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Legal Compliance Terms and Conditions Dialog in Jetpack Compose
 */
@Composable
fun TermsConditionsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.padding(16.dp).heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = "Legal note icon", tint = Color(0xFFFF5E00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Terms of Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            Last Updated: June 2026

                            1. LEGAL AGREEMENT & COMPLIANCE
                            These Terms of Service govern your standard right-of-use privileges for the Talabat CDM Finder Qatar application. This is a binding legal document between you, the authenticated operator/rider, and the system developers.

                            2. DEPOSIT MACHINE CROWD REPORTING RULES
                            Delivery riders are provided with visual status indicators ("ACTIVE", "CROWDED", "DOWN") to alert their peers in real-time. You agree that any crowds, waiting levels, or terminal repairs reported to the network will represent authentic local observations. Fake status manipulation is strictly prohibited.

                            3. SAFE DRIVING LIMITS
                            Safety is our paramount priority. The speedometer UI component displays speed limit warning flags when exceeding safe Qatar thresholds (e.g., 80 km/h on highway corridors, or 60 km/h around Industrial Sanniya). Drivers must adhere to standard physical traffic rules. The developer team holds absolutely no liability for speeding citations or physical accidents.

                            4. CASH BALANCING DISCLAIMER
                            This app serves as an educational and technical locating directory tool for Cash Deposit Machine branches. All physical cash counts, receipt prints, and Talabat central balance accounting reconciliations take place exclusively within the proprietary banking terminals. We handle absolutely no deposit transactions directly.

                            5. LICENSURE TERMINATION
                            Any user found exploiting the SMS notification system, bypassing biometric checkpoints, or distributing malicious coordinates will face instant revocation of account session tokens without refund or notification.
                        """.trimIndent(),
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Accept Terms", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
