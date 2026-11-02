package com.oyayasu.evoluir

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF33875A)
private val LightBackground = Color(0xFFF8F9F7)
private val ThinBorder = Color(0xFFE7EAE6)
private val Muted = Color(0xFF808782)
private val ScaleColors = listOf(Color(0xFFE34E4E), Color(0xFFEC793D), Color(0xFFF1C842), Color(0xFF95C357), Color(0xFF419D69))
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
                Text("Evoluir", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
                Spacer(Modifier.height(24.dp))
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
        Text("Nenhum ponto cadastrado", fontWeight = FontWeight.Medium, color = Muted)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(13.dp)) { Text("Adicionar") }
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
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ThinBorder), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxWidth().padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(IconCatalog.res(summary.improvement.symbol)), contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(summary.improvement.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 2)
                    if (summary.improvement.description.isNotEmpty()) {
                        Text(summary.improvement.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(if (score == null) "—" else "${(score * 100).toInt()}%", color = color, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(21.dp))
            ProgressScale()
            Spacer(Modifier.height(16.dp))
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
            Icon(painter = painterResource(IconCatalog.res(summary.improvement.symbol)), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(9.dp))
            Text(if (score == null) "Sem registros nos últimos 30 dias" else "${(score * 100).toInt()}% de superações nos últimos 30 dias", color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, ThinBorder)) { ProgressScale(Modifier.padding(18.dp)) }
        Spacer(Modifier.height(26.dp))
        Text("Histórico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    var symbol by remember(initial?.id) { mutableStateOf(initial?.symbol?.takeIf { id -> IconCatalog.all.any { it.id == id } } ?: "sprout") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo ponto" else "Editar ponto") },
        text = {
            Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ícone", fontSize = 13.sp, color = Muted)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(272.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(IconCatalog.all) { choice ->
                        val selected = symbol == choice.id
                        Column(
                            Modifier.background(if (selected) Color(0xFFEAF4ED) else Color(0xFFF8F9F8), RoundedCornerShape(12.dp))
                                .border(1.dp, if (selected) Color(0xFFB6D8BF) else ThinBorder, RoundedCornerShape(12.dp))
                                .clickable { symbol = choice.id }.padding(vertical = 12.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(painter = painterResource(choice.resource), contentDescription = choice.label,
                                modifier = Modifier.size(24.dp), tint = if (selected) Green else Muted)
                            Spacer(Modifier.height(6.dp))
                            Text(choice.label, fontSize = 10.sp, color = if (selected) Green else Muted,
                                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 80) name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { if (it.length <= 2000) description = it },
                    label = { Text("Descrição") }, minLines = 3, maxLines = 6)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, description, symbol) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Fixed five-stop visual scale. The current personal score is displayed separately. */
@Composable
private fun ProgressScale(modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 25, 50, 75, 100).forEachIndexed { index, percentage ->
                Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(14.dp).border(1.4.dp, ScaleColors[index], CircleShape)
                            .padding(3.dp).background(ScaleColors[index], CircleShape)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(percentage.toString() + "%", fontSize = 11.sp, color = Color(0xFF424843))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(7.dp)
            .background(Brush.horizontalGradient(ScaleColors), RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mais falhas", color = Muted, fontSize = 11.sp)
            Text("Mais superações", color = Muted, fontSize = 11.sp)
        }
    }
}
