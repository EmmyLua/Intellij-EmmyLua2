package com.tang.intellij.lua.codeInsight

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.tang.intellij.lua.comment.psi.LuaDocClassNameRef
import com.tang.intellij.lua.comment.psi.LuaDocParamNameRef
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.LuaExpr
import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.psi.LuaNameDef
import com.tang.intellij.lua.psi.LuaPsiElement


class LuaReadWriteAccessDetector : ReadWriteAccessDetector() {
    override fun getReferenceAccess(element: PsiElement, reference: PsiReference): Access {
        return getExpressionAccess(reference.element)
    }

    override fun isReadWriteAccessible(element: PsiElement): Boolean {
        return element is LuaPsiElement
    }

    override fun getExpressionAccess(element: PsiElement): Access {
        return when (element) {
            is LuaNameDef -> Access.Write
            is LuaExpr -> Access.Read
            is LuaDocTagClass -> Access.Write
            is LuaDocClassNameRef -> Access.Read
            is LuaDocParamNameRef -> Access.Read
            is LuaDocTagField -> Access.Write
            else -> return Access.ReadWrite
        }
    }

    override fun isDeclarationWriteAccess(element: PsiElement): Boolean {
        return element is LuaPsiElement
    }
}