package xyz.sevive.arcaeaoffline.ui.screens.ocr.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import xyz.sevive.arcaeaoffline.R
import xyz.sevive.arcaeaoffline.core.database.entities.PlayResult


@Composable
fun OcrQueueList(
    uiItems: List<OcrQueueScreenViewModel.TaskUiItem>,
    onSaveScore: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onEditPlayResult: (Long, PlayResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_padding)),
    ) {
        items(uiItems, key = { it.id }) { uiItem ->
            OcrQueueListItem(
                uiItem,
                onDeleteTask = onDeleteTask,
                onEditPlayResult = onEditPlayResult,
                onSavePlayResult = onSaveScore,
            )
        }
    }
}
