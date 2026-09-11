package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    viewModel: StudyViewModel,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                if (!showBackButton && title == "ON Study") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // App Logo container with official branding
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "ON Study Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = Emerald300)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(
                                text = "ON Study",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = Emerald900
                                )
                            )
                            Text(
                                text = "All Boards • KPK • Punjab • Sindh • Balochistan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate600
                                )
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.2).sp,
                                color = Slate900
                            ),
                            maxLines = 1
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .testTag("top_bar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            actions = {
                actions()
                IconButton(
                    onClick = { viewModel.showMoreBottomSheet.value = true },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                        .testTag("top_bar_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        tint = Slate800,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Slate900,
                navigationIconContentColor = Slate900,
                actionIconContentColor = Slate900
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheet(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val unreadNewsCount by viewModel.unreadNewsCount.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Emerald600),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ON Study Hub",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "Study Tools & Management",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate700, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The Main Menu options:
            MoreMenuItem(
                title = "General Books Library",
                subtitle = "English Grammar, Health Care & First Aid, GK, Science",
                icon = Icons.Outlined.AutoStories,
                badgeColor = Color(0xFF0D9488),
                containerBg = Color(0xFFF0FDFA),
                testTag = "more_menu_general_books",
                onClick = {
                    onDismiss()
                    viewModel.openGeneralBooks()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            MoreMenuItem(
                title = "Bookmarks",
                subtitle = "Saved books & bookmarked study pages",
                icon = Icons.Outlined.BookmarkBorder,
                badgeColor = ProvinceKpkBlue,
                containerBg = ProvinceKpkBlueBg,
                testTag = "more_menu_bookmarks",
                onClick = {
                    onDismiss()
                    viewModel.navigateTo(Screen.BOOKMARKS)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            MoreMenuItem(
                title = "Student Notes",
                subtitle = "Write, edit, and organize chapter notes",
                icon = Icons.Outlined.EditNote,
                badgeColor = Emerald600,
                containerBg = Emerald50,
                testTag = "more_menu_notes",
                onClick = {
                    onDismiss()
                    viewModel.navigateTo(Screen.NOTES)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            MoreMenuItem(
                title = "Board News & Updates",
                subtitle = "Date sheets, syllabus updates, new guides",
                icon = Icons.Outlined.Campaign,
                badgeColor = ProvinceBalochistanOrange,
                containerBg = ProvinceBalochistanOrangeBg,
                testTag = "more_menu_news",
                badgeNumber = unreadNewsCount,
                onClick = {
                    onDismiss()
                    viewModel.navigateTo(Screen.NEWS)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            MoreMenuItem(
                title = "Settings & Storage",
                subtitle = "Offline downloaded files manager, cloud sync & cache",
                icon = Icons.Outlined.Settings,
                badgeColor = ProvinceSindhPurple,
                containerBg = ProvinceSindhPurpleBg,
                testTag = "more_menu_settings",
                onClick = {
                    onDismiss()
                    viewModel.navigateTo(Screen.SETTINGS)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    containerBg: Color,
    testTag: String,
    badgeNumber: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate900
                        )
                    )
                    if (badgeNumber > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (badgeNumber > 9) "9+ NEW" else "$badgeNumber NEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate600,
                        fontSize = 12.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

