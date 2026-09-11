package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adsterra.AdsterraBannerAd
import com.example.data.model.Province
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

data class GradeSection(
    val title: String,
    val subtitle: String,
    val classes: List<Int>
)

@Composable
fun ClassSelectScreen(viewModel: StudyViewModel) {
    val selectedProvince by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()

    val sections = listOf(
        GradeSection("Higher Secondary / Inter", "F.Sc, ICS, FA, I.Com (Part 1 & 2)", listOf(12, 11)),
        GradeSection("Secondary / Matriculation", "SSC Part 1 & Part 2 (Science & Arts)", listOf(10, 9)),
        GradeSection("Middle School", "Middle Stage General Curriculum", listOf(8, 7, 6)),
        GradeSection("Primary School", "Foundation & Elementary Learning", listOf(5, 4, 3, 2, 1))
    )

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "${selectedProvince.title} - Select Class",
                subtitle = selectedProvince.boardName,
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    AdsterraBannerAd()
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Geometric Province Highlight Card
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = selectedProvince.primaryColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = selectedProvince.primaryColor.copy(alpha = 0.4f)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monogram Box
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedProvince.monogram,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 20.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedProvince.title} Board Curriculum",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedProvince.boardName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = Color.White,
                            shape = CircleShape
                        ) {
                            Text(
                                text = selectedProvince.urduName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = selectedProvince.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Quick instruction
            item {
                Text(
                    text = "Choose your Grade / Class level:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
            }

            // Grade Sections
            sections.forEach { section ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(selectedProvince.primaryColor)
                            )
                            Column {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Slate800
                                    )
                                )
                                Text(
                                    text = section.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate500,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Grid of large friendly class buttons
                        ClassCardsGrid(
                            classes = section.classes,
                            province = selectedProvince,
                            allBooks = allBooks,
                            onClassSelected = { classLevel ->
                                viewModel.selectClass(classLevel)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassCardsGrid(
    classes: List<Int>,
    province: Province,
    allBooks: List<com.example.data.local.BookEntity>,
    onClassSelected: (Int) -> Unit
) {
    val chunked = classes.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chunked.forEach { rowClasses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowClasses.forEach { classLevel ->
                    val booksForClass = allBooks.count {
                        it.provinceCode.equals(province.code, ignoreCase = true) && it.classLevel == classLevel
                    }

                    Surface(
                        onClick = { onClassSelected(classLevel) },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            province.borderColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .testTag("class_card_$classLevel")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(province.containerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$classLevel",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = province.primaryColor,
                                            fontSize = 20.sp
                                        )
                                    )
                                    Text(
                                        text = "Grade",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = province.primaryColor.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Class $classLevel",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Slate900
                                    )
                                )
                                Text(
                                    text = if (booksForClass > 0) "$booksForClass Guides" else "Available",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate500,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Class $classLevel",
                                tint = province.primaryColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // If odd number in row, fill remaining space
                if (rowClasses.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
