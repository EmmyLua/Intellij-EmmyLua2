package com.tang.intellij.lua.comment.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;import com.intellij.psi.tree.IElementType;
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
    private int _typeLevel = 0;
    private boolean _typeReq = false;
    public _LuaDocLexer() {
        this((java.io.Reader) null);
    }

    private void beginType() {
        yybegin(xTYPE_REF);
        _typeLevel = 0;
        _typeReq = true;
    }
%}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// LuaDoc lexems ////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+
STRING=[^\r\n\t\f]*
ID=[:jletter:] ([:jletterdigit:]|\.|\*|\-)*
NUMBER=[0-9]+
AT=@
//三个-以上
DOC_DASHES = --+
//Strings
DOUBLE_QUOTED_STRING=\"([^\\\"]|\\\S|\\[\r\n])*\"?  //\"([^\\\"\r\n]|\\[^\r\n])*\"?
SINGLE_QUOTED_STRING='([^\\\']|\\\S|\\[\r\n])*'?    //'([^\\'\r\n]|\\[^\r\n])*'?

%state xTAG
%state xTAG_WITH_ID
%state xTAG_NAME
%state xCOMMENT_STRING
%state xPARAM
%state xTYPE_REF
%state xCLASS
%state xCLASS_GENERIC
%state xCLASS_EXTEND
%state xFIELD
%state xFIELD_INDEX
%state xFIELD_ID
%state xGENERIC
%state xALIAS
%state xDOUBLE_QUOTED_STRING
%state xSINGLE_QUOTED_STRING
%state xMODULE
%state xTAG_ENUMFIELD
%state xTAG_DIAGNOSTIC
%state xENUM
%%

<YYINITIAL> {
    {EOL}                      { yybegin(YYINITIAL); return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
    {DOC_DASHES}               { return DASHES; }
    "@"                        { yybegin(xTAG_NAME); return AT; }
    "|"                        { yybegin(xTAG_ENUMFIELD); ;return OR; }
    .                          { yybegin(xCOMMENT_STRING); yypushback(yylength()); }
}

<xTAG, xTAG_WITH_ID, xTAG_NAME, xPARAM, xTYPE_REF, xCLASS, xCLASS_EXTEND, xFIELD, xFIELD_ID, xCOMMENT_STRING, xGENERIC, xALIAS,
    xMODULE, xTAG_ENUMFIELD, xTAG_DIAGNOSTIC, xENUM, xCLASS_GENERIC, xFIELD_INDEX> {
    {EOL}                      { yybegin(YYINITIAL);return com.intellij.psi.TokenType.WHITE_SPACE;}
    {LINE_WS}+                 { return com.intellij.psi.TokenType.WHITE_SPACE; }
}

<xTAG_NAME> {
    "field"                    { yybegin(xFIELD); return TAG_NAME; }
    "param"                    { yybegin(xPARAM); return TAG_NAME; }
    "class"                    { yybegin(xCLASS); return TAG_NAME; }
    "interface"                { yybegin(xCLASS); return TAG_NAME; }
    "enum"                     { yybegin(xENUM); return TAG_NAME; }
    "module"                   { yybegin(xMODULE); return TAG_NAME; }
    "source"                   { yybegin(xMODULE); return TAG_NAME; }
    "return"                   { beginType(); return TAG_NAME; }
    "type"                     { beginType(); return TAG_NAME;}
    "overload"                 { beginType(); return TAG_NAME; }
    "private"                  { return TAG_NAME; }
    "protected"                { return TAG_NAME; }
    "public"                   { return TAG_NAME; }
    "package"                  { yybegin(xTAG_WITH_ID); return TAG_NAME; }
    "language"                 { yybegin(xTAG_WITH_ID); return TAG_NAME_LANGUAGE;}
    "generic"                  { yybegin(xGENERIC); return TAG_NAME; }
    "see"                      { yybegin(xTAG); return TAG_NAME; }
    "namespace"                { yybegin(xTAG_WITH_ID); return TAG_NAME; }
    "using"                    { yybegin(xTAG_WITH_ID); return TAG_NAME; }
    "alias"                    { yybegin(xALIAS); return TAG_NAME; }
    "diagnostic"               { yybegin(xTAG_DIAGNOSTIC); return TAG_NAME; }
    {ID}                       { yybegin(xCOMMENT_STRING); return TAG_NAME; }
    [^]                        { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}

<xALIAS> {
    {ID}                       { beginType(); return ID; }
    [^]                        { yybegin(YYINITIAL); yypushback(yylength()); }
}

<xGENERIC> {
    {ID}                       { return ID; }
    ":"                        { return EXTENDS;}
    ","                        { return COMMA; }
    [^]                        { yybegin(YYINITIAL); yypushback(yylength()); }
}

<xCLASS> {
    "<"                        { yybegin(xCLASS_GENERIC); return LT; }
    {ID}                       { yybegin(xCLASS_EXTEND); return ID; }
}
<xCLASS_GENERIC> {
    {ID}                       { return ID; }
    ","                        { return COMMA; }
    ":"                        { return EXTENDS;}
    ">"                        { return GT; }
}

<xCLASS_EXTEND> {
    ":"                        { beginType(); return EXTENDS;}
    [^]                        { yybegin(xCOMMENT_STRING); yypushback(yylength()); }
}

<xPARAM> {
    {ID}                       { beginType(); return ID; }
    "..."                      { beginType(); return ID; } //varargs
}

<xFIELD> {
    "private"                  { yybegin(xFIELD_ID); return PRIVATE; }
    "protected"                { yybegin(xFIELD_ID); return PROTECTED; }
    "public"                   { yybegin(xFIELD_ID); return PUBLIC; }
    "package"                  { yybegin(xFIELD_ID); return PACKAGE; }
    "["                        { yybegin(xFIELD_ID); yypushback(yylength());}
    {ID}                       { beginType(); return ID; }
}
<xFIELD_ID> {
    "["                        { yybegin(xFIELD_INDEX); return LBRACK; }
    {ID}                       { beginType(); return ID; }
}

<xFIELD_INDEX> {
    {NUMBER}                   { return NUMBER; }
    {ID}                       { return ID; }
    "]"                        { yybegin(xFIELD_ID); return RBRACK; }
}

<xMODULE> {
    {ID}                       { return ID; }
    {STRING}                   { return STRING_LITERAL; }
    "."                        { return DOT; }
}

<xTAG_DIAGNOSTIC> {
    {ID}                       { return ID; }
    ":"                        { return EXTENDS;}
    ","                        { return COMMA; }
}

<xTYPE_REF> {
    "?"                        { return NULLABLE; }
    "@"                        { yybegin(xCOMMENT_STRING); return STRING_BEGIN; }
    "#"                        { yybegin(xCOMMENT_STRING); return STRING_BEGIN; }
    ","                        { _typeReq = true; return COMMA; }
    "|"                        { _typeReq = true; return OR; }
    ":"                        { _typeReq = true; return EXTENDS;}
    "<"                        { _typeLevel++; return LT; }
    ">"                        { _typeLevel--; _typeReq = false; return GT; }
    "("                        { _typeLevel++; return LPAREN; }
    ")"                        { _typeLevel--; _typeReq = false; return RPAREN; }
    "{"                        { _typeLevel++; return LCURLY; }
    "}"                        { _typeLevel--; _typeReq = false; return RCURLY; }
    "\""                       { yybegin(xDOUBLE_QUOTED_STRING); yypushback(yylength()); }
    "'"                        { yybegin(xSINGLE_QUOTED_STRING); yypushback(yylength()); }
    "[]"                       { _typeReq = false; return ARR; }
    "fun"                      { return FUN; }
    "vararg"                   { _typeReq = true; return VARARG; }
    "async"                    { return ASYNC; }
    {NUMBER}                   { return NUMBER; }
    "..."|{ID}                 { if (_typeReq || _typeLevel > 0) { _typeReq = false; return ID; } else { yybegin(xCOMMENT_STRING); yypushback(yylength()); } }
}

<xDOUBLE_QUOTED_STRING> {
    {DOUBLE_QUOTED_STRING}    { yybegin(xTYPE_REF); return STRING_LITERAL; }
}

<xSINGLE_QUOTED_STRING> {
    {SINGLE_QUOTED_STRING}    { yybegin(xTYPE_REF); return STRING_LITERAL; }
}

<xTAG> {
    "@"                        { yybegin(xCOMMENT_STRING); return STRING_BEGIN; }
    "#"                        { return SHARP; }
    {ID}                       { return ID; }
    [^]                        { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
<xTAG_WITH_ID> {
    {ID}                       { yybegin(xCOMMENT_STRING); return ID; }
}

<xCOMMENT_STRING> {
    {STRING}                   { yybegin(YYINITIAL); return STRING; }
}

[^]                            { return TokenType.WHITE_SPACE; }