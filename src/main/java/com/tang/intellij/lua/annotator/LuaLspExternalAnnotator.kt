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
class LuaLspExternalAnnotator :
    ExternalAnnotator<LuaLspExternalAnnotator.CollectedInfo, LuaLspExternalAnnotator.AnnotationResult>() {

    data class CollectedInfo(
        val psiFile: PsiFile,
        val document: Document,
        val uri: String
    )

    data class AnnotationResult(
        val annotators: List<com.cppcxy.ide.lsp.Annotator>,
        val document: Document
    )

    companion object {
        val priority = HighlightSeverity.INFORMATION.myVal + 1000 // 确保最高优先级
        val highlight = HighlightSeverity("LuaLspHighlight", priority)
    }

    override fun collectInformation(file: PsiFile): CollectedInfo? {
        if (file !is LuaPsiFile) return null

        val virtualFile = file.virtualFile ?: return null
        val document = file.viewProvider.document ?: return null

        // 即使存在语法错误也要继续处理
        // LSP 服务器通常能够处理语法错误的文件并提供高亮信息
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
                        try {
                            val ls = languageServerItem.server as EmmyLuaCustomApi
                            val annotatorInfos = ls.getAnnotator(AnnotatorParams(collectedInfo.uri))
                            annotatorInfos.thenAccept { annotators ->
                                if (annotators != null && annotators.isNotEmpty()) {
                                    future.complete(AnnotationResult(annotators, collectedInfo.document))
                                } else {
                                    future.complete(null)
                                }
                            }.exceptionally { _ ->
                                // 记录异常但不完全失败，允许继续处理
                                // 语法错误不应该阻止高亮显示
                                future.complete(null)
                                null
                            }
                        } catch (_: Exception) {
                            // 即使转换失败也不要完全放弃
                            future.complete(null)
                        }
                    } else {
                        future.complete(null)
                    }
                }.exceptionally { _ ->
                    future.complete(null)
                    null
                }

            // 增加超时时间，给 LSP 服务器更多时间处理有语法错误的文件
            future.get(5, TimeUnit.SECONDS)
        } catch (_: Exception) {
            // 即使超时或出错，也返回 null 而不是抛出异常
            null
        }
    }

    override fun apply(
        file: PsiFile,
        annotationResult: AnnotationResult?,
        holder: AnnotationHolder
    ) {
        if (annotationResult == null) return

        try {
            for (annotator in annotationResult.annotators) {
                for (range in annotator.ranges) {
                    val textRange = lspRangeToTextRange(range, annotationResult.document)
                    if (textRange != null && textRange.startOffset >= 0 && textRange.endOffset <= annotationResult.document.textLength) {
                        val textAttributesKey = LuaHighlightingData.getLspHighlightKey(annotator.type)
                        holder.newSilentAnnotation(highlight)
                            .range(textRange)
                            .textAttributes(textAttributesKey)
                            .needsUpdateOnTyping(false)
                            .create()
                    }
                }
            }
        } catch (_: Exception) {
            // 防止在应用注解时出现异常导致整个注解过程失败
            // 语法错误情况下文档可能不稳定，需要更加谨慎
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
        } catch (_: Exception) {
            null
        }
    }
}
