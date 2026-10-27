package dev.ofarafal.shizucleaner.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ofarafal.shizucleaner.data.DupGroup
import dev.ofarafal.shizucleaner.data.FileHit
import dev.ofarafal.shizucleaner.shizuku.ShizukuState

private sealed interface FileRow {
    data class Header(val text: String, val key: String) : FileRow
    data class Item(val hit: FileHit) : FileRow
}

@Composable
fun FilesScreen(shizuku: ShizukuState, vm: FilesViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ready = shizuku == ShizukuState.READY
    val busy = ui.progress != null
    val scanned = ui.kind in ui.scanned

    val rows: List<FileRow> = when (ui.kind) {
        FileKind.LARGE -> ui.large.map { FileRow.Item(it) }
        FileKind.APKS -> ui.apks.map { FileRow.Item(it) }
        FileKind.EMPTY -> ui.empty.map { FileRow.Item(it) }
        FileKind.DUPLICATES -> ui.dups.flatMapIndexed { i, g -> dupRows(i, g) }
    }
    val sizeBySelected = when (ui.kind) {
        FileKind.DUPLICATES -> ui.dups.flatMap { it.files }
        FileKind.LARGE -> ui.large
        FileKind.APKS -> ui.apks
        FileKind.EMPTY -> ui.empty
    }.filter { it.path in ui.selected }.sumOf { it.size }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FileKind.entries.forEach { k ->
                FilterChip(selected = ui.kind == k, onClick = { vm.setKind(k) }, label = { Text(k.label) })
            }
        }

        if (ui.kind == FileKind.LARGE) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(50, 200, 1000).forEach { mb ->
                    FilterChip(
                        selected = ui.largeMinMb == mb,
                        onClick = { vm.setLargeMin(mb) },
                        label = { Text(if (mb >= 1000) "≥ 1 GB" else "≥ $mb MB") },
                    )
                }
            }
        }

        Text(
            when (ui.kind) {
                FileKind.LARGE -> "Największe pliki w pamięci (poza Android/)."
                FileKind.DUPLICATES -> "Pliki o identycznej zawartości (≥ 1 MB). Zaznaczenie zostawia najstarszą kopię w każdej grupie."
                FileKind.APKS -> "Instalatory .apk leżące w pamięci."
                FileKind.EMPTY -> "Foldery bez żadnych plików (z podfolderami włącznie)."
            },
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )

        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = vm::scan, enabled = ready && !ui.scanning && !busy) {
                Text(if (scanned) "Skanuj ponownie" else "Skanuj")
            }
            TextButton(onClick = vm::selectAllCurrent, enabled = rows.isNotEmpty()) {
                Text(if (ui.kind == FileKind.DUPLICATES) "Zaznacz duplikaty" else "Zaznacz wszystko")
            }
            TextButton(onClick = vm::clearSelection, enabled = ui.selected.isNotEmpty()) { Text("Odznacz") }
        }
        ui.error?.let { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }

        Box(Modifier.weight(1f)) {
            when {
                ui.scanning -> BusyBox(
                    if (ui.kind == FileKind.DUPLICATES) "Szukam duplikatów… (to może potrwać kilka minut)" else "Skanuję pamięć…",
                )
                !scanned -> Hint("Naciśnij „Skanuj”.")
                rows.isEmpty() -> Hint("Nic nie znaleziono.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = {
                        when (it) {
                            is FileRow.Header -> "h:" + it.key
                            is FileRow.Item -> it.hit.path
                        }
                    }) { row ->
                        when (row) {
                            is FileRow.Header -> Text(
                                row.text,
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            is FileRow.Item -> SelectRow(
                                title = row.hit.path.removePrefix("/storage/emulated/0/"),
                                subtitle = formatDate(row.hit.mtime).ifEmpty { null },
                                trailing = if (ui.kind == FileKind.EMPTY) null else formatSize(row.hit.size),
                                checked = row.hit.path in ui.selected,
                                onToggle = { vm.toggle(row.hit.path) },
                            )
                        }
                    }
                }
            }
        }

        ActionBar(
            label = "Usuń zaznaczone (${ui.selected.size}" +
                (if (ui.kind == FileKind.EMPTY || sizeBySelected == 0L) "" else ", ${formatSize(sizeBySelected)}") + ")",
            enabled = ready && !busy && ui.selected.isNotEmpty(),
            progress = ui.progress,
            onClick = { vm.askConfirm(true) },
        )
    }

    if (ui.confirm) {
        ConfirmDeleteDialog(
            title = "Usunąć ${ui.selected.size} pozycji?",
            body = "Zostaną trwale skasowane. Zaznaczone: " +
                ui.selected.take(3).joinToString { it.substringAfterLast('/') } +
                if (ui.selected.size > 3) " i inne." else ".",
            passes = if (ui.kind == FileKind.EMPTY) 0 else vm.passes,
            confirmLabel = "Usuń",
            onConfirm = vm::deleteSelected,
            onDismiss = { vm.askConfirm(false) },
        )
    }

    ui.report?.let { ReportDialog("Gotowe", it, vm::dismissReport) }
}

private fun dupRows(index: Int, g: DupGroup): List<FileRow> =
    listOf(FileRow.Header("${formatSize(g.size)} × ${g.files.size} (zmarnowane ${formatSize(g.wasted)})", index.toString())) +
        g.files.map { FileRow.Item(it) }
