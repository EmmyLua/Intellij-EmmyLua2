package com.cppcxy.ide.lsp

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

interface EmmyLuaCustomApi : LanguageServer {
    @JsonRequest("emmy/gutter")
    fun getGutter(params: GutterParams): CompletableFuture<List<GutterInfo>>
    
    @JsonRequest("emmy/gutter/detail")
    fun getGutterDetail(params: GutterDetailParams): CompletableFuture<GutterDetailResponse>
}
