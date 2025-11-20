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

package com.tang.intellij.lua.spellchecker

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.tang.intellij.lua.comment.psi.LuaDocTypes
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.psi.LuaTypes

class LuaSpellcheckingStrategy : SpellcheckingStrategy() {
    override fun isMyContext(element: PsiElement): Boolean {
        return element.language is LuaLanguage
    }

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        return when(element.node.elementType){
            LuaDocTypes.STRING -> StringLiteralTokenizer
            LuaDocTypes.WORD -> TEXT_TOKENIZER
            LuaTypes.ID -> TEXT_TOKENIZER
            LuaTypes.STRING -> StringLiteralTokenizer
            else -> super.getTokenizer(element)
        }
    }
}


private object StringLiteralTokenizer : EscapeSequenceTokenizer<PsiElement>() {

    override fun tokenize(element: PsiElement, consumer: TokenConsumer) {
        consumer.consumeToken(element, PlainTextSplitter.getInstance())
    }
}
