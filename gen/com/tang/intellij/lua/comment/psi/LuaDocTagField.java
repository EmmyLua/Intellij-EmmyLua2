// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface LuaDocTagField extends LuaDocTag {

  @Nullable
  LuaDocAccessModifier getAccessModifier();

  @Nullable
  LuaDocClassNameRef getClassNameRef();

  @Nullable
  LuaDocCommentString getCommentString();

  @Nullable
  LuaDocTy getTy();

  @Nullable
  PsiElement getId();

}
