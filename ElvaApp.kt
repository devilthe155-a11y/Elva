package com.elva.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.elva.music.ElvaViewModel
import com.elva.music.data.Track
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("Home", Icons.Default.Home), Search("Search", Icons.Default.Search), Library("Library", Icons.Default.LibraryMusic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElvaApp(vm: ElvaViewModel) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var showPlayer by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    val player = vm.player

    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) { player.refreshPosition(); delay(500) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                player.current?.let { MiniPlayer(vm, it) { showPlayer = true } }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.values().forEach {
                        NavigationBarItem(
                            selected = tab == it, onClick = { tab = it },
                            icon = { Icon(it.icon, it.label) }, label = { Text(it.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                Tab.Home -> HomeScreen(vm) { menuTrack = it }
                Tab.Search -> SearchScreen(vm) { menuTrack = it }
                Tab.Library -> LibraryScreen(vm) { menuTrack = it }
            }
        }
    }

    if (showPlayer && player.current != null) NowPlaying(vm) { showPlayer = false }
    menuTrack?.let { TrackMenu(vm, it) { menuTrack = null } }
}

@Composable
private fun Title(text: String) = Text(
    text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
)

@Composable
private fun HomeScreen(vm: ElvaViewModel, onMore: (Track) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Title("Elva") }
        vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
        if (vm.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        if (vm.trending.isNotEmpty()) {
            item { Shelf("Trending this week", vm.trending, vm) }
            vm.genreRows.forEach { (genre, list) -> item { Shelf(genre.replaceFirstChar { it.uppercase() }, list, vm) } }
            item { Title("Top tracks") }
            itemsIndexed(vm.trending) { i, t -> TrackRow(t, onMore = { onMore(t) }) { vm.player.play(vm.trending, i) } }
        }
    }
}

@Composable
private fun Shelf(title: String, tracks: List<Track>, vm: ElvaViewModel) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(tracks) { i, t ->
            Column(Modifier.width(140.dp).clickable { vm.player.play(tracks, i) }) {
                Cover(t.image, Modifier.size(140.dp))
                Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                Text(t.artist_name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchScreen(vm: ElvaViewModel, onMore: (Track) -> Unit) {
    var q by remember { mutableStateOf("") }
    Column {
        Title("Search")
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LaunchedEffect(q) { delay(400); vm.search(q) }
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
        LazyColumn {
            itemsIndexed(vm.results) { i, t -> TrackRow(t, onMore = { onMore(t) }) { vm.player.play(vm.results, i) } }
        }
    }
}

@Composable
private fun LibraryScreen(vm: ElvaViewModel, onMore: (Track) -> Unit) {
    var openList by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    val lib = vm.library
    val tracks = when (openList) {
        null -> null
        "Liked songs" -> lib.liked
        else -> lib.playlists.firstOrNull { it.name == openList }?.tracks.orEmpty()
    }
    Column {
        if (tracks != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ openList = null }) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(openList!!, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (openList != "Liked songs") IconButton({ vm.deletePlaylist(openList!!); openList = null }) { Icon(Icons.Default.Delete, "Delete") }
            }
            if (tracks.isEmpty()) Text("Nothing here yet.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn { itemsIndexed(tracks) { i, t -> TrackRow(t, onMore = { onMore(t) }) { vm.player.play(tracks, i) } } }
        } else {
            Title("Your Library")
            ListItem(
                headlineContent = { Text("Liked songs") }, supportingContent = { Text("${lib.liked.size} songs") },
                leadingContent = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { openList = "Liked songs" }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            lib.playlists.forEach { p ->
                ListItem(
                    headlineContent = { Text(p.name) }, supportingContent = { Text("${p.tracks.size} songs") },
                    leadingContent = { Icon(Icons.Default.QueueMusic, null) },
                    modifier = Modifier.clickable { openList = p.name }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(newName, { newName = it }, singleLine = true, placeholder = { Text("New playlist") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button({ vm.createPlaylist(newName); newName = "" }) { Text("Add") }
            }
        }
    }
}

@Composable
private fun TrackRow(t: Track, onMore: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(t.image, Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.artist_name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onMore) { Icon(Icons.Default.MoreVert, "More") }
    }
}

@Composable
private fun Cover(url: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (url.isNotBlank()) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun MiniPlayer(vm: ElvaViewModel, t: Track, onOpen: () -> Unit) {
    val p = vm.player
    Column(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onOpen)) {
        LinearProgressIndicator(
            progress = { if (p.durationMs > 0) p.positionMs.toFloat() / p.durationMs else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
        )
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(t.image, Modifier.size(44.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(t.artist_name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton({ vm.toggleLike(t) }) { Icon(if (vm.isLiked(t)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.primary) }
            IconButton(p::toggle) { Icon(if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause") }
        }
    }
}

@Composable
private fun NowPlaying(vm: ElvaViewModel, onClose: () -> Unit) {
    val p = vm.player
    val t = p.current ?: return
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) { IconButton(onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close") } }
                Spacer(Modifier.height(16.dp))
                Cover(t.image, Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(t.artist_name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ vm.toggleLike(t) }) { Icon(if (vm.isLiked(t)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.primary) }
                }
                var drag by remember { mutableStateOf<Float?>(null) }
                Slider(
                    value = drag ?: if (p.durationMs > 0) p.positionMs.toFloat() / p.durationMs else 0f,
                    onValueChange = { drag = it },
                    onValueChangeFinished = { drag?.let { p.seekTo((it * p.durationMs).toLong()) }; drag = null },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(p.positionMs), fontSize = 12.sp); Text(fmt(p.durationMs), fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(p::toggleShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (p.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(p::previous) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                    FilledIconButton(p::toggle, Modifier.size(72.dp)) { Icon(if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", Modifier.size(40.dp)) }
                    IconButton(p::next) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
                    IconButton(p::toggleRepeat) { Icon(Icons.Default.Repeat, "Repeat", tint = if (p.repeatAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackMenu(vm: ElvaViewModel, t: Track, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Text(t.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp), maxLines = 1)
        ListItem(
            headlineContent = { Text(if (vm.isLiked(t)) "Remove from Liked songs" else "Add to Liked songs") },
            modifier = Modifier.clickable { vm.toggleLike(t); onDismiss() },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
        vm.library.playlists.forEach { p ->
            ListItem(
                headlineContent = { Text("Add to ${p.name}") },
                modifier = Modifier.clickable { vm.addToPlaylist(p.name, t); onDismiss() },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }
