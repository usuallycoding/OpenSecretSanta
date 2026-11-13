package com.usuallycoding.opensecretsanta.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.usuallycoding.opensecretsanta.ui.form.BadgeGap
import com.usuallycoding.opensecretsanta.ui.form.DragHandleIcon
import com.usuallycoding.opensecretsanta.ui.form.NumberBadge
import com.usuallycoding.opensecretsanta.ui.form.StatusPill
import com.usuallycoding.opensecretsanta.ui.form.SnowCard
import com.usuallycoding.opensecretsanta.ui.load.LocalPageScroll
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistLimits
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadSection
import com.usuallycoding.opensecretsanta.ui.form.LinkText
import com.usuallycoding.opensecretsanta.ui.form.ReadOnlyText
import com.usuallycoding.opensecretsanta.ui.form.TimesLine
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.filled.Add
import com.usuallycoding.opensecretsanta.ui.form.AppFab

/** The person's own wishlist for one group, as a section of the Receiver page: their items in their order, and while the group allows it, ways to change them. */
@Composable
fun WishlistSection(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val ui by viewModel.ui.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Toolbar(viewModel)
        ui.notice?.let { Notice(it, onDismiss = viewModel::dismissNotice) }
        LoadSection(state, onRetry = { viewModel.refresh() }) { data ->
            if (data.items.isEmpty()) {
                Text(Strings.WISHLIST_EMPTY, style = MaterialTheme.typography.bodyLarge)
            } else {
                ItemList(viewModel, data, moving = ui.moving)
            }
        }
    }
    ItemSheetHost(viewModel)
}

/** The floating "Add item" button of the wishlist: there only while the list can be changed and has room. */
@Composable
fun BoxScope.WishlistFab(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val data = state.value
    if (data != null && data.editable && viewModel.canAdd) {
        AppFab(Icons.Filled.Add, Strings.ADD_ITEM, onClick = viewModel::openAdd)
    }
}

@Composable
private fun Toolbar(viewModel: WishlistViewModel) {
    val state by viewModel.data.collectAsState()
    val data = state.value
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            when {
                data == null -> Unit
                !data.editable -> Text(Strings.WISHLIST_CLOSED, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !viewModel.canAdd -> Text(Strings.wishlistFull(WishlistLimits.MAX_ITEMS), style = MaterialTheme.typography.bodyMedium)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Notice(notice: WishlistNotice, onDismiss: () -> Unit) {
    val text = when (notice) {
        WishlistNotice.ChangedElsewhere -> Strings.NOTICE_CHANGED_ELSEWHERE
        WishlistNotice.ItemGone -> Strings.NOTICE_ITEM_GONE
        WishlistNotice.Locked -> Strings.NOTICE_LOCKED
        WishlistNotice.ListFull -> Strings.NOTICE_LIST_FULL
        WishlistNotice.Unreachable -> Strings.SERVER_UNREACHABLE
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onDismiss) { Text(Strings.OK) }
    }
}

/**
 * The items in their order. An item is dragged by its handle: its row follows the pointer, the rows between make room,
 * and a drop saves the new order with one request. The rows keep their resting places throughout a drag and only get
 * offsets, so those places can be measured once; the order the server answers with is what ends the drag.
 */
@Composable
private fun ItemList(viewModel: WishlistViewModel, data: WishlistData, moving: Boolean) {
    val items = data.items
    val drag = remember { ItemDrag() }
    val page = LocalPageScroll.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tops = remember { HashMap<Long, Float>() }
    val heights = remember { HashMap<Long, Float>() }
    val gap = with(density) { ITEM_GAP.toPx() }
    val band = with(density) { EDGE_BAND.toPx() }
    val maxSpeed = with(density) { EDGE_SPEED.toPx() }

    val ids = items.map { it.id }
    // a drag only counts while the list has the order it began with: a reload or the saved order ends it
    val dragId = drag.id?.takeIf { drag.startIds == ids }
    val from = dragId?.let(ids::indexOf) ?: -1
    val live = from >= 0
    fun targetFor(offset: Float) = dragTargetIndex(ids.map { tops[it] ?: 0f }, ids.map { heights[it] ?: 0f }, from, offset)
    val target = when {
        !live -> -1
        drag.settling -> drag.target
        else -> targetFor(drag.offset)
    }
    if (drag.id != null && !live) LaunchedEffect(drag.id) { drag.clear() }

    if (page != null) {
        LaunchedEffect(dragId) {
            if (dragId == null) return@LaunchedEffect
            while (true) {
                withFrameNanos { }
                if (drag.settling) continue
                val view = page.viewport
                val speed = edgeScrollSpeed(drag.pointerY, view.top, view.bottom, band, maxSpeed)
                // the page moving under the pointer is pulled into the offset, so the row stays under the pointer
                if (speed != 0f) drag.offset += page.state.scrollBy(speed)
            }
        }
    }

    /** Lets go of the dragged row: it settles into [to] (or back) and the drag ends once the server's order is shown. */
    fun letGo(to: Int) {
        val id = dragId ?: return
        if (drag.settling) return
        val job = if (to != from) viewModel.moveTo(id, to) else null
        val settledAt = if (job != null) to else from
        drag.target = settledAt
        drag.settling = true
        val rest = settleOffset(ids.map { tops[it] ?: 0f }, ids.map { heights[it] ?: 0f }, from, settledAt)
        scope.launch {
            animate(drag.offset, rest) { value, _ -> drag.offset = value }
            job?.join()
            drag.clear()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ITEM_GAP)) {
        items.forEachIndexed { index, item ->
            key(item.id) {
                val dragged = live && index == from
                val shift = if (live && !dragged) rowShift(index, from, target, (heights[dragId] ?: 0f) + gap) else 0f
                val animatedShift by animateFloatAsState(shift, if (live) spring(stiffness = Spring.StiffnessMediumLow) else snap())
                Box(
                    Modifier.zIndex(if (dragged) 1f else 0f).onGloballyPositioned {
                        tops[item.id] = it.positionInParent().y
                        heights[item.id] = it.size.height.toFloat()
                    },
                ) {
                    ItemRow(
                        item = item,
                        number = if (live) shownNumber(index, from, target) else index + 1,
                        editable = data.editable,
                        dragged = dragged,
                        canMoveUp = data.editable && index > 0 && !moving,
                        canMoveDown = data.editable && index < items.lastIndex && !moving,
                        handleEnabled = !moving,
                        onEdit = { viewModel.openEdit(item.id) },
                        onUp = { viewModel.moveUp(item.id) },
                        onDown = { viewModel.moveDown(item.id) },
                        onDragStart = { pointerY ->
                            if (!moving && !drag.active && ids.all { it in tops && it in heights }) drag.begin(item.id, pointerY, ids)
                        },
                        onDrag = { dy ->
                            if (drag.id == item.id && !drag.settling) {
                                drag.offset += dy
                                drag.pointerY += dy
                            }
                        },
                        onDragEnd = { if (drag.id == item.id) letGo(targetFor(drag.offset)) },
                        onDragCancel = { if (drag.id == item.id) letGo(from) },
                        modifier = Modifier.graphicsLayer { translationY = if (dragged) drag.offset else animatedShift },
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: WishItem,
    number: Int,
    editable: Boolean,
    dragged: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    handleEnabled: Boolean,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDragStart: (pointerY: Float) -> Unit,
    onDrag: (dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // no buttons to move an item: a screen reader moves it with these, which save the order the way a drop does
    val actions = buildList {
        if (canMoveUp) add(CustomAccessibilityAction(Strings.MOVE_UP) { onUp(); true })
        if (canMoveDown) add(CustomAccessibilityAction(Strings.MOVE_DOWN) { onDown(); true })
    }
    SnowCard(
        "wish-$number",
        modifier.fillMaxWidth().semantics { customActions = actions },
        elevation = CardDefaults.cardElevation(defaultElevation = if (dragged) 8.dp else 0.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemText(number, item, Modifier.weight(1f))
            if (editable) {
                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(ITEM_BUTTON)) { Icon(Icons.Filled.Edit, contentDescription = Strings.EDIT_ITEM) }
                    DragHandle(handleEnabled, onDragStart, onDrag, onDragEnd, onDragCancel)
                }
            }
        }
    }
}

/**
 * The grip an item is dragged by. It takes its own pointer events, so a swipe on the rest of the item still scrolls the
 * page and a drag never starts a pull to refresh. [onStart] gets where on the screen the pointer went down.
 */
@Composable
private fun DragHandle(enabled: Boolean, onStart: (Float) -> Unit, onDrag: (Float) -> Unit, onEnd: () -> Unit, onCancel: () -> Unit) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    val cancel by rememberUpdatedState(onCancel)
    Box(
        Modifier.size(ITEM_BUTTON).onGloballyPositioned { coordinates = it }.pointerHoverIcon(PointerIcon.Hand).then(
            if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { at -> coordinates?.let { start(it.localToWindow(at).y) } },
                    onDrag = { change, amount ->
                        change.consume()
                        move(amount.y)
                    },
                    onDragEnd = { end() },
                    onDragCancel = { cancel() },
                )
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(DragHandleIcon, contentDescription = Strings.DRAG_TO_REORDER, tint = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

/** The side of the edit and drag buttons: two of them make a column that keeps the cards compact. */
private val ITEM_BUTTON = 40.dp

/** The space between items, which a dragged item also has to make room for. */
private val ITEM_GAP = 12.dp

/** How close to the top or bottom of the page a dragged item scrolls it, and the fastest it scrolls, per frame. */
private val EDGE_BAND = 72.dp
private val EDGE_SPEED = 16.dp

/**
 * An item's title, note and links, one under another, as the person and their Santa both read it, after the circle with its [number] in the
 * list. A link opens a dialog first. The times are drawn only with [showTimes] (the owner's sheets); a [pill] ("New", "Edited") sits beside
 * them, or alone when they are hidden.
 */
@Composable
internal fun ItemText(number: Int, title: String, note: String?, urls: List<String>, added: String, changed: String?, modifier: Modifier = Modifier, pill: String? = null, showTimes: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(BadgeGap)) {
        NumberBadge(number)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ReadOnlyText(title, style = MaterialTheme.typography.titleMedium, label = "${Strings.ITEM_LABEL} $number", maxLines = 1)
            note?.let { ReadOnlyText(it, style = MaterialTheme.typography.bodyMedium, label = Strings.NOTE_LABEL) }
            urls.forEach { LinkText(it) }
            if (showTimes || pill != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (showTimes) TimesLine(Strings.added(added), changed?.let(Strings::changed), modifier = Modifier.weight(1f, fill = false))
                    pill?.let { StatusPill(it) }
                }
            }
        }
    }
}

@Composable
private fun ItemText(number: Int, item: WishItem, modifier: Modifier = Modifier) = ItemText(number, item.title, item.note, item.urls, item.added, item.changed, modifier)
