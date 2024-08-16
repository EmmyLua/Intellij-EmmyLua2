// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.tang.intellij.lua.comment.psi.LuaDocTypes.*;
import static com.tang.intellij.lua.psi.LuaParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class LuaDocParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return doc(b, l + 1);
  }

  /* ********************************************************** */
  // doc_item | STRING
  static boolean after_dash(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "after_dash")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = doc_item(b, l + 1);
    if (!r) r = consumeToken(b, STRING);
    exit_section_(b, l, m, r, false, LuaDocParser::after_dash_recover);
    return r;
  }

  /* ********************************************************** */
  // !(DASHES)
  static boolean after_dash_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "after_dash_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, DASHES);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ID | STRING | NUMBER | AT | SHARP | EQ | COMMA | EXTENDS | OR | GT | LT | LPAREN | RPAREN | LCURLY | RCURLY
  // | STRING_LITERAL | ARR | FUN | VARARG | PRIVATE | PROTECTED | PUBLIC | DASHES | STRING_BEGIN | TAG_NAME |
  // TAG_NAME_LANGUAGE | ASYNC | NULLABLE | LBRACK | RBRACK | PACKAGE
  public static boolean any(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "any")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ANY, "<any>");
    r = consumeToken(b, ID);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, AT);
    if (!r) r = consumeToken(b, SHARP);
    if (!r) r = consumeToken(b, EQ);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, EXTENDS);
    if (!r) r = consumeToken(b, OR);
    if (!r) r = consumeToken(b, GT);
    if (!r) r = consumeToken(b, LT);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, LCURLY);
    if (!r) r = consumeToken(b, RCURLY);
    if (!r) r = consumeToken(b, STRING_LITERAL);
    if (!r) r = consumeToken(b, ARR);
    if (!r) r = consumeToken(b, FUN);
    if (!r) r = consumeToken(b, VARARG);
    if (!r) r = consumeToken(b, PRIVATE);
    if (!r) r = consumeToken(b, PROTECTED);
    if (!r) r = consumeToken(b, PUBLIC);
    if (!r) r = consumeToken(b, DASHES);
    if (!r) r = consumeToken(b, STRING_BEGIN);
    if (!r) r = consumeToken(b, TAG_NAME);
    if (!r) r = consumeToken(b, TAG_NAME_LANGUAGE);
    if (!r) r = consumeToken(b, ASYNC);
    if (!r) r = consumeToken(b, NULLABLE);
    if (!r) r = consumeToken(b, LBRACK);
    if (!r) r = consumeToken(b, RBRACK);
    if (!r) r = consumeToken(b, PACKAGE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (DASHES after_dash?)*
  static boolean doc(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc")) return false;
    while (true) {
      int c = current_position_(b);
      if (!doc_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "doc", c)) break;
    }
    return true;
  }

  // DASHES after_dash?
  private static boolean doc_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DASHES);
    r = r && doc_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // after_dash?
  private static boolean doc_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_0_1")) return false;
    after_dash(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '@' tag_normal
  static boolean doc_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_item")) return false;
    if (!nextTokenIs(b, AT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AT);
    r = r && tag_normal(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // TAG_NAME any*
  public static boolean tag_normal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_normal")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TAG_NORMAL, "<tag normal>");
    r = consumeToken(b, TAG_NAME);
    p = r; // pin = 1
    r = r && tag_normal_1(b, l + 1);
    exit_section_(b, l, m, r, p, LuaDocParser::after_dash_recover);
    return r || p;
  }

  // any*
  private static boolean tag_normal_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_normal_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!any(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "tag_normal_1", c)) break;
    }
    return true;
  }

}
