// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class LuaDocVisitor extends PsiElementVisitor {

  public void visitAny(@NotNull LuaDocAny o) {
    visitPsiElement(o);
  }

  public void visitTagNormal(@NotNull LuaDocTagNormal o) {
    visitTag(o);
  }

  public void visitTag(@NotNull LuaDocTag o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull LuaDocPsiElement o) {
    visitElement(o);
  }

}
