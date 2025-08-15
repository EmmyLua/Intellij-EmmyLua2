package com.cppcxy.ide.lsp

import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

interface EmmyLuaCustomApi : LanguageServer {
    @JsonRequest("emmy/annotator")
    fun getAnnotator(params: AnnotatorParams): CompletableFuture<List<Annotator>>
}


data class AnnotatorParams(
    val uri: String
)

data class Annotator(
    val ranges: List<Range>,
    val type: AnnotatorType
)

enum class AnnotatorType {
    ReadOnlyParam,
    Global,
    ReadOnlyLocal,
    MutLocal,
    MutParam,
    DocEm,
    DocStrong
}