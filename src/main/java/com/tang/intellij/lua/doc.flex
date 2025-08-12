package com.tang.intellij.lua.comment.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.tang.intellij.lua.comment.psi.LuaDocTypes;

%%

%class _LuaDocLexer
%implements FlexLexer, LuaDocTypes

%unicode
%public

%function advance
%type IElementType

%eof{ return;
%eof}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////// User code //////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

%{ // User code
    public _LuaDocLexer() {
        this((java.io.Reader) null);
    }
%}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// LuaDoc lexems ////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+
STRING=[^\r\n\t\f]*
//三个-以上
DOC_DASHES = --+
//Strings
DOUBLE_QUOTED_STRING=\"([^\\\"]|\\\S|\\[\r\n])*\"?
SINGLE_QUOTED_STRING='([^\\\']|\\\S|\\[\r\n])*'?

%state xCOMMENT_STRING

%%

<YYINITIAL> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {DOC_DASHES}               { return DASHES; }
    .                          { yybegin(xCOMMENT_STRING); yypushback(yylength()); }
}

<xCOMMENT_STRING> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {STRING}                   { yybegin(YYINITIAL); return STRING; }
}

[^]                            { return TokenType.WHITE_SPACE; }