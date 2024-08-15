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

package com.tang.intellij.lua.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.CustomParsingType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.IReparseableElementType;
import com.intellij.util.CharTable;
import com.tang.intellij.lua.comment.lexer.LuaDocLexerAdapter;
import com.tang.intellij.lua.comment.parser.LuaDocParser;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.lang.LuaParserDefinition;
import com.tang.intellij.lua.lexer.LuaLexerAdapter;
import com.tang.intellij.lua.parser.LuaParser;
//import com.tang.intellij.lua.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by tangzx on 2015/11/15.
 * Email:love.tangzx@qq.com
 */
public class LuaElementType extends IElementType {
    public LuaElementType(String debugName) {
        super(debugName, LuaLanguage.INSTANCE);
    }

    public static CustomParsingType DOC_COMMENT =  new CustomParsingType ("DOC_COMMENT", LuaLanguage.INSTANCE) {
        @NotNull
        @Override
        public ASTNode parse(@NotNull CharSequence charSequence, @NotNull CharTable charTable) {
            PsiParser parser = new LuaDocParser();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
                    new LuaParserDefinition(),
                    new LuaDocLexerAdapter(),
                    charSequence);
            return parser.parse(this, builder);
        }
    };

    public static LuaElementType FUNC_DEF = new LuaElementType("FUNC_DEF");
    public static LuaElementType CLASS_METHOD_DEF = new LuaElementType("CLASS_METHOD_DEF ");
    public static LuaElementType CLASS_FIELD_DEF = new LuaElementType("CLASS_FIELD_DEF");
    public static LuaElementType TYPE_DEF = new LuaElementType("TYPE_DEF");
    public static LuaElementType CLASS_DEF = new LuaElementType("CLASS_DEF");
    public static LuaElementType DOC_TABLE_DEF = new LuaElementType("DOC_TABLE_DEF");
    public static LuaElementType DOC_TABLE_FIELD_DEF = new LuaElementType("DOC_TABLE_FIELD_DEF");
    public static LuaElementType DOC_ALIAS = new LuaElementType("DOC_ALIAS");
    public static LuaElementType TABLE = new LuaElementType("TABLE");
    public static LuaElementType TABLE_FIELD = new LuaElementType("TABLE_FIELD");
    public static LuaElementType INDEX = new LuaElementType("INDEX");
    public static LuaElementType NAME_EXPR = new LuaElementType("NAME_EXPR");
    public static ILazyParseableElementType BLOCK = new LuaBlockElementType();

    static class LuaBlockElementType extends IReparseableElementType {

        LuaBlockElementType() {
            super("LuaBlock", LuaLanguage.INSTANCE);
        }

        @Override
        public ASTNode parseContents(@NotNull ASTNode chameleon) {
            Project project = chameleon.getPsi().getProject();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
                    project,
                    chameleon,
                    new LuaLexerAdapter(),
                    LuaLanguage.INSTANCE,
                    chameleon.getText());
            PsiParser luaParser = new LuaParser();
            return luaParser.parse(this, builder).getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return null;
        }
    }

    public static LuaElementType NAME_DEF = new LuaElementType("NAME_DEF");
    public static LuaElementType PARAM_NAME_DEF = new LuaElementType("PARAM_NAME_DEF");
    public static LuaElementType LITERAL_EXPR = new LuaElementType("LITERAL_EXPR");
}
