// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.tang.intellij.lua.lang.LuaParserDefinitionKt;
import com.tang.intellij.lua.comment.psi.impl.*;

public interface LuaDocTypes {

  IElementType ANY = LuaParserDefinitionKt.createDocType("ANY");
  IElementType TAG_NORMAL = LuaParserDefinitionKt.createDocType("TAG_NORMAL");

  IElementType ARR = new LuaDocTokenType("[]");
  IElementType ASYNC = new LuaDocTokenType("async");
  IElementType AT = new LuaDocTokenType("@");
  IElementType COMMA = new LuaDocTokenType(",");
  IElementType DASHES = new LuaDocTokenType("DASHES");
  IElementType EQ = new LuaDocTokenType("=");
  IElementType EXTENDS = new LuaDocTokenType(":");
  IElementType FUN = new LuaDocTokenType("fun");
  IElementType GT = new LuaDocTokenType(">");
  IElementType ID = new LuaDocTokenType("ID");
  IElementType LBRACK = new LuaDocTokenType("[");
  IElementType LCURLY = new LuaDocTokenType("{");
  IElementType LPAREN = new LuaDocTokenType("(");
  IElementType LT = new LuaDocTokenType("<");
  IElementType NULLABLE = new LuaDocTokenType("?");
  IElementType NUMBER = new LuaDocTokenType("NUMBER");
  IElementType OR = new LuaDocTokenType("|");
  IElementType PACKAGE = new LuaDocTokenType("package");
  IElementType PRIVATE = new LuaDocTokenType("private");
  IElementType PROTECTED = new LuaDocTokenType("protected");
  IElementType PUBLIC = new LuaDocTokenType("public");
  IElementType RBRACK = new LuaDocTokenType("]");
  IElementType RCURLY = new LuaDocTokenType("}");
  IElementType RPAREN = new LuaDocTokenType(")");
  IElementType SHARP = new LuaDocTokenType("#");
  IElementType STRING = new LuaDocTokenType("STRING");
  IElementType STRING_BEGIN = new LuaDocTokenType("STRING_BEGIN");
  IElementType STRING_LITERAL = new LuaDocTokenType("STRING_LITERAL");
  IElementType TAG_NAME = new LuaDocTokenType("TAG_NAME");
  IElementType TAG_NAME_LANGUAGE = new LuaDocTokenType("language");
  IElementType VARARG = new LuaDocTokenType("vararg");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ANY) {
        return new LuaDocAnyImpl(node);
      }
      else if (type == TAG_NORMAL) {
        return new LuaDocTagNormalImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
