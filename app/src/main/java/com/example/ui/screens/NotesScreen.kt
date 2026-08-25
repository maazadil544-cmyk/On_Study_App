package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NoteEntity
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: StudyViewModel) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    var showEditorDialogForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "Study Notes",
                subtitle = "${notes.size} Notes Created",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        },
        bottomBar = {
            GeometricBottomNavigation(viewModel = viewModel, currentScreen = Screen.NOTES)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isCreatingNewNote = true
                    showEditorDialogForNote = NoteEntity(title = "", content = "")
                },
                containerColor = Emerald600,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Emerald600)
                    .testTag("add_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Note")
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Notes Created",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Write your own formulas, chapter summaries, and revision cheat-sheets.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isCreatingNewNote = true
                            showEditorDialogForNote = NoteEntity(title = "", content = "")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create First Note", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val colorPalette = listOf(
                        Pair(Color(0xFFCCFBF1), Emerald600),
                        Pair(Color(0xFFE0F2FE), Color(0xFF0369A1)),
                        Pair(Color(0xFFFEF3C7), Color(0xFFB45309)),
                        Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE)),
                        Pair(Color(0xFFFFE4E6), Color(0xFFBE123C))
                    )
                    val (bgContainer, accentColor) = colorPalette.getOrElse(note.colorIndex % colorPalette.size) {
                        colorPalette.first()
                    }

                    Surface(
                        onClick = {
                            isCreatingNewNote = false
                            showEditorDialogForNote = note
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Slate200)
                            .testTag("note_card_${note.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = bgContainer
                                ) {
                                    Text(
                                        text = note.subject ?: "General Note",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            isCreatingNewNote = false
                                            showEditorDialogForNote = note
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Slate100)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit note",
                                            tint = Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Slate100)
                                            .testTag("delete_note_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete note",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Slate900,
                                    letterSpacing = (-0.3).sp
                                )
                            )

                            if (!note.bookTitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Linked to: ${note.bookTitle}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Emerald600,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Slate600,
                                    lineHeight = 22.sp,
                                    fontSize = 13.sp
                                ),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
                            Text(
                                text = "Updated $dateStr",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Note Editor Dialog
    showEditorDialogForNote?.let { note ->
        NoteEditorDialog(
            initialNote = note,
            isNew = isCreatingNewNote,
            onDismiss = { showEditorDialogForNote = null },
            onSave = { updatedNote ->
                viewModel.saveNote(updatedNote)
                showEditorDialogForNote = null
            }
        )
    }
}

@Composable
fun NoteEditorDialog(
    initialNote: NoteEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.content) }
    var subject by remember { mutableStateOf(initialNote.subject ?: "") }
    var colorIndex by remember { mutableStateOf(initialNote.colorIndex) }

    val subjectsList = listOf("General", "Physics", "Chemistry", "Mathematics", "Biology", "Computer Science", "Urdu", "English", "Pak Studies")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Create Study Note" else "Edit Note",
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write formulas, concepts, or questions...") },
                    minLines = 5,
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Subject picker
                Text(
                    text = "Subject Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                )
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjectsList) { s ->
                        val isSelected = (subject.ifBlank { "General" } == s)
                        FilterChip(
                            selected = isSelected,
                            onClick = { subject = if (s == "General") "" else s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald600,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Emerald600 else Slate200
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(
                            initialNote.copy(
                                title = title.ifBlank { "Untitled Note" },
                                content = content,
                                subject = subject.ifBlank { null },
                                colorIndex = colorIndex,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Note", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}
