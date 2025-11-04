package com.tang.intellij.lua.codeInsight.highlighting

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.Consumer
import com.tang.intellij.lua.psi.*

/**

 * Created by tangzx on 2017/3/18.
 */
class LuaHighlightExitPointsHandler internal constructor(
    editor: Editor,
    file: PsiFile,
    private val target: LuaReturnStat,
    private val funcBody: LuaFuncBody
) : HighlightUsagesHandlerBase<PsiElement>(editor, file) {

    override fun getTargets(): List<PsiElement> {
        return listOf<PsiElement>(target)
    }

    override fun selectTargets(list: List<PsiElement>, consumer: Consumer<in List<PsiElement>>) {
    }

    override fun computeUsages(list: List<PsiElement>) {
        funcBody.acceptChildren(object : LuaVisitor() {
            override fun visitReturnStat(o: LuaReturnStat) {
                addOccurrence(o)
            }

            override fun visitClosureExpr(o: LuaClosureExpr) {

            }

            override fun visitPsiElement(o: LuaPsiElement) {
                o.acceptChildren(this)
            }
        })
    }
}