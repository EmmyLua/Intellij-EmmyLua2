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
  // token_sequence?
  static boolean comment_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_content")) return false;
    Marker m = enter_section_(b, l, _NONE_);
    token_sequence(b, l + 1);
    exit_section_(b, l, m, true, false, LuaDocParser::comment_content_recover);
    return true;
  }

  /* ********************************************************** */
  // !(DASHES)
  static boolean comment_content_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment_content_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, DASHES);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (DASHES comment_content?)*
  static boolean doc(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc")) return false;
    while (true) {
      int c = current_position_(b);
      if (!doc_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "doc", c)) break;
    }
    return true;
  }

  // DASHES comment_content?
  private static boolean doc_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DASHES);
    r = r && doc_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // comment_content?
  private static boolean doc_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_0_1")) return false;
    comment_content(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (WORD | STRING | HASH | AT)+
  static boolean token_sequence(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "token_sequence")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = token_sequence_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!token_sequence_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "token_sequence", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // WORD | STRING | HASH | AT
  private static boolean token_sequence_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "token_sequence_0")) return false;
    boolean r;
    r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, HASH);
    if (!r) r = consumeToken(b, AT);
    return r;
  }

}
