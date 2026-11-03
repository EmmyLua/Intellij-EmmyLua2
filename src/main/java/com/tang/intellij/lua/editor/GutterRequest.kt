package com.tang.intellij.lua.editor

import com.cppcxy.ide.lsp.GutterInfo
import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Resolves one gutter request without treating unavailable servers or missing replies as empty data.
 * Cancellation during request creation propagates when the callback runs inline on the calling thread.
 * After an asynchronous handoff, cancellation completes the returned future exceptionally.
 */
internal fun <S> requestGutterData(
    findServer: () -> CompletableFuture<out S?>,
    load: (S) -> CompletableFuture<List<GutterInfo>>
): CompletableFuture<List<GutterInfo>> {
    val result = CompletableFuture<List<GutterInfo>>()
    val serverFuture = try {
        findServer()
    } catch (canceled: ProcessCanceledException) {
        throw canceled
    } catch (failure: Exception) {
        result.completeExceptionally(failure)
        return result
    }
    val callingThread = Thread.currentThread()
    // The thread check below confines access to these locals to the calling thread.
    var registeringCompletion = true
    var inlineCancellation: ProcessCanceledException? = null
    serverFuture.whenComplete { server, error ->
        if (result.isDone) return@whenComplete
        if (error != null) {
            result.completeExceptionally(error)
            return@whenComplete
        }
        if (server == null) {
            result.completeExceptionally(IllegalStateException("Gutter language server is unavailable"))
            return@whenComplete
        }
        try {
            val request = load(server)
            result.whenComplete { _, _ -> if (result.isCompletedExceptionally) request.cancel(true) }
            request.whenComplete { infos, failure ->
                when {
                    failure != null -> result.completeExceptionally(failure)
                    infos == null -> result.completeExceptionally(IllegalStateException("Gutter request returned no response"))
                    else -> result.complete(infos)
                }
            }
        } catch (canceled: ProcessCanceledException) {
            result.completeExceptionally(canceled)
            // CompletableFuture captures callback exceptions, including callbacks invoked inline.
            if (Thread.currentThread() === callingThread && registeringCompletion) {
                inlineCancellation = canceled
            }
        } catch (failure: Exception) {
            result.completeExceptionally(failure)
        }
    }
    registeringCompletion = false
    inlineCancellation?.let { throw it }
    return result.orTimeout(10, TimeUnit.SECONDS)
}
