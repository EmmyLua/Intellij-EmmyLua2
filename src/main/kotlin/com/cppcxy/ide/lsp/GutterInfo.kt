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
    val detail: String?
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
