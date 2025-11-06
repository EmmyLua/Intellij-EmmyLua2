package com.cppcxy.ide.lsp

import org.eclipse.lsp4j.Range

/**
 * LSP Custom Request for gutter information
 */
data class GutterParams(
    val uri: String
)

/**
 * Gutter information returned from LSP
 */
data class GutterInfo(
    val range: Range,
    val kind: GutterKind,
    val detail: String?,
    val data: Any? = null  // Additional data for detail request
)

/**
 * Request for gutter detail information
 */
data class GutterDetailParams(
    val data: Any
)

/**
 * Location information for navigation
 */
data class GutterLocation(
    val uri: String,
    val line: Int,
    val kind: GutterKind? = null  // Icon type for this location
)

/**
 * Response for gutter detail request
 */
data class GutterDetailResponse(
    val locations: List<GutterLocation>
)

/**
 * Types of gutter icons
 */
enum class GutterKind {
    Class,
    Enum,
    Alias,
    Method,
    Module,
    Override,
}
