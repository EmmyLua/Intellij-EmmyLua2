package com.tang.intellij.lua.editor

import com.cppcxy.ide.lsp.GutterInfo
import com.intellij.openapi.progress.ProcessCanceledException
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

class GutterRequestTest {
    @Test fun synchronousLookupFailureBecomesAFailedFuture() {
        val failure = IllegalStateException("server manager unavailable")
        val result = requestGutterData<String>({ throw failure }) {
            fail("lookup failed")
            CompletableFuture()
        }
        assertSame(failure, assertThrows(CompletionException::class.java) { result.join() }.cause)
    }

    @Test fun synchronousHighlightingCancellationStillPropagates() {
        val canceled = ProcessCanceledException()
        val actual = assertThrows(ProcessCanceledException::class.java) {
            requestGutterData<String>({ throw canceled }) { CompletableFuture() }
        }
        assertSame(canceled, actual)
    }

    @Test fun missingServerDoesNotCacheAnEmptyReply() {
        val cache = GutterRequestCache<String>()
        var lookups = 0
        val load = {
            requestGutterData({
                lookups++
                CompletableFuture.completedFuture(if (lookups == 1) null else "ready")
            }) { CompletableFuture.completedFuture(emptyList<GutterInfo>()) }
        }
        assertNull(cache.getOrRequest("file", 1, load) { fail("unavailable server notification") })
        assertNull(cache.getOrRequest("file", 1, load) {})
        assertEquals(emptyList<GutterInfo>(), cache.getOrRequest("file", 1, load) {})
        assertEquals(2, lookups)
    }

    @Test fun missingReplyIsAFailureButAnEmptyReplyIsValid() {
        val missing = CompletableFuture<List<GutterInfo>>()
        val failed = requestGutterData({ CompletableFuture.completedFuture("ready") }) { missing }
        missing.complete(null)
        assertTrue(failed.isCompletedExceptionally)
        val empty = requestGutterData({ CompletableFuture.completedFuture("ready") }) {
            CompletableFuture.completedFuture(emptyList<GutterInfo>())
        }
        assertEquals(emptyList<GutterInfo>(), empty.join())
    }

    @Test fun synchronousRequestFailureBecomesAFailedFuture() {
        val failure = IllegalStateException("unsupported request")
        val result = requestGutterData({ CompletableFuture.completedFuture("ready") }) { throw failure }
        assertSame(failure, assertThrows(CompletionException::class.java) { result.join() }.cause)
    }

    @Test fun synchronousRequestCancellationStillPropagates() {
        val canceled = ProcessCanceledException()
        val actual = assertThrows(ProcessCanceledException::class.java) {
            requestGutterData({ CompletableFuture.completedFuture("ready") }) { throw canceled }
        }
        assertSame(canceled, actual)
    }

    @Test fun cancellationAfterAnAsynchronousLookupCancelsTheResult() {
        val lookup = CompletableFuture<String>()
        val canceled = ProcessCanceledException()
        val result = requestGutterData({ lookup }) { throw canceled }
        lookup.complete("ready")
        assertTrue(result.isCancelled)
        val actual = assertThrows(CancellationException::class.java) { result.join() }
        assertTrue(actual === canceled || actual.cause === canceled)
    }

    @Test fun cancellationBeforeLookupCompletesSuppressesTheRequest() {
        val lookup = CompletableFuture<String>()
        val result = requestGutterData({ lookup }) {
            fail("canceled lookup must not issue a request")
            CompletableFuture()
        }
        result.cancel(true)
        lookup.complete("ready")
        assertTrue(result.isCancelled)
        assertFalse(lookup.isCancelled)
    }

    @Test fun cancellationAndTimeoutCancelTheOutstandingRequest() {
        for (cancel in listOf(true, false)) {
            val request = CompletableFuture<List<GutterInfo>>()
            val result = requestGutterData({ CompletableFuture.completedFuture("ready") }) { request }
            if (cancel) result.cancel(true) else result.completeExceptionally(TimeoutException())
            assertTrue(request.isCancelled)
            assertTrue(result.isCompletedExceptionally)
        }
    }
}
