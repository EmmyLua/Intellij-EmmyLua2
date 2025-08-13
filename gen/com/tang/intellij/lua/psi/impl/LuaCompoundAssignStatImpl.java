// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.tang.intellij.lua.psi.LuaTypes.*;
import com.tang.intellij.lua.psi.*;

public class LuaCompoundAssignStatImpl extends LuaStatementImpl implements LuaCompoundAssignStat {

  public LuaCompoundAssignStatImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull LuaVisitor visitor) {
    visitor.visitCompoundAssignStat(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaVisitor) accept((LuaVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public LuaCompoundAssignOp getCompoundAssignOp() {
    return findNotNullChildByClass(LuaCompoundAssignOp.class);
  }

  @Override
  @NotNull
  public List<LuaExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, LuaExpr.class);
  }

  @Override
  @NotNull
  public LuaExpr getValueExpr() {
    List<LuaExpr> p1 = getExprList();
    return p1.get(0);
  }

}
