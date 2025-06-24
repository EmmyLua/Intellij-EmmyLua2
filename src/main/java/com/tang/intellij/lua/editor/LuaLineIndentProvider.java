/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.editor;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider;
import com.intellij.psi.tree.IElementType;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides smart line indentation for Lua files
 */
public class LuaLineIndentProvider implements LineIndentProvider {

    @Override
    public @Nullable String getLineIndent(@NotNull Project project, @NotNull Editor editor, @Nullable Language language, int offset) {
        if (!isSuitableFor(language)) {
            return null;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            return null;
        }

        return calculateIndent(project, editor, file, offset);
    }

    @Override
    public boolean isSuitableFor(@Nullable Language language) {
        return language instanceof LuaLanguage;
    }

    private @Nullable String calculateIndent(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, int offset) {
        Document document = editor.getDocument();

        if (offset == 0) {
            return "";
        }

        // 获取当前行号
        int lineNumber = document.getLineNumber(offset);
        if (lineNumber == 0) {
            return "";
        }

        // 获取前一行的缩进
        String previousLineIndent = getPreviousLineIndent(document, lineNumber);

        // 获取前一行的最后一个有意义的元素
        PsiElement elementAtPrevLine = getLastMeaningfulElementInPreviousLine(file, document, lineNumber);

        if (elementAtPrevLine == null) {
            return previousLineIndent;
        }

        // 根据前一行的内容决定缩进
        return calculateIndentBasedOnPreviousLine(editor, project, elementAtPrevLine, previousLineIndent);
    }

    private String getPreviousLineIndent(Document document, int currentLineNumber) {
        if (currentLineNumber <= 0) {
            return "";
        }

        int prevLineStartOffset = document.getLineStartOffset(currentLineNumber - 1);
        int prevLineEndOffset = document.getLineEndOffset(currentLineNumber - 1);
        String prevLineText = document.getText(new TextRange(prevLineStartOffset, prevLineEndOffset));

        // 提取前一行的缩进
        StringBuilder indent = new StringBuilder();
        for (char c : prevLineText.toCharArray()) {
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }

    private @Nullable PsiElement getLastMeaningfulElementInPreviousLine(PsiFile file, Document document, int currentLineNumber) {
        if (currentLineNumber <= 0) {
            return null;
        }

        int prevLineStartOffset = document.getLineStartOffset(currentLineNumber - 1);
        int prevLineEndOffset = document.getLineEndOffset(currentLineNumber - 1);

        // 从行尾开始向前查找有意义的元素
        for (int offset = prevLineEndOffset - 1; offset >= prevLineStartOffset; offset--) {
            PsiElement element = file.findElementAt(offset);
            if (element != null && !isWhitespaceOrComment(element)) {
                return element;
            }
        }

        return null;
    }

    private boolean isWhitespaceOrComment(PsiElement element) {
        IElementType elementType = element.getNode().getElementType();
        return
                elementType == LuaTypes.SHORT_COMMENT ||
                        elementType == LuaTypes.DOC_COMMENT ||
                        elementType == LuaTypes.BLOCK_COMMENT;
    }

    private String calculateIndentBasedOnPreviousLine(Editor editor, Project project, PsiElement element, String baseIndent) {
        String indentUnit = getIndentUnit(editor, project);

        // 检查是否是table相关的缩进
        String tableIndent = calculateTableIndent(element, baseIndent, indentUnit);
        if (tableIndent != null) {
            return tableIndent;
        }

        // 查找包含该元素的语句或块
//        PsiElement parent = element;
//        while (parent != null) {
//            if (shouldIncreaseIndent(parent)) {
//                return baseIndent + indentUnit;
//            }
//
//            if (parent instanceof LuaBlock) {
//                break;
//            }
//
//            parent = parent.getParent();
//        }

        // 检查元素本身的类型
        IElementType elementType = element.getNode().getElementType();

        // 需要增加缩进的情况
        if (elementType == LuaTypes.DO ||
                elementType == LuaTypes.THEN ||
                elementType == LuaTypes.ELSE ||
                elementType == LuaTypes.ELSEIF ||
                elementType == LuaTypes.FUNCTION ||
                elementType == LuaTypes.LOCAL ||
                elementType == LuaTypes.REPEAT ||
                elementType == LuaTypes.LCURLY ||
                elementType == LuaTypes.LPAREN ||
                elementType == LuaTypes.AND ||
                elementType == LuaTypes.OR
        ) {
            return baseIndent + indentUnit;
        }
        return baseIndent;
    }

    private boolean shouldIncreaseIndent(PsiElement element) {
        if (element instanceof LuaBlock) {
            // 检查是否是新开始的块
            PsiElement parent = element.getParent();
            return parent instanceof LuaIfStat ||
                    parent instanceof LuaWhileStat ||
                    parent instanceof LuaForAStat ||
                    parent instanceof LuaForBStat ||
                    parent instanceof LuaRepeatStat ||
                    parent instanceof LuaFuncBody ||
                    parent instanceof LuaDoStat;
        }

        // 对于table，只有在开始新的table时才增加缩进
        if (element instanceof LuaTableExpr) {
            return true;
        }

        return false;
    }

    private @Nullable String calculateTableIndent(PsiElement element, String baseIndent, String indentUnit) {
        IElementType elementType = element.getNode().getElementType();

        // 如果是在左大括号后，增加缩进
        if (elementType == LuaTypes.LCURLY || elementType == LuaTypes.LPAREN) {
            return baseIndent + indentUnit;
        }

        // 如果是逗号或分号，并且它们直接在table内（不在嵌套的function等结构中）
        if (elementType == LuaTypes.COMMA || elementType == LuaTypes.SEMI) {
            if (isDirectTableSeparator(element)) {
                return baseIndent; // 在table field分隔符后保持当前缩进
            }
        }

        return null;
    }

    private boolean isDirectTableSeparator(PsiElement separator) {
        // 向上查找，看是否是直接在table内的分隔符
        PsiElement current = separator.getParent();
        while (current != null) {
            if (current instanceof LuaTableExpr) {
                return true;
            }
            // 如果遇到了这些复杂结构，说明分隔符不是直接在table内
            if (current instanceof LuaFuncBody ||
                    current instanceof LuaIfStat ||
                    current instanceof LuaWhileStat ||
                    current instanceof LuaForAStat ||
                    current instanceof LuaForBStat ||
                    current instanceof LuaRepeatStat ||
                    current instanceof LuaDoStat) {
                return false;
            }
            current = current.getParent();
        }
        return false;
    }

    private String getIndentUnit(Editor editor, Project project) {
        var settings = editor.getSettings();
        if (settings.isUseTabCharacter(project)) {
            return "\t";
        } else {
            int indentSize = settings.getTabSize(project);
            if (indentSize <= 0) {
                indentSize = settings.getTabSize(project);
            }
            return " ".repeat(Math.max(0, indentSize));
        }
    }
}
