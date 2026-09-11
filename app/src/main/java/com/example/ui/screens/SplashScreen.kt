package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Province
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: StudyViewModel) {
    val scale = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(1200)
        if (viewModel.isAdminLoggedIn.value) {
            viewModel.navigateTo(Screen.ADMIN_PANEL, addToBackStack = false)
        } else if (viewModel.isUserLoggedIn.value) {
            viewModel.navigateTo(Screen.HOME, addToBackStack = false)
        } else {
            viewModel.navigateTo(Screen.LOGIN, addToBackStack = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Emerald800,
                        Emerald900,
                        Color(0xFF022C22)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Logo Display with Premium Framing & Glow
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .scale(scale.value)
                    .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Emerald300)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(3.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "ON Study App Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "ON Study",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pakistan Academic Guides & Textbooks",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Emerald100,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Class 1 – 12 • All Boards Curriculum",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Emerald200,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Province tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProvinceBadge("KPK", Province.KPK.primaryColor)
                ProvinceBadge("Punjab", Province.PUNJAB.primaryColor)
                ProvinceBadge("Balochistan", Province.BALOCHISTAN.primaryColor)
                ProvinceBadge("Sindh", Province.SINDH.primaryColor)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (viewModel.isAdminLoggedIn.value) {
                        viewModel.navigateTo(Screen.ADMIN_PANEL, addToBackStack = false)
                    } else if (viewModel.isUserLoggedIn.value) {
                        viewModel.navigateTo(Screen.HOME, addToBackStack = false)
                    } else {
                        viewModel.navigateTo(Screen.LOGIN, addToBackStack = false)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Emerald700
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                    .testTag("splash_start_button")
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ProvinceBadge(name: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
