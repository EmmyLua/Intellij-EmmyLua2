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

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Handles Enter key press for Lua files, especially for 'end' keyword indentation
 */
public class LuaEnterHandlerDelegate extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(@NotNull PsiFile file,
                                  @NotNull Editor editor,
                                  @NotNull Ref<Integer> caretOffset,
                                  @NotNull Ref<Integer> caretAdvance,
                                  @NotNull DataContext dataContext,
                                  EditorActionHandler originalHandler) {
        if (!file.getLanguage().is(LuaLanguage.INSTANCE)) {
            return Result.Continue;
        }

        int offset = caretOffset.get();
        if (offset <= 0) {
            return Result.Continue;
        }

        Document document = editor.getDocument();
        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(file.getProject());
        psiDocManager.commitDocument(document);

        // 获取光标前的元素
        PsiElement element = file.findElementAt(offset - 1);
        if (element == null) {
            return Result.Continue;
        }

        // 检查是否在 'end' 或 'until' 关键字之后按 Enter
        if (isAfterEndKeyword(element, document, offset)) {
            // 找到对应的开始关键字的缩进
            String targetIndent = findMatchingBlockIndent(element);
            if (targetIndent != null) {
                int currentLine = document.getLineNumber(offset);
                int lineStartOffset = document.getLineStartOffset(currentLine);
                int lineEndOffset = document.getLineEndOffset(currentLine);

                // 获取当前行内容
                String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset));

                // 提取当前行的缩进和关键字后面的内容
                String keyword = lineText.contains("end") ? "end" : "until";
                int keywordPos = lineText.indexOf(keyword);
                if (keywordPos >= 0) {
                    String afterKeyword = lineText.substring(keywordPos + keyword.length()).trim();

                    // 替换整行为正确缩进的关键字
                    document.replaceString(lineStartOffset, lineEndOffset,
                            targetIndent + keyword + (afterKeyword.isEmpty() ? "" : " " + afterKeyword));

                    // 更新光标位置到关键字之后
                    caretOffset.set(lineStartOffset + targetIndent.length() + keyword.length() +
                            (afterKeyword.isEmpty() ? 0 : 1 + afterKeyword.length()));
                }
            }
        }

        return Result.Continue;
    }

    /**
     * 检查光标是否在 'end' 或 'until' 关键字之后
     */
    private boolean isAfterEndKeyword(PsiElement element, Document document, int offset) {
        // 简单检查:查看当前行文本是否以 'end' 或 'until' 结尾
        int currentLine = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(currentLine);
        String lineText = document.getText(new TextRange(lineStartOffset, offset)).trim();

        return lineText.equals("end") || lineText.equals("until");
    }

    /**
     * 查找与 'end' 或 'until' 匹配的父级语句的缩进
     * 对于 local f = function() ... end, end 应该对齐到 local 而不是 function
     */
    private String findMatchingBlockIndent(PsiElement endElement) {
        // 向上遍历 PSI 树，找到包含此 end 的块结构
        PsiElement current = endElement.getParent();
        int maxIterations = 50; // 防止无限循环
        int iterations = 0;

        while (current != null && iterations++ < maxIterations) {
            // 找到块结构
            if (current instanceof LuaStatement ||
                    current instanceof LuaFuncDef ||
                    current instanceof LuaLocalFuncDef ||
                    current instanceof LuaClassMethodDef ||
                    current instanceof LuaLocalDef
            ) {

                // 获取包含该块的最外层语句
                PsiElement statement = current;
                PsiElement parent = current.getParent();

                // 向上查找,找到最顶层的语句 (例如 LuaLocalDef 包含 LuaLocalFuncDef)
                while (parent != null && !(parent instanceof LuaPsiFile) && !(parent instanceof LuaBlock)) {
                    if (parent instanceof LuaExprStat ||
                            parent instanceof LuaLocalDef ||
                            parent instanceof LuaAssignStat) {
                        statement = parent;
                    }
                    parent = parent.getParent();
                }

                // 获取语句的缩进
                int startOffset = statement.getTextRange().getStartOffset();
                Document document = PsiDocumentManager.getInstance(current.getProject())
                        .getDocument(current.getContainingFile());
                if (document != null) {
                    int lineNumber = document.getLineNumber(startOffset);
                    int lineStartOffset = document.getLineStartOffset(lineNumber);
                    String lineText = document.getText(new TextRange(lineStartOffset, startOffset));

                    // 提取缩进（空格和制表符）
                    StringBuilder indent = new StringBuilder();
                    for (char c : lineText.toCharArray()) {
                        if (c == ' ' || c == '\t') {
                            indent.append(c);
                        } else {
                            break;
                        }
                    }
                    return indent.toString();
                }
            }

            current = current.getParent();
        }

        return null;
    }
}
