package com.tang.intellij.lua.braces

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.psi.LuaTypes


/**
 * Created by tangzx
 * Date : 2015/11/16.
 */
class LuaBraceMatcher : PairedBraceMatcher {


    override fun getPairs(): Array<BracePair> {
        return PAIRS
    }

    override fun isPairedBracesAllowedBeforeType(iElementType: IElementType, iElementType1: IElementType?): Boolean {
        return true
    }

    override fun getCodeConstructStart(psiFile: PsiFile, i: Int): Int {
        return i
    }

    companion object {
        private val PAIRS = arrayOf(
            BracePair(LuaTypes.LCURLY, LuaTypes.RCURLY, true), //{}
            BracePair(LuaTypes.LPAREN, LuaTypes.RPAREN, true), //()
            BracePair(LuaTypes.LBRACK, LuaTypes.RBRACK, true), //[]
            BracePair(LuaTypes.DO, LuaTypes.END, true), //do end
            BracePair(LuaTypes.IF, LuaTypes.END, true), //if end
            BracePair(LuaTypes.REPEAT, LuaTypes.UNTIL, true), //repeat until
            BracePair(LuaTypes.FUNCTION, LuaTypes.END, true)    //function end
        )
    }
}