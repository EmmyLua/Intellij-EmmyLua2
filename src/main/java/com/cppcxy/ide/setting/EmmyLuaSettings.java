package com.cppcxy.ide.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "EmmyLuaSettings",
    storages = @Storage("emmylua2.xml")
)
public class EmmyLuaSettings implements PersistentStateComponent<EmmyLuaSettings> {
    
    // Analyzer location
    private String location = "";
    
    // Completion settings
    private boolean completionEnable = true;
    private boolean autoRequire = true;
    private String autoRequireFunction = "require";
    private String autoRequireNamingConvention = "keep";
    private String autoRequireSeparator = ".";
    private boolean baseFunctionIncludesName = true;
    private boolean callSnippet = false;
    private String postfix = "@";

    // Diagnostic settings
    private boolean diagnosticEnable = true;
    private int diagnosticInterval = 500;
    private List<String> diagnosticDisable = new ArrayList<>();
    private List<String> diagnosticEnables = new ArrayList<>();
    private List<String> globalVariables = new ArrayList<>();
    private List<String> globalsRegex = new ArrayList<>();

    // Hint settings
    private boolean hintEnable = true;
    private boolean enumParamHint = false;
    private boolean indexHint = true;
    private boolean localHint = true;
    private boolean metaCallHint = true;
    private boolean overrideHint = true;
    private boolean paramHint = true;

    // Other feature settings
    private boolean codeLensEnable = true;
    private boolean codeActionInsertSpace = false;
    private boolean documentColorEnable = true;
    private boolean hoverEnable = true;
    private boolean inlineValuesEnable = true;
    private boolean referencesEnable = true;
    private boolean referencesFuzzySearch = true;
    private boolean referencesShortStringSearch = false;
    private boolean semanticTokensEnable = true;
    private boolean signatureDetailHelper = true;

    // Runtime settings
    private String luaVersion = "LuaLatest";
    private List<String> extensions = new ArrayList<>();
    private List<String> frameworkVersions = new ArrayList<>();
    private List<String> requireLikeFunction = new ArrayList<>();
    private List<String> requirePattern = new ArrayList<>();

    // Class default call settings
    private boolean classDefaultCallForceNonColon = false;
    private boolean classDefaultCallForceReturnSelf = false;
    private String classDefaultCallFunctionName = "";

    // Doc settings
    private List<String> docKnownTags = new ArrayList<>();
    private List<String> docPrivateName = new ArrayList<>();

    // Strict settings
    private boolean strictArrayIndex = true;
    private boolean strictDocBaseConstMatchBaseType = true;
    private boolean strictMetaOverrideFileDefine = true;
    private boolean strictRequirePath = false;
    private boolean strictTypeCall = false;

    // Workspace settings
    private boolean workspaceEnableReindex = false;
    private String workspaceEncoding = "utf-8";
    private List<String> workspaceIgnoreDir = new ArrayList<>();
    private List<String> workspaceIgnoreGlobs = new ArrayList<>();
    private List<String> workspaceLibrary = new ArrayList<>();
    private int workspacePreloadFileSize = 0;
    private int workspaceReindexDuration = 5000;
    private List<String> workspaceRoots = new ArrayList<>();

    // Resource settings
    private List<String> resourcePaths = new ArrayList<>();

    public static EmmyLuaSettings getInstance() {
        return ApplicationManager.getApplication().getService(EmmyLuaSettings.class);
    }

    @Override
    public @Nullable EmmyLuaSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull EmmyLuaSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // Completion settings
    public boolean isCompletionEnable() { return completionEnable; }
    public void setCompletionEnable(boolean completionEnable) { this.completionEnable = completionEnable; }

    public boolean isAutoRequire() { return autoRequire; }
    public void setAutoRequire(boolean autoRequire) { this.autoRequire = autoRequire; }

    public String getAutoRequireFunction() { return autoRequireFunction; }
    public void setAutoRequireFunction(String autoRequireFunction) { this.autoRequireFunction = autoRequireFunction; }

    public String getAutoRequireNamingConvention() { return autoRequireNamingConvention; }
    public void setAutoRequireNamingConvention(String autoRequireNamingConvention) { this.autoRequireNamingConvention = autoRequireNamingConvention; }

    public String getAutoRequireSeparator() { return autoRequireSeparator; }
    public void setAutoRequireSeparator(String autoRequireSeparator) { this.autoRequireSeparator = autoRequireSeparator; }

    public boolean isBaseFunctionIncludesName() { return baseFunctionIncludesName; }
    public void setBaseFunctionIncludesName(boolean baseFunctionIncludesName) { this.baseFunctionIncludesName = baseFunctionIncludesName; }

    public boolean isCallSnippet() { return callSnippet; }
    public void setCallSnippet(boolean callSnippet) { this.callSnippet = callSnippet; }

    public String getPostfix() { return postfix; }
    public void setPostfix(String postfix) { this.postfix = postfix; }

    // Diagnostic settings
    public boolean isDiagnosticEnable() { return diagnosticEnable; }
    public void setDiagnosticEnable(boolean diagnosticEnable) { this.diagnosticEnable = diagnosticEnable; }

    public int getDiagnosticInterval() { return diagnosticInterval; }
    public void setDiagnosticInterval(int diagnosticInterval) { this.diagnosticInterval = diagnosticInterval; }

    public List<String> getDiagnosticDisable() { return diagnosticDisable; }
    public void setDiagnosticDisable(List<String> diagnosticDisable) { this.diagnosticDisable = diagnosticDisable; }

    public List<String> getDiagnosticEnables() { return diagnosticEnables; }
    public void setDiagnosticEnables(List<String> diagnosticEnables) { this.diagnosticEnables = diagnosticEnables; }

    public List<String> getGlobalVariables() { return globalVariables; }
    public void setGlobalVariables(List<String> globalVariables) { this.globalVariables = globalVariables; }

    public List<String> getGlobalsRegex() { return globalsRegex; }
    public void setGlobalsRegex(List<String> globalsRegex) { this.globalsRegex = globalsRegex; }

    // Hint settings
    public boolean isHintEnable() { return hintEnable; }
    public void setHintEnable(boolean hintEnable) { this.hintEnable = hintEnable; }

    public boolean isEnumParamHint() { return enumParamHint; }
    public void setEnumParamHint(boolean enumParamHint) { this.enumParamHint = enumParamHint; }

    public boolean isIndexHint() { return indexHint; }
    public void setIndexHint(boolean indexHint) { this.indexHint = indexHint; }

    public boolean isLocalHint() { return localHint; }
    public void setLocalHint(boolean localHint) { this.localHint = localHint; }

    public boolean isMetaCallHint() { return metaCallHint; }
    public void setMetaCallHint(boolean metaCallHint) { this.metaCallHint = metaCallHint; }

    public boolean isOverrideHint() { return overrideHint; }
    public void setOverrideHint(boolean overrideHint) { this.overrideHint = overrideHint; }

    public boolean isParamHint() { return paramHint; }
    public void setParamHint(boolean paramHint) { this.paramHint = paramHint; }

    // Other feature settings
    public boolean isCodeLensEnable() { return codeLensEnable; }
    public void setCodeLensEnable(boolean codeLensEnable) { this.codeLensEnable = codeLensEnable; }

    public boolean isCodeActionInsertSpace() { return codeActionInsertSpace; }
    public void setCodeActionInsertSpace(boolean codeActionInsertSpace) { this.codeActionInsertSpace = codeActionInsertSpace; }

    public boolean isDocumentColorEnable() { return documentColorEnable; }
    public void setDocumentColorEnable(boolean documentColorEnable) { this.documentColorEnable = documentColorEnable; }

    public boolean isHoverEnable() { return hoverEnable; }
    public void setHoverEnable(boolean hoverEnable) { this.hoverEnable = hoverEnable; }

    public boolean isInlineValuesEnable() { return inlineValuesEnable; }
    public void setInlineValuesEnable(boolean inlineValuesEnable) { this.inlineValuesEnable = inlineValuesEnable; }

    public boolean isReferencesEnable() { return referencesEnable; }
    public void setReferencesEnable(boolean referencesEnable) { this.referencesEnable = referencesEnable; }

    public boolean isReferencesFuzzySearch() { return referencesFuzzySearch; }
    public void setReferencesFuzzySearch(boolean referencesFuzzySearch) { this.referencesFuzzySearch = referencesFuzzySearch; }

    public boolean isReferencesShortStringSearch() { return referencesShortStringSearch; }
    public void setReferencesShortStringSearch(boolean referencesShortStringSearch) { this.referencesShortStringSearch = referencesShortStringSearch; }

    public boolean isSemanticTokensEnable() { return semanticTokensEnable; }
    public void setSemanticTokensEnable(boolean semanticTokensEnable) { this.semanticTokensEnable = semanticTokensEnable; }

    public boolean isSignatureDetailHelper() { return signatureDetailHelper; }
    public void setSignatureDetailHelper(boolean signatureDetailHelper) { this.signatureDetailHelper = signatureDetailHelper; }

    // Runtime settings
    public String getLuaVersion() { return luaVersion; }
    public void setLuaVersion(String luaVersion) { this.luaVersion = luaVersion; }

    public List<String> getExtensions() { return extensions; }
    public void setExtensions(List<String> extensions) { this.extensions = extensions; }

    public List<String> getFrameworkVersions() { return frameworkVersions; }
    public void setFrameworkVersions(List<String> frameworkVersions) { this.frameworkVersions = frameworkVersions; }

    public List<String> getRequireLikeFunction() { return requireLikeFunction; }
    public void setRequireLikeFunction(List<String> requireLikeFunction) { this.requireLikeFunction = requireLikeFunction; }

    public List<String> getRequirePattern() { return requirePattern; }
    public void setRequirePattern(List<String> requirePattern) { this.requirePattern = requirePattern; }

    // Class default call settings
    public boolean isClassDefaultCallForceNonColon() { return classDefaultCallForceNonColon; }
    public void setClassDefaultCallForceNonColon(boolean classDefaultCallForceNonColon) { this.classDefaultCallForceNonColon = classDefaultCallForceNonColon; }

    public boolean isClassDefaultCallForceReturnSelf() { return classDefaultCallForceReturnSelf; }
    public void setClassDefaultCallForceReturnSelf(boolean classDefaultCallForceReturnSelf) { this.classDefaultCallForceReturnSelf = classDefaultCallForceReturnSelf; }

    public String getClassDefaultCallFunctionName() { return classDefaultCallFunctionName; }
    public void setClassDefaultCallFunctionName(String classDefaultCallFunctionName) { this.classDefaultCallFunctionName = classDefaultCallFunctionName; }

    // Doc settings
    public List<String> getDocKnownTags() { return docKnownTags; }
    public void setDocKnownTags(List<String> docKnownTags) { this.docKnownTags = docKnownTags; }

    public List<String> getDocPrivateName() { return docPrivateName; }
    public void setDocPrivateName(List<String> docPrivateName) { this.docPrivateName = docPrivateName; }

    // Strict settings
    public boolean isStrictArrayIndex() { return strictArrayIndex; }
    public void setStrictArrayIndex(boolean strictArrayIndex) { this.strictArrayIndex = strictArrayIndex; }

    public boolean isStrictDocBaseConstMatchBaseType() { return strictDocBaseConstMatchBaseType; }
    public void setStrictDocBaseConstMatchBaseType(boolean strictDocBaseConstMatchBaseType) { this.strictDocBaseConstMatchBaseType = strictDocBaseConstMatchBaseType; }

    public boolean isStrictMetaOverrideFileDefine() { return strictMetaOverrideFileDefine; }
    public void setStrictMetaOverrideFileDefine(boolean strictMetaOverrideFileDefine) { this.strictMetaOverrideFileDefine = strictMetaOverrideFileDefine; }

    public boolean isStrictRequirePath() { return strictRequirePath; }
    public void setStrictRequirePath(boolean strictRequirePath) { this.strictRequirePath = strictRequirePath; }

    public boolean isStrictTypeCall() { return strictTypeCall; }
    public void setStrictTypeCall(boolean strictTypeCall) { this.strictTypeCall = strictTypeCall; }

    // Workspace settings
    public boolean isWorkspaceEnableReindex() { return workspaceEnableReindex; }
    public void setWorkspaceEnableReindex(boolean workspaceEnableReindex) { this.workspaceEnableReindex = workspaceEnableReindex; }

    public String getWorkspaceEncoding() { return workspaceEncoding; }
    public void setWorkspaceEncoding(String workspaceEncoding) { this.workspaceEncoding = workspaceEncoding; }

    public List<String> getWorkspaceIgnoreDir() { return workspaceIgnoreDir; }
    public void setWorkspaceIgnoreDir(List<String> workspaceIgnoreDir) { this.workspaceIgnoreDir = workspaceIgnoreDir; }

    public List<String> getWorkspaceIgnoreGlobs() { return workspaceIgnoreGlobs; }
    public void setWorkspaceIgnoreGlobs(List<String> workspaceIgnoreGlobs) { this.workspaceIgnoreGlobs = workspaceIgnoreGlobs; }

    public List<String> getWorkspaceLibrary() { return workspaceLibrary; }
    public void setWorkspaceLibrary(List<String> workspaceLibrary) { this.workspaceLibrary = workspaceLibrary; }

    public int getWorkspacePreloadFileSize() { return workspacePreloadFileSize; }
    public void setWorkspacePreloadFileSize(int workspacePreloadFileSize) { this.workspacePreloadFileSize = workspacePreloadFileSize; }

    public int getWorkspaceReindexDuration() { return workspaceReindexDuration; }
    public void setWorkspaceReindexDuration(int workspaceReindexDuration) { this.workspaceReindexDuration = workspaceReindexDuration; }

    public List<String> getWorkspaceRoots() { return workspaceRoots; }
    public void setWorkspaceRoots(List<String> workspaceRoots) { this.workspaceRoots = workspaceRoots; }

    // Resource settings
    public List<String> getResourcePaths() { return resourcePaths; }
    public void setResourcePaths(List<String> resourcePaths) { this.resourcePaths = resourcePaths; }
}
