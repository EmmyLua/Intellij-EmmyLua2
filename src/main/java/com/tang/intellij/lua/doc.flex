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

// 单词分隔符：空格、制表符等
WORD_SEPARATOR=[\ \t\f]
// 单词：字母、数字、下划线等组成，不包含#和@
WORD=[a-zA-Z_][a-zA-Z0-9_]*
// 特殊符号：点、逗号、分号、冒号、括号等
SPECIAL_SYMBOL=[\.,:;()\[\]{}|&!+\-*/=<>?~`'\"\\]
// #和@符号
HASH="#"
AT="@"

%state xCOMMENT_STRING
%state xWORD_PARSING

%%

<YYINITIAL> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {DOC_DASHES}               { yybegin(xWORD_PARSING); return DASHES; }
    .                          { yybegin(xCOMMENT_STRING); yypushback(yylength()); }
}

<xWORD_PARSING> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {WORD_SEPARATOR}+          { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {WORD}                     { return WORD; }
    {SPECIAL_SYMBOL}           { return STRING; }
    {HASH}                     { yybegin(xCOMMENT_STRING); return HASH; }
    {AT}                       { return AT; }
    .                          { yybegin(xCOMMENT_STRING); yypushback(yylength()); }
}

<xCOMMENT_STRING> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {STRING}                   { yybegin(YYINITIAL); return STRING; }
}

[^]                            { return TokenType.WHITE_SPACE; }