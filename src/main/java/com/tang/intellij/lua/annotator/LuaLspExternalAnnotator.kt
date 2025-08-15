package com.tang.intellij.lua.annotator

import com.cppcxy.ide.lsp.AnnotatorParams
import com.cppcxy.ide.lsp.EmmyLuaCustomApi
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerItem
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.tang.intellij.lua.highlighting.LuaHighlightingData
import com.tang.intellij.lua.psi.LuaPsiFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * LSP 外部注解器，使用最高优先级确保渲染效果
 * 解决多个高亮系统冲突时只有背景色高亮显示的问题
 */
class LuaLspExternalAnnotator : ExternalAnnotator<LuaLspExternalAnnotator.CollectedInfo, LuaLspExternalAnnotator.AnnotationResult>() {

    data class CollectedInfo(
        val psiFile: PsiFile,
        val document: Document,
        val uri: String
    )

    data class AnnotationResult(
        val annotators: List<com.cppcxy.ide.lsp.Annotator>,
        val document: Document
    )

    override fun collectInformation(file: PsiFile): CollectedInfo? {
        if (file !is LuaPsiFile) return null

        val virtualFile = file.virtualFile ?: return null
        val document = file.viewProvider.document ?: return null

        return CollectedInfo(
            psiFile = file,
            document = document,
            uri = virtualFile.url
        )
    }

    override fun doAnnotate(collectedInfo: CollectedInfo): AnnotationResult? {
        val project = collectedInfo.psiFile.project

        return try {
            val future = CompletableFuture<AnnotationResult?>()

            LanguageServerManager.getInstance(project)
                .getLanguageServer("EmmyLua")
                .thenAccept { languageServerItem: LanguageServerItem? ->
                    if (languageServerItem != null) {
                        val ls = languageServerItem.server as EmmyLuaCustomApi
                        val annotatorInfos = ls.getAnnotator(AnnotatorParams(collectedInfo.uri))
                        annotatorInfos.thenAccept { annotators ->
                            if (annotators != null) {
                                future.complete(AnnotationResult(annotators, collectedInfo.document))
                            } else {
                                future.complete(null)
                            }
                        }.exceptionally {
                            future.complete(null)
                            null
                        }
                    } else {
                        future.complete(null)
                    }
                }

            // 等待结果，但不要阻塞太久
            future.get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }
    }

    override fun apply(
        file: PsiFile,
        annotationResult: AnnotationResult?,
        holder: AnnotationHolder
    ) {
        if (annotationResult == null) return

        for (annotator in annotationResult.annotators) {
            for (range in annotator.ranges) {
                val textRange = lspRangeToTextRange(range, annotationResult.document)
                if (textRange != null) {
                    val textAttributesKey = LuaHighlightingData.getLspHighlightKey(annotator.type)

                    // 使用WEAK_WARNING级别，并强制使用textAttributes确保显示优先级
                    holder.newSilentAnnotation(HighlightSeverity.WEAK_WARNING)
                        .range(textRange)
                        .textAttributes(textAttributesKey)
                        .needsUpdateOnTyping(false)
                        .create()
                }
            }
        }
    }

    private fun lspRangeToTextRange(range: org.eclipse.lsp4j.Range, document: Document): TextRange? {
        return try {
            val startLine = range.start.line
            val startChar = range.start.character
            val endLine = range.end.line
            val endChar = range.end.character

            val startOffset = document.getLineStartOffset(startLine) + startChar
            val endOffset = document.getLineStartOffset(endLine) + endChar

            if (startOffset >= 0 && endOffset >= startOffset && endOffset <= document.textLength) {
                TextRange(startOffset, endOffset)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
