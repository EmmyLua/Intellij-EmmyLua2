// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.tang.intellij.lua.lang.LuaParserDefinitionKt;
import com.tang.intellij.lua.psi.impl.*;

public interface LuaTypes {

  IElementType ARGS = LuaParserDefinitionKt.createType("ARGS");
  IElementType ASSIGN_OP = LuaParserDefinitionKt.createType("ASSIGN_OP");
  IElementType ASSIGN_STAT = LuaParserDefinitionKt.createType("ASSIGN_STAT");
  IElementType ATTRIBUTE = LuaParserDefinitionKt.createType("ATTRIBUTE");
  IElementType BINARY_EXPR = LuaParserDefinitionKt.createType("BINARY_EXPR");
  IElementType BINARY_OP = LuaParserDefinitionKt.createType("BINARY_OP");
  IElementType BLOCK = LuaParserDefinitionKt.createType("BLOCK");
  IElementType BREAK_STAT = LuaParserDefinitionKt.createType("BREAK_STAT");
  IElementType CALL_EXPR = LuaParserDefinitionKt.createType("CALL_EXPR");
  IElementType CLASS_METHOD_DEF = LuaParserDefinitionKt.createType("CLASS_METHOD_DEF");
  IElementType CLASS_METHOD_NAME = LuaParserDefinitionKt.createType("CLASS_METHOD_NAME");
  IElementType CLOSURE_EXPR = LuaParserDefinitionKt.createType("CLOSURE_EXPR");
  IElementType COMPOUND_ASSIGN_OP = LuaParserDefinitionKt.createType("COMPOUND_ASSIGN_OP");
  IElementType COMPOUND_ASSIGN_STAT = LuaParserDefinitionKt.createType("COMPOUND_ASSIGN_STAT");
  IElementType CONTINUE_STAT = LuaParserDefinitionKt.createType("CONTINUE_STAT");
  IElementType DO_STAT = LuaParserDefinitionKt.createType("DO_STAT");
  IElementType ELSEIF_CLAUSE = LuaParserDefinitionKt.createType("ELSEIF_CLAUSE");
  IElementType ELSE_CLAUSE = LuaParserDefinitionKt.createType("ELSE_CLAUSE");
  IElementType EMPTY_STAT = LuaParserDefinitionKt.createType("EMPTY_STAT");
  IElementType EXPR = LuaParserDefinitionKt.createType("EXPR");
  IElementType EXPR_LIST = LuaParserDefinitionKt.createType("EXPR_LIST");
  IElementType EXPR_STAT = LuaParserDefinitionKt.createType("EXPR_STAT");
  IElementType FOR_A_STAT = LuaParserDefinitionKt.createType("FOR_A_STAT");
  IElementType FOR_B_STAT = LuaParserDefinitionKt.createType("FOR_B_STAT");
  IElementType FUNC_BODY = LuaParserDefinitionKt.createType("FUNC_BODY");
  IElementType FUNC_DEF = LuaParserDefinitionKt.createType("FUNC_DEF");
  IElementType GOTO_STAT = LuaParserDefinitionKt.createType("GOTO_STAT");
  IElementType IF_STAT = LuaParserDefinitionKt.createType("IF_STAT");
  IElementType INDEX_EXPR = LuaParserDefinitionKt.createType("INDEX_EXPR");
  IElementType LABEL_STAT = LuaParserDefinitionKt.createType("LABEL_STAT");
  IElementType LIST_ARGS = LuaParserDefinitionKt.createType("LIST_ARGS");
  IElementType LITERAL_EXPR = LuaParserDefinitionKt.createType("LITERAL_EXPR");
  IElementType LOCAL_DEF = LuaParserDefinitionKt.createType("LOCAL_DEF");
  IElementType LOCAL_FUNC_DEF = LuaParserDefinitionKt.createType("LOCAL_FUNC_DEF");
  IElementType NAME_DEF = LuaParserDefinitionKt.createType("NAME_DEF");
  IElementType NAME_EXPR = LuaParserDefinitionKt.createType("NAME_EXPR");
  IElementType NAME_LIST = LuaParserDefinitionKt.createType("NAME_LIST");
  IElementType PARAM_NAME_DEF = LuaParserDefinitionKt.createType("PARAM_NAME_DEF");
  IElementType PAREN_EXPR = LuaParserDefinitionKt.createType("PAREN_EXPR");
  IElementType PRIMARY_EXPR = LuaParserDefinitionKt.createType("PRIMARY_EXPR");
  IElementType REPEAT_STAT = LuaParserDefinitionKt.createType("REPEAT_STAT");
  IElementType RETURN_STAT = LuaParserDefinitionKt.createType("RETURN_STAT");
  IElementType SHEBANG_LINE = LuaParserDefinitionKt.createType("SHEBANG_LINE");
  IElementType SINGLE_ARG = LuaParserDefinitionKt.createType("SINGLE_ARG");
  IElementType TABLE_EXPR = LuaParserDefinitionKt.createType("TABLE_EXPR");
  IElementType TABLE_FIELD = LuaParserDefinitionKt.createType("TABLE_FIELD");
  IElementType TABLE_FIELD_SEP = LuaParserDefinitionKt.createType("TABLE_FIELD_SEP");
  IElementType UNARY_EXPR = LuaParserDefinitionKt.createType("UNARY_EXPR");
  IElementType UNARY_OP = LuaParserDefinitionKt.createType("UNARY_OP");
  IElementType VAR_LIST = LuaParserDefinitionKt.createType("VAR_LIST");
  IElementType WHILE_STAT = LuaParserDefinitionKt.createType("WHILE_STAT");

  IElementType AND = LuaParserDefinitionKt.createToken("and");
  IElementType ASSIGN = LuaParserDefinitionKt.createToken("=");
  IElementType BACKTICK = LuaParserDefinitionKt.createToken("`");
  IElementType BIT_AND = LuaParserDefinitionKt.createToken("&");
  IElementType BIT_AND_ASSIGN = LuaParserDefinitionKt.createToken("&=");
  IElementType BIT_LTLT = LuaParserDefinitionKt.createToken("<<");
  IElementType BIT_LTLT_ASSIGN = LuaParserDefinitionKt.createToken("<<=");
  IElementType BIT_OR = LuaParserDefinitionKt.createToken("|");
  IElementType BIT_OR_ASSIGN = LuaParserDefinitionKt.createToken("|=");
  IElementType BIT_RTRT = LuaParserDefinitionKt.createToken(">>");
  IElementType BIT_RTRT_ASSIGN = LuaParserDefinitionKt.createToken(">>=");
  IElementType BIT_TILDE = LuaParserDefinitionKt.createToken("~");
  IElementType BLOCK_COMMENT = LuaParserDefinitionKt.createToken("BLOCK_COMMENT");
  IElementType BREAK = LuaParserDefinitionKt.createToken("break");
  IElementType COLON = LuaParserDefinitionKt.createToken(":");
  IElementType COMMA = LuaParserDefinitionKt.createToken(",");
  IElementType CONCAT = LuaParserDefinitionKt.createToken("..");
  IElementType CONTINUE = LuaParserDefinitionKt.createToken("continue");
  IElementType DIV = LuaParserDefinitionKt.createToken("/");
  IElementType DIV_ASSIGN = LuaParserDefinitionKt.createToken("/=");
  IElementType DO = LuaParserDefinitionKt.createToken("do");
  IElementType DOC_COMMENT = LuaParserDefinitionKt.createToken("DOC_COMMENT");
  IElementType DOT = LuaParserDefinitionKt.createToken(".");
  IElementType DOUBLE_COLON = LuaParserDefinitionKt.createToken("::");
  IElementType DOUBLE_DIV = LuaParserDefinitionKt.createToken("//");
  IElementType DOUBLE_DIV_ASSIGN = LuaParserDefinitionKt.createToken("//=");
  IElementType ELLIPSIS = LuaParserDefinitionKt.createToken("...");
  IElementType ELSE = LuaParserDefinitionKt.createToken("else");
  IElementType ELSEIF = LuaParserDefinitionKt.createToken("elseif");
  IElementType END = LuaParserDefinitionKt.createToken("end");
  IElementType ENDREGION = LuaParserDefinitionKt.createToken("ENDREGION");
  IElementType EQ = LuaParserDefinitionKt.createToken("==");
  IElementType EXP = LuaParserDefinitionKt.createToken("^");
  IElementType EXP_ASSIGN = LuaParserDefinitionKt.createToken("^=");
  IElementType FALSE = LuaParserDefinitionKt.createToken("false");
  IElementType FOR = LuaParserDefinitionKt.createToken("for");
  IElementType FUNCTION = LuaParserDefinitionKt.createToken("function");
  IElementType GE = LuaParserDefinitionKt.createToken(">=");
  IElementType GETN = LuaParserDefinitionKt.createToken("#");
  IElementType GOTO = LuaParserDefinitionKt.createToken("goto");
  IElementType GT = LuaParserDefinitionKt.createToken(">");
  IElementType ID = LuaParserDefinitionKt.createToken("ID");
  IElementType IF = LuaParserDefinitionKt.createToken("if");
  IElementType IN = LuaParserDefinitionKt.createToken("in");
  IElementType LBRACK = LuaParserDefinitionKt.createToken("[");
  IElementType LCURLY = LuaParserDefinitionKt.createToken("{");
  IElementType LE = LuaParserDefinitionKt.createToken("<=");
  IElementType LOCAL = LuaParserDefinitionKt.createToken("local");
  IElementType LOGICAL_AND = LuaParserDefinitionKt.createToken("&&");
  IElementType LOGICAL_NOT = LuaParserDefinitionKt.createToken("!");
  IElementType LOGICAL_OR = LuaParserDefinitionKt.createToken("||");
  IElementType LPAREN = LuaParserDefinitionKt.createToken("(");
  IElementType LT = LuaParserDefinitionKt.createToken("<");
  IElementType MINUS = LuaParserDefinitionKt.createToken("-");
  IElementType MINUS_ASSIGN = LuaParserDefinitionKt.createToken("-=");
  IElementType MOD = LuaParserDefinitionKt.createToken("%");
  IElementType MOD_ASSIGN = LuaParserDefinitionKt.createToken("%=");
  IElementType MULT = LuaParserDefinitionKt.createToken("*");
  IElementType MULT_ASSIGN = LuaParserDefinitionKt.createToken("*=");
  IElementType NE = LuaParserDefinitionKt.createToken("~=");
  IElementType NIL = LuaParserDefinitionKt.createToken("nil");
  IElementType NOT = LuaParserDefinitionKt.createToken("not");
  IElementType NOT_EQ = LuaParserDefinitionKt.createToken("!=");
  IElementType NUMBER = LuaParserDefinitionKt.createToken("NUMBER");
  IElementType OR = LuaParserDefinitionKt.createToken("or");
  IElementType PLUS = LuaParserDefinitionKt.createToken("+");
  IElementType PLUS_ASSIGN = LuaParserDefinitionKt.createToken("+=");
  IElementType RBRACK = LuaParserDefinitionKt.createToken("]");
  IElementType RCURLY = LuaParserDefinitionKt.createToken("}");
  IElementType REGION = LuaParserDefinitionKt.createToken("REGION");
  IElementType REPEAT = LuaParserDefinitionKt.createToken("repeat");
  IElementType RETURN = LuaParserDefinitionKt.createToken("return");
  IElementType RPAREN = LuaParserDefinitionKt.createToken(")");
  IElementType SEMI = LuaParserDefinitionKt.createToken(";");
  IElementType SHEBANG = LuaParserDefinitionKt.createToken("#!");
  IElementType SHEBANG_CONTENT = LuaParserDefinitionKt.createToken("SHEBANG_CONTENT");
  IElementType SHORT_COMMENT = LuaParserDefinitionKt.createToken("SHORT_COMMENT");
  IElementType STRING = LuaParserDefinitionKt.createToken("STRING");
  IElementType THEN = LuaParserDefinitionKt.createToken("then");
  IElementType TRUE = LuaParserDefinitionKt.createToken("true");
  IElementType UNTIL = LuaParserDefinitionKt.createToken("until");
  IElementType WHILE = LuaParserDefinitionKt.createToken("while");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ASSIGN_OP) {
        return new LuaAssignOpImpl(node);
      }
      else if (type == ASSIGN_STAT) {
        return new LuaAssignStatImpl(node);
      }
      else if (type == ATTRIBUTE) {
        return new LuaAttributeImpl(node);
      }
      else if (type == BINARY_EXPR) {
        return new LuaBinaryExprImpl(node);
      }
      else if (type == BINARY_OP) {
        return new LuaBinaryOpImpl(node);
      }
      else if (type == BLOCK) {
        return new LuaBlockImpl(node);
      }
      else if (type == BREAK_STAT) {
        return new LuaBreakStatImpl(node);
      }
      else if (type == CALL_EXPR) {
        return new LuaCallExprImpl(node);
      }
      else if (type == CLASS_METHOD_DEF) {
        return new LuaClassMethodDefImpl(node);
      }
      else if (type == CLASS_METHOD_NAME) {
        return new LuaClassMethodNameImpl(node);
      }
      else if (type == CLOSURE_EXPR) {
        return new LuaClosureExprImpl(node);
      }
      else if (type == COMPOUND_ASSIGN_OP) {
        return new LuaCompoundAssignOpImpl(node);
      }
      else if (type == COMPOUND_ASSIGN_STAT) {
        return new LuaCompoundAssignStatImpl(node);
      }
      else if (type == CONTINUE_STAT) {
        return new LuaContinueStatImpl(node);
      }
      else if (type == DO_STAT) {
        return new LuaDoStatImpl(node);
      }
      else if (type == ELSEIF_CLAUSE) {
        return new LuaElseifClauseImpl(node);
      }
      else if (type == ELSE_CLAUSE) {
        return new LuaElseClauseImpl(node);
      }
      else if (type == EMPTY_STAT) {
        return new LuaEmptyStatImpl(node);
      }
      else if (type == EXPR) {
        return new LuaExprImpl(node);
      }
      else if (type == EXPR_LIST) {
        return new LuaExprListImpl(node);
      }
      else if (type == EXPR_STAT) {
        return new LuaExprStatImpl(node);
      }
      else if (type == FOR_A_STAT) {
        return new LuaForAStatImpl(node);
      }
      else if (type == FOR_B_STAT) {
        return new LuaForBStatImpl(node);
      }
      else if (type == FUNC_BODY) {
        return new LuaFuncBodyImpl(node);
      }
      else if (type == FUNC_DEF) {
        return new LuaFuncDefImpl(node);
      }
      else if (type == GOTO_STAT) {
        return new LuaGotoStatImpl(node);
      }
      else if (type == IF_STAT) {
        return new LuaIfStatImpl(node);
      }
      else if (type == INDEX_EXPR) {
        return new LuaIndexExprImpl(node);
      }
      else if (type == LABEL_STAT) {
        return new LuaLabelStatImpl(node);
      }
      else if (type == LIST_ARGS) {
        return new LuaListArgsImpl(node);
      }
      else if (type == LITERAL_EXPR) {
        return new LuaLiteralExprImpl(node);
      }
      else if (type == LOCAL_DEF) {
        return new LuaLocalDefImpl(node);
      }
      else if (type == LOCAL_FUNC_DEF) {
        return new LuaLocalFuncDefImpl(node);
      }
      else if (type == NAME_DEF) {
        return new LuaNameDefImpl(node);
      }
      else if (type == NAME_EXPR) {
        return new LuaNameExprImpl(node);
      }
      else if (type == NAME_LIST) {
        return new LuaNameListImpl(node);
      }
      else if (type == PARAM_NAME_DEF) {
        return new LuaParamNameDefImpl(node);
      }
      else if (type == PAREN_EXPR) {
        return new LuaParenExprImpl(node);
      }
      else if (type == REPEAT_STAT) {
        return new LuaRepeatStatImpl(node);
      }
      else if (type == RETURN_STAT) {
        return new LuaReturnStatImpl(node);
      }
      else if (type == SHEBANG_LINE) {
        return new LuaShebangLineImpl(node);
      }
      else if (type == SINGLE_ARG) {
        return new LuaSingleArgImpl(node);
      }
      else if (type == TABLE_EXPR) {
        return new LuaTableExprImpl(node);
      }
      else if (type == TABLE_FIELD) {
        return new LuaTableFieldImpl(node);
      }
      else if (type == TABLE_FIELD_SEP) {
        return new LuaTableFieldSepImpl(node);
      }
      else if (type == UNARY_EXPR) {
        return new LuaUnaryExprImpl(node);
      }
      else if (type == UNARY_OP) {
        return new LuaUnaryOpImpl(node);
      }
      else if (type == VAR_LIST) {
        return new LuaVarListImpl(node);
      }
      else if (type == WHILE_STAT) {
        return new LuaWhileStatImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
