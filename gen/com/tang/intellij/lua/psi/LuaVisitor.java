// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;

public class LuaVisitor extends PsiElementVisitor {

  public void visitArgs(@NotNull LuaArgs o) {
    visitPsiElement(o);
  }

  public void visitAssignStat(@NotNull LuaAssignStat o) {
    visitStatement(o);
  }

  public void visitAttribute(@NotNull LuaAttribute o) {
    visitPsiElement(o);
  }

  public void visitBinaryExpr(@NotNull LuaBinaryExpr o) {
    visitExpr(o);
  }

  public void visitBinaryOp(@NotNull LuaBinaryOp o) {
    visitPsiElement(o);
  }

  public void visitBlock(@NotNull LuaBlock o) {
  }

  public void visitBreakStat(@NotNull LuaBreakStat o) {
    visitStatement(o);
  }

  public void visitCallExpr(@NotNull LuaCallExpr o) {
    visitExpr(o);
  }

  public void visitClassMethodDef(@NotNull LuaClassMethodDef o) {
    visitPsiElement(o);
  }

  public void visitClassMethodName(@NotNull LuaClassMethodName o) {
    visitPsiElement(o);
  }

  public void visitClosureExpr(@NotNull LuaClosureExpr o) {
    visitExpr(o);
  }

  public void visitDoStat(@NotNull LuaDoStat o) {
    visitStatement(o);
  }

  public void visitEmptyStat(@NotNull LuaEmptyStat o) {
    visitStatement(o);
  }

  public void visitExpr(@NotNull LuaExpr o) {
    visitPsiElement(o);
  }

  public void visitExprList(@NotNull LuaExprList o) {
    visitPsiElement(o);
  }

  public void visitExprStat(@NotNull LuaExprStat o) {
    visitStatement(o);
  }

  public void visitForAStat(@NotNull LuaForAStat o) {
    visitStatement(o);
  }

  public void visitForBStat(@NotNull LuaForBStat o) {
    visitStatement(o);
  }

  public void visitFuncBody(@NotNull LuaFuncBody o) {
    visitPsiElement(o);
  }

  public void visitFuncDef(@NotNull LuaFuncDef o) {
    visitPsiElement(o);
  }

  public void visitGotoStat(@NotNull LuaGotoStat o) {
    visitStatement(o);
  }

  public void visitIfStat(@NotNull LuaIfStat o) {
    visitStatement(o);
  }

  public void visitIndexExpr(@NotNull LuaIndexExpr o) {
    visitExpr(o);
  }

  public void visitLabelStat(@NotNull LuaLabelStat o) {
    visitStatement(o);
  }

  public void visitListArgs(@NotNull LuaListArgs o) {
    visitArgs(o);
  }

  public void visitLiteralExpr(@NotNull LuaLiteralExpr o) {
    visitExpr(o);
  }

  public void visitLocalDef(@NotNull LuaLocalDef o) {
    visitPsiElement(o);
  }

  public void visitLocalFuncDef(@NotNull LuaLocalFuncDef o) {
    visitPsiElement(o);
  }

  public void visitNameDef(@NotNull LuaNameDef o) {
    visitPsiElement(o);
  }

  public void visitNameExpr(@NotNull LuaNameExpr o) {
    visitExpr(o);
  }

  public void visitNameList(@NotNull LuaNameList o) {
    visitPsiElement(o);
  }

  public void visitParamNameDef(@NotNull LuaParamNameDef o) {
    visitNameDef(o);
  }

  public void visitParenExpr(@NotNull LuaParenExpr o) {
    visitExpr(o);
  }

  public void visitRepeatStat(@NotNull LuaRepeatStat o) {
    visitStatement(o);
  }

  public void visitReturnStat(@NotNull LuaReturnStat o) {
    visitStatement(o);
  }

  public void visitShebangLine(@NotNull LuaShebangLine o) {
    visitPsiElement(o);
  }

  public void visitSingleArg(@NotNull LuaSingleArg o) {
    visitArgs(o);
  }

  public void visitStatement(@NotNull LuaStatement o) {
    visitPsiElement(o);
  }

  public void visitTableExpr(@NotNull LuaTableExpr o) {
    visitExpr(o);
  }

  public void visitTableField(@NotNull LuaTableField o) {
    visitPsiElement(o);
  }

  public void visitTableFieldSep(@NotNull LuaTableFieldSep o) {
    visitPsiElement(o);
  }

  public void visitUnaryExpr(@NotNull LuaUnaryExpr o) {
    visitExpr(o);
  }

  public void visitUnaryOp(@NotNull LuaUnaryOp o) {
    visitPsiElement(o);
  }

  public void visitVarList(@NotNull LuaVarList o) {
    visitExprList(o);
  }

  public void visitWhileStat(@NotNull LuaWhileStat o) {
    visitStatement(o);
  }

  public void visitPsiElement(@NotNull LuaPsiElement o) {
    visitElement(o);
  }

}
