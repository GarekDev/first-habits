package app.firstyou.habits.ui.today

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.data.HabitKind
import app.firstyou.habits.data.WeekDays
import java.time.LocalDate
import kotlin.math.abs
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HISTORY_DAYS = 3660
private val NameWidth = 148.dp
private val DayWidth = 52.dp
private val HeaderHeight = 52.dp
private val RowHeight = 52.dp

private data class AmountEdit(val habit: BoardHabit, val date: LocalDate)

private data class StreamerBurst(val id: Int, val origin: Offset, val color: Color)

@Composable
fun TodayScreen(
    onNewHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenReminderHelp: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as HabitsApp
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(app.container.repository))
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<AmountEdit?>(null) }
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HabitsBar(
            sort = state.sort,
            onSort = viewModel::setSort,
            onNewHabit = onNewHabit,
            onOpenArchived = onOpenArchived,
            onOpenReminderHelp = onOpenReminderHelp,
            onOpenBackup = onOpenBackup,
            onOpenSettings = onOpenSettings,
        )
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.habits.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Todavía no hay hábitos. Pulsa + para crear uno.")
            }
            else -> HabitBoard(
                habits = state.habits,
                onOpenHabit = onOpenHabit,
                onSaveOrder = viewModel::saveOrder,
                onCell = { habit, date ->
                    if (!WeekDays.isScheduled(habit.days, date)) return@HabitBoard
                    if (habit.kind == HabitKind.MEASURABLE) {
                        editing = AmountEdit(habit, date)
                        draft = habit.values[date]?.let(::formatNumber).orEmpty()
                    } else {
                        viewModel.toggleYesNo(habit, date)
                    }
                },
            )
        }
    }

    val pending = editing
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(pending.habit.name) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(pending.habit.unit ?: "Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setAmount(pending.habit.id, pending.date, draft.replace(',', '.').toDoubleOrNull())
                    editing = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setAmount(pending.habit.id, pending.date, null)
                    editing = null
                }) { Text("Borrar") }
            },
        )
    }
}

@Composable
private fun HabitsBar(
    sort: BoardSort,
    onSort: (BoardSort) -> Unit,
    onNewHabit: () -> Unit,
    onOpenArchived: () -> Unit,
    onOpenReminderHelp: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "First Habits",
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onNewHabit) {
            Icon(Icons.Filled.Add, contentDescription = "Nuevo hábito")
        }
        Box {
            IconButton(onClick = { sortOpen = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Orden")
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (sort == BoardSort.MANUAL) "Manual ✓" else "Manual") },
                    onClick = { onSort(BoardSort.MANUAL); sortOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(if (sort == BoardSort.NAME) "Nombre ✓" else "Nombre") },
                    onClick = { onSort(BoardSort.NAME); sortOpen = false },
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Más")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Ajustes") }, onClick = { menuOpen = false; onOpenSettings() })
                DropdownMenuItem(text = { Text("Archivados") }, onClick = { menuOpen = false; onOpenArchived() })
                DropdownMenuItem(text = { Text("Copia de seguridad") }, onClick = { menuOpen = false; onOpenBackup() })
                DropdownMenuItem(text = { Text("Recordatorio") }, onClick = { menuOpen = false; onOpenReminderHelp() })
            }
        }
    }
}

@Composable
private fun HabitBoard(
    habits: List<BoardHabit>,
    onOpenHabit: (Long) -> Unit,
    onSaveOrder: (List<Long>) -> Unit,
    onCell: (BoardHabit, LocalDate) -> Unit,
) {
    val density = LocalDensity.current
    val dayPx = with(density) { DayWidth.toPx() }
    val rowPx = with(density) { RowHeight.toPx() }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var orderIds by remember { mutableStateOf<List<Long>?>(null) }
    val shown = remember(habits, orderIds) {
        val ids = orderIds
        if (ids == null) {
            habits
        } else {
            val byId = habits.associateBy { it.id }
            val ordered = ids.mapNotNull { byId[it] }
            if (ordered.size == habits.size) ordered else habits
        }
    }
    val currentIds = rememberUpdatedState(shown.map { it.id })
    LaunchedEffect(habits.map { it.id }, dragId) {
        if (dragId == null && orderIds != null && habits.map { it.id } == orderIds) {
            orderIds = null
        }
    }
    var scrollPx by remember { mutableFloatStateOf(0f) }
    var board by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var burst by remember { mutableStateOf<StreamerBurst?>(null) }
    var burstCount by remember { mutableIntStateOf(0) }
    val ribbon = remember { Animatable(0f) }
    val reduce = reducedMotion()
    LaunchedEffect(burst?.id) {
        val current = burst ?: return@LaunchedEffect
        if (current.id == 0 || reduce) return@LaunchedEffect
        ribbon.snapTo(0f)
        ribbon.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = 760,
                easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f),
            ),
        )
    }
    val travel = ribbon.value
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { board = it }
            .drawWithContent {
                drawContent()
                val shot = burst
                if (shot != null && !reduce && travel > 0f && travel < 1f) {
                    drawStreamers(shot.origin, shot.color, travel)
                }
            },
    ) {
        val daysWidth = maxWidth - NameWidth
        val viewportPx = with(density) { daysWidth.toPx() }.coerceAtLeast(dayPx)
        val maxScroll = (HISTORY_DAYS * dayPx - viewportPx).coerceAtLeast(0f)
        val scroller = rememberScrollableState { delta ->
            val next = (scrollPx - delta).coerceIn(0f, maxScroll)
            val consumed = scrollPx - next
            scrollPx = next
            consumed
        }
        val vertical = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(scroller, Orientation.Horizontal)
                .verticalScroll(vertical, enabled = dragId == null),
        ) {
            Row(Modifier.fillMaxWidth().height(HeaderHeight)) {
                Spacer(Modifier.width(NameWidth))
                DayWindow(scrollPx, dayPx, daysWidth, viewportPx) { date ->
                    DayHeading(date)
                }
            }
            shown.forEach { habit ->
                key(habit.id) {
                val lifted = habit.id == dragId
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (lifted) dragOffset else 0f
                            shadowElevation = if (lifted) 6.dp.toPx() else 0f
                        }
                        .background(if (lifted) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .pointerInput(habit.id) {
                            val namePx = NameWidth.toPx()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var tapped = false
                                var movedAway = false
                                try {
                                    withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                tapped = true
                                                break
                                            }
                                            val moved = change.position - down.position
                                            if (abs(moved.x) > viewConfiguration.touchSlop || abs(moved.y) > viewConfiguration.touchSlop) {
                                                movedAway = true
                                                break
                                            }
                                        }
                                    }
                                } catch (_: PointerEventTimeoutCancellationException) {
                                }
                                if (tapped && down.position.x <= namePx) onOpenHabit(habit.id)
                                if (tapped || movedAway) return@awaitEachGesture
                                orderIds = currentIds.value
                                dragId = habit.id
                                dragOffset = 0f
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val delta = change.positionChange().y
                                    change.consume()
                                    dragOffset += delta
                                    val ids = orderIds?.toMutableList() ?: continue
                                    val index = ids.indexOf(habit.id)
                                    if (index < 0) continue
                                    if (dragOffset > rowPx / 2f && index < ids.lastIndex) {
                                        ids.swap(index, index + 1)
                                        orderIds = ids
                                        dragOffset -= rowPx
                                    } else if (dragOffset < -rowPx / 2f && index > 0) {
                                        ids.swap(index, index - 1)
                                        orderIds = ids
                                        dragOffset += rowPx
                                    }
                                }
                                onSaveOrder(orderIds ?: currentIds.value)
                                dragId = null
                                dragOffset = 0f
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitName(habit, Modifier.width(NameWidth))
                    DayWindow(scrollPx, dayPx, daysWidth, viewportPx) { date ->
                        val open = if (WeekDays.isScheduled(habit.days, date)) {
                            Modifier.clickable { onCell(habit, date) }
                        } else {
                            Modifier
                        }
                        DayCell(habit, date, open) { cell, color ->
                            val host = board ?: return@DayCell
                            val topLeft = host.localPositionOf(cell, Offset.Zero)
                            burstCount += 1
                            burst = StreamerBurst(
                                id = burstCount,
                                origin = topLeft + Offset(cell.size.width / 2f, cell.size.height / 2f),
                                color = color,
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

private fun <T> MutableList<T>.swap(from: Int, to: Int) {
    val item = this[from]
    this[from] = this[to]
    this[to] = item
}

@Composable
private fun DayWindow(
    scrollPx: Float,
    dayPx: Float,
    width: Dp,
    viewportPx: Float,
    content: @Composable (LocalDate) -> Unit,
) {
    val first = (scrollPx / dayPx).toInt().coerceIn(0, HISTORY_DAYS - 1)
    val shown = (viewportPx / dayPx).toInt() + 2
    val today = LocalDate.now()
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clipToBounds(),
    ) {
        val last = (first + shown).coerceAtMost(HISTORY_DAYS)
        for (index in first until last) {
            val x = index * dayPx - scrollPx
            Box(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), 0) }
                    .width(DayWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                val date = today.minusDays(index.toLong())
                key(date) { content(date) }
            }
        }
    }
}

@Composable
private fun DayHeading(date: LocalDate) {
    val locale = Locale.forLanguageTag("es")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (date.dayOfMonth == 1 || date == LocalDate.now()) {
                date.format(DateTimeFormatter.ofPattern("MMM", locale)).uppercase(locale)
            } else {
                " "
            },
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale).take(3),
            fontSize = 11.sp,
        )
        Text(text = date.dayOfMonth.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HabitName(habit: BoardHabit, modifier: Modifier) {
    Row(modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(habit.color), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = habit.name,
            color = readableHabitColor(habit.color),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DayCell(
    habit: BoardHabit,
    date: LocalDate,
    modifier: Modifier,
    onCelebrate: (LayoutCoordinates, Color) -> Unit,
) {
    val scheduled = WeekDays.isScheduled(habit.days, date)
    val value = habit.values[date]
    val done = habit.kind == HabitKind.YES_NO && (value ?: 0.0) > 0.0
    val missed = scheduled && habit.kind == HabitKind.YES_NO && !done
    var armed by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    LaunchedEffect(done) {
        if (!armed) {
            armed = true
            return@LaunchedEffect
        }
        if (done) {
            coords?.let { onCelebrate(it, Color(habit.color)) }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { coords = it },
        contentAlignment = Alignment.Center,
    ) {
        when {
            done -> Icon(
                Icons.Filled.Check,
                contentDescription = "Hecho",
                tint = readableHabitColor(habit.color),
                modifier = Modifier.size(20.dp),
            )
            missed -> Icon(
                Icons.Filled.Close,
                contentDescription = "No hecho",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            scheduled && habit.kind == HabitKind.MEASURABLE -> {
                val amount = value
                if (amount == null) {
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        fontSize = 16.sp,
                    )
                } else {
                    val met = habit.met(amount)
                    Text(
                        text = formatNumber(amount),
                        color = if (met) {
                            readableHabitColor(habit.color)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = if (formatNumber(amount).length >= 4) 13.sp else 16.sp,
                        fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStreamers(
    origin: Offset,
    accent: Color,
    travel: Float,
) {
    val fade = if (travel < 0.62f) 1f else ((1f - travel) / 0.38f).coerceIn(0f, 1f)
    ribbons.forEachIndexed { index, ribbon ->
        val path = Path()
        val length = ribbon.length.toPx() * (0.22f + 0.78f * travel)
        val curl = ribbon.curl.toPx()
        val drop = ribbon.drop.toPx() * travel
        for (step in 0..18) {
            val u = step / 18f
            val along = length * u
            val wave = sin(u * PI * ribbon.turns + ribbon.phase).toFloat() * curl * u
            val x = origin.x + cos(ribbon.angle).toFloat() * along - sin(ribbon.angle).toFloat() * wave
            val y = origin.y + sin(ribbon.angle).toFloat() * along * 0.72f + drop * u * u + wave * 0.35f
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = (if (index == 2) accent else ribbon.color).copy(alpha = fade),
            style = Stroke(
                width = ribbon.thickness.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private class Ribbon(
    val angle: Double,
    val length: Dp,
    val curl: Dp,
    val drop: Dp,
    val thickness: Dp,
    val turns: Double,
    val phase: Double,
    val color: Color,
)

private val ribbons = listOf(
    Ribbon(-2.45, 74.dp, 11.dp, 22.dp, 4.4.dp, 2.1, 0.3, Color(0xFFFF4F7B)),
    Ribbon(-1.75, 62.dp, 8.dp, 28.dp, 3.4.dp, 2.7, 1.2, Color(0xFFFFC857)),
    Ribbon(-1.05, 82.dp, 13.dp, 18.dp, 4.8.dp, 1.7, 0.5, Color(0xFF6DFFB0)),
    Ribbon(-0.4, 56.dp, 7.dp, 30.dp, 3.1.dp, 3.1, 2.1, Color(0xFF62C4FF)),
    Ribbon(0.4, 70.dp, 12.dp, 24.dp, 4.2.dp, 2.0, 0.9, Color(0xFFE15CFF)),
    Ribbon(1.2, 78.dp, 9.dp, 26.dp, 3.6.dp, 2.5, 1.7, Color(0xFFFF8A3D)),
    Ribbon(2.15, 64.dp, 10.dp, 20.dp, 3.3.dp, 2.8, 0.2, Color(0xFFFFF176)),
)

@Composable
private fun reducedMotion(): Boolean {
    val scale = Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}

@Composable
private fun readableHabitColor(argb: Int): Color {
    val color = Color(argb)
    val lightBackground = MaterialTheme.colorScheme.background.red > 0.8f
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (lightBackground && luminance > 0.85f) Color(0xFF3A3A3A) else color
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}
