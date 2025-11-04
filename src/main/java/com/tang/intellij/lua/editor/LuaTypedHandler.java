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

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Handles typed characters for Lua files, especially for 'end' keyword auto-alignment
 */
public class LuaTypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!file.getLanguage().is(LuaLanguage.INSTANCE)) {
            return Result.CONTINUE;
        }

        // 处理输入 'd' (end 的最后一个字符) 或 'l' (until 的最后一个字符)
        if (c != 'd' && c != 'l') {
            return Result.CONTINUE;
        }

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();

        if (caretOffset < 3) {
            return Result.CONTINUE;
        }

        // 提交文档以更新 PSI
        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.commitDocument(document);

        // 检查是否刚刚输入了 "end" 或 "until"
        int currentLine = document.getLineNumber(caretOffset);
        int lineStartOffset = document.getLineStartOffset(currentLine);
        String lineText = document.getText(new TextRange(lineStartOffset, caretOffset));

        // 检查当前行是否以 "end" 或 "until" 结尾（可能前面有空格）
        String trimmedLine = lineText.trim();
        String keyword = null;
        if (trimmedLine.equals("end")) {
            keyword = "end";
        } else if (trimmedLine.equals("until")) {
            keyword = "until";
        }

        if (keyword == null) {
            return Result.CONTINUE;
        }

        // 获取光标前的元素
        PsiElement element = file.findElementAt(caretOffset - 1);
        if (element == null) {
            return Result.CONTINUE;
        }

        // 查找匹配的块结构的缩进
        String targetIndent = findMatchingBlockIndent(element);
        if (targetIndent != null) {
            // 获取当前缩进
            String currentIndent = lineText.substring(0, lineText.indexOf(keyword));

            // 如果缩进不匹配，则调整
            if (!currentIndent.equals(targetIndent)) {
                // 替换当前行的缩进
                int lineEndOffset = document.getLineEndOffset(currentLine);
                String afterKeyword = document.getText(new TextRange(caretOffset, lineEndOffset));

                document.replaceString(lineStartOffset, lineEndOffset, targetIndent + keyword + afterKeyword);

                // 调整光标位置
                editor.getCaretModel().moveToOffset(lineStartOffset + targetIndent.length() + keyword.length());
            }
        }

        return Result.CONTINUE;
    }

    /**
     * 查找与 'end' 或 'until' 匹配的父级语句的缩进
     * 对于 local f = function() ... end, end 应该对齐到 local 而不是 function
     */
    private String findMatchingBlockIndent(PsiElement endElement) {
        // 向上遍历 PSI 树，找到包含此 end/until 的块结构
        PsiElement current = endElement.getParent();
        int maxIterations = 50; // 防止无限循环
        int iterations = 0;

        while (current != null && iterations++ < maxIterations) {
            if (current instanceof LuaDoStat ||
                    current instanceof LuaIfStat ||
                    current instanceof LuaWhileStat ||
                    current instanceof LuaForAStat ||
                    current instanceof LuaForBStat ||
                    current instanceof LuaRepeatStat ||
                    current instanceof LuaFuncDef ||
                    current instanceof LuaLocalFuncDef ||
                    current instanceof LuaClassMethodDef) {

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
