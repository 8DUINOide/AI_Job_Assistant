package com.aijobassistant.app.ui.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijobassistant.app.model.*
import com.aijobassistant.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorScreen(
    viewModel: ResumeEditorViewModel,
    isTailored: Boolean = false,
    onNavigateBack: () -> Unit,
    onGeneratePdf: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        if (!isTailored) {
            viewModel.loadProfile(null)
        }
    }
    
    // Update saveSuccess flag
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetSaveSuccess()
            if (!isTailored) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isTailored) "Review Tailored Resume" else "Edit Master Resume") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = DarkBackground,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isTailored) {
                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Save Master Profile")
                        }
                    } else {
                        Button(
                            onClick = { onGeneratePdf?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                        ) {
                            Text("Generate Final Resume")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            return@Scaffold
        }
        
        ResumeEditorForm(
            profile = state.profile,
            onProfileChange = { viewModel.updateProfile(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ResumeEditorForm(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Personal Info
        item {
            SectionCard(title = "Contact Information") {
                val p = profile.personalInfo
                OutlinedTextField(
                    value = p.firstName,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(firstName = it))) },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = p.lastName,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(lastName = it))) },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = p.email,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(email = it))) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = p.phone,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(phone = it))) },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = p.location,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(location = it))) },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = p.portfolioUrl,
                    onValueChange = { onProfileChange(profile.copy(personalInfo = p.copy(portfolioUrl = it))) },
                    label = { Text("Portfolio/GitHub/LinkedIn") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Summary
        item {
            SectionCard(title = "Professional Summary") {
                OutlinedTextField(
                    value = profile.summary,
                    onValueChange = { onProfileChange(profile.copy(summary = it)) },
                    label = { Text("Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
        
        // Experience
        item {
            SectionCard(title = "Work Experience", onAdd = {
                val newList = profile.experience + Experience()
                onProfileChange(profile.copy(experience = newList))
            }) {
                profile.experience.forEachIndexed { index, exp ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Experience ${index + 1}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    val newList = profile.experience.toMutableList().apply { removeAt(index) }
                                    onProfileChange(profile.copy(experience = newList))
                                }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Delete", tint = StatusDanger) }
                            }
                            OutlinedTextField(value = exp.title, onValueChange = { v -> val nl = profile.experience.toMutableList(); nl[index] = exp.copy(title = v); onProfileChange(profile.copy(experience = nl)) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = exp.company, onValueChange = { v -> val nl = profile.experience.toMutableList(); nl[index] = exp.copy(company = v); onProfileChange(profile.copy(experience = nl)) }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = exp.startDate, onValueChange = { v -> val nl = profile.experience.toMutableList(); nl[index] = exp.copy(startDate = v); onProfileChange(profile.copy(experience = nl)) }, label = { Text("Start Date") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = exp.endDate, onValueChange = { v -> val nl = profile.experience.toMutableList(); nl[index] = exp.copy(endDate = v); onProfileChange(profile.copy(experience = nl)) }, label = { Text("End Date") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = exp.description, onValueChange = { v -> val nl = profile.experience.toMutableList(); nl[index] = exp.copy(description = v); onProfileChange(profile.copy(experience = nl)) }, label = { Text("Bullets") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3)
                        }
                    }
                }
            }
        }
        
        // Projects
        item {
            SectionCard(title = "Projects", onAdd = {
                val newList = profile.projects + Project()
                onProfileChange(profile.copy(projects = newList))
            }) {
                profile.projects.forEachIndexed { index, proj ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Project ${index + 1}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    val newList = profile.projects.toMutableList().apply { removeAt(index) }
                                    onProfileChange(profile.copy(projects = newList))
                                }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Delete", tint = StatusDanger) }
                            }
                            OutlinedTextField(value = proj.title, onValueChange = { v -> val nl = profile.projects.toMutableList(); nl[index] = proj.copy(title = v); onProfileChange(profile.copy(projects = nl)) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = proj.role, onValueChange = { v -> val nl = profile.projects.toMutableList(); nl[index] = proj.copy(role = v); onProfileChange(profile.copy(projects = nl)) }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = proj.dates, onValueChange = { v -> val nl = profile.projects.toMutableList(); nl[index] = proj.copy(dates = v); onProfileChange(profile.copy(projects = nl)) }, label = { Text("Dates") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = proj.link, onValueChange = { v -> val nl = profile.projects.toMutableList(); nl[index] = proj.copy(link = v); onProfileChange(profile.copy(projects = nl)) }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = proj.description, onValueChange = { v -> val nl = profile.projects.toMutableList(); nl[index] = proj.copy(description = v); onProfileChange(profile.copy(projects = nl)) }, label = { Text("Bullets") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        }
                    }
                }
            }
        }
        
        // Education
        item {
            SectionCard(title = "Education", onAdd = {
                val newList = profile.education + Education()
                onProfileChange(profile.copy(education = newList))
            }) {
                profile.education.forEachIndexed { index, edu ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Education ${index + 1}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    val newList = profile.education.toMutableList().apply { removeAt(index) }
                                    onProfileChange(profile.copy(education = newList))
                                }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, "Delete", tint = StatusDanger) }
                            }
                            OutlinedTextField(value = edu.degree, onValueChange = { v -> val nl = profile.education.toMutableList(); nl[index] = edu.copy(degree = v); onProfileChange(profile.copy(education = nl)) }, label = { Text("Degree") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = edu.university, onValueChange = { v -> val nl = profile.education.toMutableList(); nl[index] = edu.copy(university = v); onProfileChange(profile.copy(education = nl)) }, label = { Text("University") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = edu.graduationYear, onValueChange = { v -> val nl = profile.education.toMutableList(); nl[index] = edu.copy(graduationYear = v); onProfileChange(profile.copy(education = nl)) }, label = { Text("Year / Location") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        
        // Simple lists: Skills, Certifications, Awards
        item {
            SimpleListSection("Skills", profile.skills) { nl -> onProfileChange(profile.copy(skills = nl)) }
        }
        item {
            SimpleListSection("Certifications", profile.certifications) { nl -> onProfileChange(profile.copy(certifications = nl)) }
        }
        item {
            SimpleListSection("Awards & Achievements", profile.awards) { nl -> onProfileChange(profile.copy(awards = nl)) }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SectionCard(title: String, onAdd: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                if (onAdd != null) {
                    IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = PrimaryBlue)
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun SimpleListSection(title: String, list: List<String>, onUpdate: (List<String>) -> Unit) {
    SectionCard(title = title, onAdd = { onUpdate(list + "") }) {
        list.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item,
                    onValueChange = { v -> val nl = list.toMutableList(); nl[index] = v; onUpdate(nl) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { val nl = list.toMutableList(); nl.removeAt(index); onUpdate(nl) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger)
                }
            }
        }
    }
}
