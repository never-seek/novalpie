package com.novalpie.nativeapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class EditorScriptOptions(
    val mode: String,
    val chunkSize: Int,
    val chunkIndex: Int,
    val totalChunks: Int,
    val textLength: Int,
    val isFirstChunk: Boolean,
    val isLastChunk: Boolean
)

internal fun chunkEditorScriptText(text: String, targetSize: Int): List<String> {
    require(targetSize > 0) { "Chunk size must be positive" }
    if (text.isEmpty()) return listOf("")
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        val targetEnd = (start + targetSize).coerceAtMost(text.length)
        val end = if (targetEnd >= text.length) {
            text.length
        } else {
            text.indexOf('\n', targetEnd).takeIf { it >= 0 }?.plus(1) ?: text.length
        }
        chunks += text.substring(start, end)
        start = end
    }
    return chunks
}

internal fun editorScriptOptions(chunks: List<String>, textLength: Int): List<EditorScriptOptions> {
    val mode = if (chunks.size > 1) "chunked" else "full"
    return chunks.mapIndexed { index, chunk ->
        EditorScriptOptions(
            mode = mode,
            chunkSize = chunk.length,
            chunkIndex = index,
            totalChunks = chunks.size,
            textLength = textLength,
            isFirstChunk = index == 0,
            isLastChunk = index == chunks.lastIndex
        )
    }
}

internal fun buildEditorScriptProgram(
    script: String,
    text: String,
    options: EditorScriptOptions
): String {
    val optionsJson = JSONObject()
        .put("mode", options.mode)
        .put("chunkSize", options.chunkSize)
        .put("chunkIndex", options.chunkIndex)
        .put("totalChunks", options.totalChunks)
        .put("textLength", options.textLength)
        .put("isFirstChunk", options.isFirstChunk)
        .put("isLastChunk", options.isLastChunk)
        .toString()
    return """
        (function() {
          "use strict";
          const __source = ${JSONObject.quote(script)};
          const __text = ${JSONObject.quote(text)};
          const __options = $optionsJson;
          const __markers = [];
          const __logs = [];
          const insertMarker = function(index, type) {
            const value = Number(index);
            if (!Number.isFinite(value)) throw new Error("insertMarker index must be numeric");
            __markers.push({ index: Math.max(0, Math.floor(value)), type: type === "content" ? "content" : "title" });
          };
          const findMatches = function(pattern, flags) {
            const regex = pattern instanceof RegExp ? pattern : new RegExp(String(pattern), flags || "gm");
            const matches = [];
            let match;
            while ((match = regex.exec(__text)) !== null) {
              matches.push({ index: match.index, value: match[0], groups: Array.prototype.slice.call(match, 1) });
              if (match[0] === "") regex.lastIndex += 1;
            }
            return matches;
          };
          const splitByParagraphs = function(value) { return String(value).split(/\n\s*\n/); };
          const splitByWords = function(value) { return String(value).trim().split(/\s+/).filter(Boolean); };
          const getParagraphs = function() { return splitByParagraphs(__text).filter(function(value) { return value.trim().length > 0; }); };
          const getWordCount = function() { return splitByWords(__text).length; };
          const getLineCount = function() { return __text.split("\n").length; };
          const localConsole = {
            log: function(value) { __logs.push(String(value)); },
            info: function(value) { __logs.push(String(value)); },
            warn: function(value) { __logs.push(String(value)); },
            error: function(value) { __logs.push(String(value)); }
          };
          try {
            const factory = new Function(
              "insertMarker", "findMatches", "splitByParagraphs", "splitByWords",
              "getParagraphs", "getWordCount", "getLineCount", "console",
              __source + "\nreturn typeof processText === 'function' ? processText : null;"
            );
            const processText = factory(
              insertMarker, findMatches, splitByParagraphs, splitByWords,
              getParagraphs, getWordCount, getLineCount, localConsole
            );
            if (typeof processText !== "function") throw new Error("Script must define processText(text, options)");
            let output = processText.length >= 2 ? processText(__text, __options) : processText(__text);
            if (output === undefined || output === null) output = __text;
            output = String(output);
            __markers.sort(function(left, right) { return right.index - left.index; });
            __markers.forEach(function(marker) {
              const number = String(marker.index).padStart(5, "0");
              const token = marker.type === "content" ? "##__C[" + number + "]__##\n" : "##__T[" + number + "]__##\n";
              const position = Math.min(marker.index, output.length);
              output = output.slice(0, position) + token + output.slice(position);
            });
            return JSON.stringify({ ok: true, result: output, logs: __logs });
          } catch (error) {
            return JSON.stringify({ ok: false, error: error && error.message ? error.message : String(error), logs: __logs });
          }
        })();
    """.trimIndent()
}

internal fun parseEditorScriptCallback(raw: String): String {
    val first = JSONTokener(raw).nextValue()
    val payload = when (first) {
        is String -> JSONObject(first)
        is JSONObject -> first
        else -> throw IllegalArgumentException("Script returned an invalid response")
    }
    if (!payload.optBoolean("ok", false)) {
        throw IllegalArgumentException(payload.optString("error", "Script execution failed"))
    }
    return payload.optString("result", "")
}

internal class EditorScriptEngine(private val context: Context) {
    suspend fun process(script: String, text: String, chunked: Boolean, targetChunkSize: Int): String =
        withContext(Dispatchers.Main.immediate) {
            val chunks = if (chunked) chunkEditorScriptText(text, targetChunkSize) else listOf(text)
            val options = editorScriptOptions(chunks, text.length).map { option ->
                if (chunked) option.copy(mode = "chunked", chunkSize = targetChunkSize)
                else option.copy(mode = "full", chunkSize = text.length)
            }
            buildString(text.length) {
                chunks.forEachIndexed { index, chunk ->
                    append(evaluate(buildEditorScriptProgram(script, chunk, options[index])))
                }
            }
        }

    @SuppressLint("SetJavaScriptEnabled") // Local script sandbox disables network, files, content, and popups.
    private suspend fun evaluate(program: String): String = withTimeout(SCRIPT_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            var completed = false
            fun finish(result: Result<String>) {
                if (completed) return
                completed = true
                webView.stopLoading()
                webView.destroy()
                result.fold(continuation::resume, continuation::resumeWithException)
            }
            webView.settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                domStorageEnabled = false
                // databaseEnabled dropped: the WebSQL API it gated is deprecated and
                // removed from modern WebView, and its default is already false, so the
                // sandbox posture here is unchanged.
                blockNetworkLoads = true
                allowFileAccess = false
                allowContentAccess = false
                loadsImagesAutomatically = false
            }
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript(program) { raw ->
                        finish(runCatching { parseEditorScriptCallback(raw) })
                    }
                }
            }
            continuation.invokeOnCancellation {
                webView.post {
                    if (!completed) {
                        completed = true
                        webView.stopLoading()
                        webView.destroy()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, "<html><body></body></html>", "text/html", "UTF-8", null)
        }
    }

    private companion object {
        const val SCRIPT_TIMEOUT_MS = 15_000L
    }
}
