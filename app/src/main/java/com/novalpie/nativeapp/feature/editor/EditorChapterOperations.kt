package com.novalpie.nativeapp.feature.editor

import com.novalpie.nativeapp.data.EditorProcessor
import com.novalpie.nativeapp.model.UploadChapter

internal sealed interface EditorChapterEdit {
    data object Add : EditorChapterEdit
    data class Update(val title: String, val content: String) : EditorChapterEdit
    data object Delete : EditorChapterEdit
}

internal data class EditorChapterDocument(val chapters: List<UploadChapter>, val text: String)

/** Call on the processing dispatcher: both list editing and whole-manuscript serialization live here. */
internal fun editChapterDocument(state: EditorUiState, edit: EditorChapterEdit, index: Int?): EditorChapterDocument {
    val next = when (edit) {
        EditorChapterEdit.Add -> {
            val existing = state.chapters.ifEmpty {
                if (state.text.isNotEmpty()) listOf(UploadChapter("第 1 章", state.text, 1)) else emptyList()
            }
            existing + UploadChapter("第 ${existing.size + 1} 章", "", existing.size + 1)
        }
        is EditorChapterEdit.Update -> {
            require(index != null && index in state.chapters.indices) { "原章节已变化，无法应用本次编辑" }
            state.chapters.toMutableList().also {
                it[index] = it[index].copy(title = edit.title.trim().ifBlank { "第 ${index + 1} 章" }, content = edit.content)
            }
        }
        EditorChapterEdit.Delete -> {
            require(index != null && index in state.chapters.indices) { "原章节已变化，无法删除" }
            state.chapters.filterIndexed { chapterIndex, _ -> chapterIndex != index }
                .mapIndexed { chapterIndex, chapter -> chapter.copy(chapterNumber = chapterIndex + 1) }
        }
    }
    return EditorChapterDocument(next, EditorProcessor.toWebsiteIdentifiers(next))
}
