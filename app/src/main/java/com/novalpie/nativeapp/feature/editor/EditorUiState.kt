package com.novalpie.nativeapp.feature.editor

import com.novalpie.nativeapp.ui.*
import com.novalpie.nativeapp.model.*

data class EditorUiState(
    val selectedTab: EditorTab = EditorTab.Text,
    val text: String = "",
    val cursorPosition: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val fileName: String? = null,
    val files: List<UploadDocument> = emptyList(),
    val encoding: String = "UTF-8",
    val metadata: EditorBookMetadata = EditorBookMetadata(),
    val chapters: List<UploadChapter> = emptyList(),
    val splitMode: EditorSplitMode = EditorSplitMode.Regex,
    val splitPattern: String = DEFAULT_EDITOR_CHAPTER_REGEX,
    val splitTarget: String = "3000",
    val customScript: String = DEFAULT_EDITOR_CUSTOM_SCRIPT,
    val scriptChunked: Boolean = false,
    val scriptChunkSize: String = "200000",
    val scriptRunId: Long = 0,
    val apiEndpoint: String = "http://localhost:8000",
    val apiTimeoutSeconds: String = "30",
    val apiMarkerMode: EditorMarkerMode = EditorMarkerMode.Incremental,
    val batchMode: EditorBatchMode = EditorBatchMode.Chapters,
    val batchTarget: String = EditorBatchMode.Chapters.defaultTarget,
    val markerValidationErrors: List<String> = emptyList(),
    val aiConfigs: List<WorkspaceLocalApiConfig> = emptyList(),
    val selectedAiConfigId: Long? = null,
    val findText: String = "",
    val replaceText: String = "",
    val findUsesRegex: Boolean = false,
    val archiveName: String = "",
    val archives: List<EditorArchive> = emptyList(),
    val busy: Boolean = false,
    val actionMessage: String? = null
)
