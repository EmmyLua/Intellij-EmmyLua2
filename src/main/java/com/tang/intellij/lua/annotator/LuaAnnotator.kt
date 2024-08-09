package com.tang.intellij.lua.annotator


import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.highlighting.LuaHighlightingData
import com.tang.intellij.lua.psi.*

/**
 * LuaAnnotator
 * Created by TangZX on 2016/11/22.
 */
class LuaAnnotator : Annotator {
    private var myHolder: AnnotationHolder? = null
    private val luaVisitor = LuaElementVisitor()
    private val docVisitor = LuaDocElementVisitor()

//    companion object {
//        private val STD_MARKER = Key.create<Boolean>("lua.std.marker")
//        private val UPVALUE = HighlightSeverity("UPVALUE", HighlightSeverity.INFORMATION.myVal + 1)
//    }

    override fun annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder) {
        myHolder = annotationHolder
        if (psiElement is LuaDocPsiElement) {
            psiElement.accept(docVisitor)
        } else if (psiElement is LuaPsiElement) {
            psiElement.accept(luaVisitor)
        }
        myHolder = null
    }

    private inline fun newAnnotation(
        severity: HighlightSeverity,
        psi: PsiElement,
        msg: String?,
        action: (builder: AnnotationBuilder) -> Unit
    ) {
        val builder =
            if (msg == null) myHolder?.newSilentAnnotation(severity) else myHolder?.newAnnotation(severity, msg)
        builder?.apply {
            range(psi)
            action(this)
            create()
        }
    }

    private inline fun newInfoAnnotation(psi: PsiElement, msg: String?, action: (builder: AnnotationBuilder) -> Unit) {
        newAnnotation(HighlightSeverity.INFORMATION, psi, msg, action)
    }

    internal inner class LuaElementVisitor : LuaVisitor() {
        override fun visitLocalDef(o: LuaLocalDef) {
            val nameList = o.nameList
            if (nameList != null) {
                var child: PsiElement? = nameList.firstChild
                while (child != null) {
                    if (child is LuaNameDef) {
                        newInfoAnnotation(child, "Local variable \"${child.name}\"") {
                            it.textAttributes(LuaHighlightingData.LOCAL_VAR)
                        }
                    }
                    child = child.nextSibling
                }
            }
            super.visitLocalDef(o)
        }

        override fun visitTableField(o: LuaTableField) {
            super.visitTableField(o)
            val id = o.id
            if (id != null) {
                newInfoAnnotation(id, null) {
                    it.textAttributes(LuaHighlightingData.FIELD)
                }
            }
        }

        override fun visitClassMethodName(o: LuaClassMethodName) {
            val id = o.id ?: return
            newInfoAnnotation(id, null) {
                if (o.dot != null) {
                    it.textAttributes(LuaHighlightingData.STATIC_METHOD)
                } else {
                    it.textAttributes(LuaHighlightingData.INSTANCE_METHOD)
                }
            }
        }

        override fun visitParamNameDef(o: LuaParamNameDef) {
            newInfoAnnotation(o, "Parameter : \"${o.name}\"") {
                it.textAttributes(LuaHighlightingData.PARAMETER)
            }
        }

        override fun visitIndexExpr(o: LuaIndexExpr) {
            super.visitIndexExpr(o)

            val id = o.id
            if (id != null) {
                if (o.parent is LuaCallExpr) {
                    newInfoAnnotation(id, null) {
                        it.textAttributes(LuaHighlightingData.INSTANCE_METHOD)
                    }
                } else {
                    newInfoAnnotation(id, null) {
                        it.textAttributes(LuaHighlightingData.FIELD)
                    }
                }
            }
        }

        override fun visitCallExpr(o: LuaCallExpr) {
            super.visitCallExpr(o)
            val expr = o.expr
            if (expr is LuaNameExpr) {
                newInfoAnnotation(expr, null) {
                    it.textAttributes(LuaHighlightingData.INSTANCE_METHOD)
                }
            }
            else if(expr is LuaIndexExpr) {
                val id = expr.id
                if (id != null) {
                    newInfoAnnotation(id, null) {
                        it.textAttributes(LuaHighlightingData.INSTANCE_METHOD)
                    }
                }
            }
        }

        override fun visitFuncDef(o: LuaFuncDef) {
            super.visitFuncDef(o)
            val id = o.id
            if (id != null){
                newInfoAnnotation(id, null) {
                    it.textAttributes(LuaHighlightingData.GLOBAL_FUNCTION)
                }
            }
        }

        override fun visitNameExpr(o: LuaNameExpr) {
            super.visitNameExpr(o)
            val name = o.id.text
            if (name != null) {
                if (name == "self") {
                    newInfoAnnotation(o, null) {
                        it.textAttributes(LuaHighlightingData.SELF)
                    }
                }
            }
        }
    }

    internal inner class LuaDocElementVisitor : LuaDocVisitor() {
        override fun visitTagClass(o: LuaDocTagClass) {
            super.visitTagClass(o)
            newInfoAnnotation(o.id, null) {
                it.textAttributes(LuaHighlightingData.CLASS_NAME)
            }
        }

        override fun visitTagAlias(o: LuaDocTagAlias) {
            super.visitTagAlias(o)
            val id = o.id ?: return
            newInfoAnnotation(id, null) {
                it.textAttributes(LuaHighlightingData.TYPE_ALIAS)
            }
        }

        override fun visitClassNameRef(o: LuaDocClassNameRef) {
            newInfoAnnotation(o, null) {
                it.textAttributes(LuaHighlightingData.CLASS_REFERENCE)
            }
        }

        override fun visitParamNameRef(o: LuaDocParamNameRef) {
            newInfoAnnotation(o, null) {
                it.textAttributes(LuaHighlightingData.DOC_COMMENT_TAG_VALUE)
            }
        }
    }
}
