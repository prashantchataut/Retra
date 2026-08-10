package app.retra.emulator.onboarding

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.emulator.auth.AuthOperation

@Composable
fun ChapterFourKeep(
    authOperation: AuthOperation,
    onGoogleSignIn: () -> Unit,
    onContinueOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Area (55%): Serene Floating Blob + Drifting Memory Remnants
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                contentAlignment = Alignment.Center
            ) {
                // Drifting Memory Remnants in Background
                Text(
                    text = "after school",
                    style = OnboardingTokens.FloatingWord.copy(
                        fontSize = 15.sp,
                        color = OnboardingTokens.TextMuted.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 32.dp, top = 24.dp)
                        .rotate(-8f)
                )

                Text(
                    text = "save state",
                    style = OnboardingTokens.FloatingWord.copy(
                        fontSize = 14.sp,
                        color = OnboardingTokens.ElectricLavender.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 20.dp)
                        .rotate(6f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "retra",
                        style = OnboardingTokens.WordmarkHero.copy(
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                            color = OnboardingTokens.TextPrimary
                        )
                    )

                    // Calm, Centered Mascot Blob
                    RetraBlobMascot(
                        size = 130.dp,
                        primaryAccent = OnboardingTokens.ElectricLavender,
                        secondaryAccent = OnboardingTokens.DreamCyan,
                        interactive = true
                    )
                }
            }

            // Lower Auth Panel: Single Liquid Glass Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color = OnboardingTokens.GlassSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    OnboardingTokens.ElectricLavender.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title & Subtitle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Keep your adventures close.",
                                style = OnboardingTokens.ChapterTitle.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Text(
                                text = "Create a profile to personalize Retra. Games and saves always stay private on this device.",
                                style = OnboardingTokens.ChapterSubtitle.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        // Auth Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Primary: Continue with Google
                            Button(
                                onClick = onGoogleSignIn,
                                enabled = authOperation == AuthOperation.IDLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OnboardingTokens.TextPrimary,
                                    contentColor = OnboardingTokens.MidnightBlack
                                )
                            ) {
                                if (authOperation == AuthOperation.SIGNING_IN) {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = OnboardingTokens.MidnightBlack
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Connecting Google Profile...", style = OnboardingTokens.ButtonCta)
                                } else {
                                    GoogleIcon(Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Continue with Google", style = OnboardingTokens.ButtonCta)
                                }
                            }

                            // Secondary: Continue without account (Offline)
                            OutlinedButton(
                                onClick = onContinueOffline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OnboardingTokens.PlumAtmosphere3)
                            ) {
                                Text(
                                    "Continue without an account",
                                    style = OnboardingTokens.ButtonCta.copy(
                                        fontSize = 15.sp,
                                        color = OnboardingTokens.TextSecondary
                                    )
                                )
                            }
                        }

                        // Privacy Statement
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, null, Modifier.size(14.dp), tint = OnboardingTokens.SaveMint)
                            Text(
                                text = "100% private local storage · Zero analytics",
                                style = OnboardingTokens.MicroLabel.copy(fontSize = 11.sp),
                                color = OnboardingTokens.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean SVG-rendered 4-color Google 'G' Mark conforming to branding standards.
 */
@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w / 2f

        // Blue, Green, Yellow, Red arcs
        val red = Color(0xFFEA4335)
        val blue = Color(0xFF4285F4)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        // Draw outer circular colored arcs
        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 135f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 45f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )
        drawArc(
            color = blue,
            startAngle = 315f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(0f, 0f),
            size = Size(w, h)
        )

        // Cut out center circle
        drawCircle(
            color = OnboardingTokens.TextPrimary,
            radius = radius * 0.58f,
            center = Offset(cx, cy)
        )

        // Horizontal blue bar
        drawRect(
            color = blue,
            topLeft = Offset(cx - radius * 0.05f, cy - radius * 0.22f),
            size = Size(radius * 1.05f, radius * 0.44f)
        )
    }
}
