package com.oyayasu.evoluir

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF387F57)
private val LightBackground = Color(0xFFFAFAF7)
private val DateLabel = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ImprovementStore(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Green,
                    onPrimary = Color.White,
                    background = LightBackground,
                    surface = Color.White,
                    onSurface = Color(0xFF242A26)
                )
            ) {
                ImprovementApp(store)
            }
        }
    }
}

@Composable
private fun ImprovementApp(store: ImprovementStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var summaries by remember { mutableStateOf<List<ImprovementSummary>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Improvement?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        loading = true
        summaries = withContext(Dispatchers.IO) { store.list() }
        loading = false
    }

    fun commit(operation: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                operation()
                ProgressWidgetProvider.refreshAll(context)
            }
            refresh++
        }
    }

    val selected = summaries.firstOrNull { it.improvement.id == selectedId }
    Scaffold(
        containerColor = LightBackground,
        floatingActionButton = {
            if (selectedId == null) {
                FloatingActionButton(onClick = { editing = null; showEditor = true }, containerColor = Green) {
                    Text("+", fontSize = 28.sp, color = Color.White)
                }
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp)) {
            if (selectedId == null) {
                Spacer(Modifier.height(28.dp))
                Text("Evoluir", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Observe suas atitudes. Reconheça suas mudanças.", color = Color.Gray, fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                if (loading) {
                    CircularProgressIndicator()
                } else if (summaries.isEmpty()) {
                    EmptyState(onAdd = { showEditor = true })
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(summaries, key = { it.improvement.id }) { summary ->
                            ImprovementCard(
                                summary = summary,
                                onOpen = { selectedId = summary.improvement.id },
                                onMark = { outcome -> commit { store.mark(summary.improvement.id, LocalDate.now().toString(), outcome) } }
                            )
                        }
                    }
                }
            } else if (selected != null) {
                DetailScreen(
                    summary = selected,
                    onBack = { selectedId = null },
                    onEdit = { editing = selected.improvement; showEditor = true },
                    onDelete = { confirmDelete = true },
                    onMark = { day, outcome, note -> commit { store.mark(selected.improvement.id, day, outcome, note) } },
                    onClear = { day -> commit { store.clear(selected.improvement.id, day) } }
                )
            } else {
                LaunchedEffect(selectedId, loading) { if (!loading) selectedId = null }
            }
        }
    }

    if (showEditor) {
        ImprovementEditor(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { title, description, symbol ->
                val id = editing?.id
                showEditor = false
                commit { store.save(id, title, description, symbol) }
            }
        )
    }

    if (confirmDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir este ponto?") },
            text = { Text("Os registros e as notas deste ponto também serão excluídos. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = selected.improvement.id
                    confirmDelete = false
                    selectedId = null
                    commit { store.delete(id) }
                }) { Text("Excluir", color = Color(0xFFC14944)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Comece com um ponto de melhoria", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Descreva o comportamento que deseja mudar.\nNenhum dia começa como uma falha.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd) { Text("Adicionar ponto") }
    }
}

@Composable
private fun ImprovementCard(
    summary: ImprovementSummary,
    onOpen: () -> Unit,
    onMark: (Int) -> Unit
) {
    val today = LocalDate.now()
    val score = summary.score()
    val color = Color(ProgressPalette.forScore(score))
    val todayMark = summary.markOn(today.toString())
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.improvement.symbol, color = color, fontSize = 26.sp)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(summary.improvement.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 2)
                    if (summary.improvement.description.isNotEmpty()) {
                        Text(summary.improvement.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(if (score == null) "—" else "${(score * 100).toInt()}%", color = color, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                (13L downTo 0L).forEach { delta ->
                    val day = today.minusDays(delta)
                    Box(
                        Modifier.weight(1f).height(9.dp).background(
                            Color(ProgressPalette.forDay(summary.markOn(day.toString()))),
                            RoundedCornerShape(3.dp)
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Últimos 14 dias · cinza = não registrado", fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMark.bind(0), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (todayMark?.outcome == 0) Color(0xFFBC4644) else Color.Gray)) {
                    Text(if (todayMark?.outcome == 0) "✕ Falhei ✓" else "✕ Falhei", fontSize = 12.sp)
                }
                Button(onClick = onMark.bind(1), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (todayMark?.outcome == 1) Green else Color(0xFF578C6C))) {
                    Text(if (todayMark?.outcome == 1) "✓ Superei ✓" else "✓ Superei", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun <T> ((T) -> Unit).bind(value: T): () -> Unit = { this(value) }

@Composable
private fun DetailScreen(
    summary: ImprovementSummary,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMark: (String, Int, String) -> Unit,
    onClear: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedDay by remember(summary.improvement.id) { mutableStateOf(LocalDate.now().toString()) }
    val currentMark = summary.markOn(selectedDay)
    var note by remember { mutableStateOf("") }
    LaunchedEffect(selectedDay, currentMark) { note = currentMark?.note.orEmpty() }
    val today = LocalDate.now()
    val score = summary.score()
    val color = Color(ProgressPalette.forScore(score))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Voltar") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("Editar") }
        }
        Spacer(Modifier.height(10.dp))
        Text(summary.improvement.name, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        if (summary.improvement.description.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(summary.improvement.description, color = Color.DarkGray)
        }
        Spacer(Modifier.height(19.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(summary.improvement.symbol, color = color, fontSize = 30.sp)
            Spacer(Modifier.width(9.dp))
            Text(if (score == null) "Sem registros nos últimos 30 dias" else "${(score * 100).toInt()}% de superações nos últimos 30 dias", color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(21.dp))
        Text("Histórico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Toque num dia para registrar ou revisar.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(11.dp))
        val days = (34L downTo 0L).map { today.minusDays(it) }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { date ->
                    val key = date.toString()
                    val active = selectedDay == key
                    Column(Modifier.weight(1f).clickable { selectedDay = key }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(33.dp).background(Color(ProgressPalette.forDay(summary.markOn(key))), CircleShape), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = 12.sp, color = if (summary.markOn(key) == null) Color.DarkGray else Color.White, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (active) Box(Modifier.padding(top = 3.dp).size(5.dp).background(Green, CircleShape))
                        else Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = {
            val day = LocalDate.parse(selectedDay)
            DatePickerDialog(context, { _, y, m, d ->
                selectedDay = LocalDate.of(y, m + 1, d).toString()
            }, day.year, day.monthValue - 1, day.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }) { Text("Escolher outra data") }
        Spacer(Modifier.height(18.dp))
        Text("Dia ${LocalDate.parse(selectedDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("O que aconteceu? (opcional)") },
            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = { onMark(selectedDay, 0, note) }, modifier = Modifier.weight(1f)) { Text("✕ Falhei") }
            Button(onClick = { onMark(selectedDay, 1, note) }, modifier = Modifier.weight(1f)) { Text("✓ Superei") }
        }
        if (currentMark != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onMark(selectedDay, currentMark.outcome, note) }) { Text("Salvar nota") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onClear(selectedDay) }) { Text("Limpar registro", color = Color(0xFFC14944)) }
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider()
        TextButton(onClick = onDelete) { Text("Excluir este ponto de melhoria", color = Color(0xFFC14944)) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ImprovementEditor(initial: Improvement?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var symbol by remember(initial?.id) { mutableStateOf(initial?.symbol ?: "●") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo ponto de melhoria" else "Editar ponto de melhoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Ícone", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("●", "◆", "✦", "▲").forEach { option ->
                        FilterChip(selected = symbol == option, onClick = { symbol = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 80) name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { if (it.length <= 2000) description = it },
                    label = { Text("Como desejo agir diferente?") }, minLines = 3, maxLines = 6)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, description, symbol) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
