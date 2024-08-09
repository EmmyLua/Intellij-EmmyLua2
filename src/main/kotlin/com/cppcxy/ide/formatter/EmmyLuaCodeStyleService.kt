package com.cppcxy.ide.formatter

import com.intellij.configurationStore.NOTIFICATION_GROUP_ID
import com.intellij.execution.ExecutionException
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.psi.PsiFile
import com.tang.intellij.lua.lang.LuaLanguage


class EmmyLuaCodeStyle : AsyncDocumentFormattingService() {

    private val FEATURES: MutableSet<FormattingService.Feature> = mutableSetOf(
        FormattingService.Feature.AD_HOC_FORMATTING,
        FormattingService.Feature.FORMAT_FRAGMENTS
    )

    override fun getFeatures(): MutableSet<FormattingService.Feature> {
        return FEATURES
    }

    override fun canFormat(file: PsiFile): Boolean {
        return file.language is LuaLanguage
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        try {
            val documentText = request.documentText
            val ranges = request.formattingRanges
            return object : FormattingTask {
                override fun run() {
                    val range = ranges.first();
                    if (range.startOffset == 0 && range.endOffset == documentText.length) {
                        CodeFormatAdaptor.runCodeFormat(
                            request.context.virtualFile?.path,
                            documentText,
                            object : ReformatAccept {
                                override fun accept(s: String) {
                                    request.onTextReady(s);
                                }

                                override fun error(s: String) {
                                    request.onError("formatting error", s)
                                }
                            })
                    } else {
                        CodeFormatAdaptor.runCodeRangeFormat(
                            request.context.virtualFile?.path,
                            range,
                            documentText,
                            object : ReformatAccept {
                                override fun accept(s: String) {
                                    request.onTextReady(s);
                                }

                                override fun error(s: String) {
                                    request.onError("range formatting error", s)
                                }
                            })
                    }
                }

                override fun cancel(): Boolean {
                    return true
                }

                override fun isRunUnderProgress(): Boolean {
                    return true
                }
            }

        } catch (e: ExecutionException) {
            e.message?.let { request.onError("EmmyLuaCodeStyle", it) };
            return null;
        }

    }

    override fun getNotificationGroupId(): String {
        return NOTIFICATION_GROUP_ID
    }

    override fun getName(): String {
        return "formatting"
    }
}