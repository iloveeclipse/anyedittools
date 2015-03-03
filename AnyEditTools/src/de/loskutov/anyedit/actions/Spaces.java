/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
/* This class is started as extension of Rahul Kuchal's whitespace plugin.
 * Rahul Kuchal - http://www.kuchhal.com/ */

package de.loskutov.anyedit.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.jdt.JdtUtils;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.ui.preferences.CombinedPreferences;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.LineReplaceResult;
import de.loskutov.anyedit.util.TextReplaceResultSet;
import de.loskutov.anyedit.util.TextUtil;

public class Spaces extends AbstractTextAction {

    private CombinedPreferences combinedPreferences;

    public Spaces() {
        super();
    }

    @Override
    protected TextReplaceResultSet estimateActionRange(IDocument doc) {
        TextReplaceResultSet result = new TextReplaceResultSet();
        if (doc == null) {
            return result;
        }
        int linesNumber = doc.getNumberOfLines();
        result.setStartLine(0);
        result.setStopLine(linesNumber - 1);
        return result;
    }

    /**
     * Should be invoked always after estimateActionRange() to ensure that
     * operaton is possible
     * @param doc cannot be null
     * @param actionID
     * @param resultSet cannot be null
     */
    @Override
    protected void doTextOperation(IDocument doc, String actionID,
            TextReplaceResultSet resultSet) throws BadLocationException {
        int maxNbr = resultSet.getStartLine() + resultSet.getNumberOfLines();
        boolean removeTrailing;
        boolean convertEnabled;
        boolean tabsToSpaces;
        boolean addLineEnabled;
        boolean fixLineDelimiters;
        CombinedPreferences prefs = getCombinedPreferences();
        boolean replaceAllTabs = isReplaceAllTabsEnabled(prefs);
        boolean replaceAllSpaces = isReplaceAllSpacesEnabled(prefs);
        boolean useModulo4Tabs = prefs.getBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE);
        boolean ignoreBlankLines = prefs.getBoolean(IAnyEditConstants.IGNORE_BLANK_LINES_WHEN_TRIMMING);

        boolean usedOnSave = isUsedOnSave();
        if (usedOnSave) {
            removeTrailing = isSaveAndTrimEnabled();
            tabsToSpaces = isDefaultTabToSpaces();
            convertEnabled = isSaveAndConvertEnabled();
            addLineEnabled = isSaveAndAddLineEnabled(prefs);
            fixLineDelimiters = isSaveAndFixLineDelimitersEnabled(prefs);
        } else {
            removeTrailing = isRemoveTrailingSpaceEnabled(prefs);
            tabsToSpaces = actionID.startsWith(ACTION_ID_CONVERT_TABS);
            convertEnabled = true;
            addLineEnabled = isAddLineEnabled(prefs);
            fixLineDelimiters = isFixLineDelimitersEnabled(prefs);
        }

        int tabWidth = getTabWidth(getFile(), prefs);
        String lineDelimiter = getLineDelimiter();

        StringBuffer sb = new StringBuffer();
        if (!tabsToSpaces && tabWidth == 0) {
            // TODO: prevent division by zero - probably on another place?
            tabWidth = 1;
        }
        String line;
        IRegion lineInfo;

        for (int i = resultSet.getStartLine(); i < maxNbr; i++) {
            lineInfo = doc.getLineInformation(i);
            // whole line text will be "replaced"
            int rangeToReplace = lineInfo.getLength();
            line = doc.get(lineInfo.getOffset(), rangeToReplace);
            if (line == null) {
                resultSet.add(null);
                continue;
            }
            sb.append(line);
            boolean changed;
            if (convertEnabled) {
                if (tabsToSpaces) {
                    changed = TextUtil.convertTabsToSpaces(sb, tabWidth, removeTrailing,
                            ignoreBlankLines, replaceAllTabs, useModulo4Tabs);
                } else {
                    changed = TextUtil.convertSpacesToTabs(sb, tabWidth, removeTrailing,
                            ignoreBlankLines, replaceAllSpaces);
                }
            } else {
                if (!usedOnSave || removeTrailing) {
                    changed = TextUtil.removeTrailingSpace(sb, ignoreBlankLines);
                } else {
                    changed = false;
                }
            }

            // on the last NON empty line add new line character(s)
            if (addLineEnabled && i == maxNbr - 1 && sb.length() != 0) {
                sb.append(lineDelimiter);
                changed = true;
            } else if(fixLineDelimiters){
                final String delimiter = doc.getLineDelimiter(i);
                if (delimiter != null && delimiter.length() > 0 && !delimiter.equals(lineDelimiter)) {
                    rangeToReplace += delimiter.length();
                    sb.append(lineDelimiter);
                    changed = true;
                }
            }

            if (changed) {
                LineReplaceResult result = new LineReplaceResult();
                result.rangeToReplace = rangeToReplace;
                result.textToReplace = sb.toString();
                resultSet.add(result);
            } else {
                resultSet.add(null);
            }
            // cleanup
            sb.setLength(0);
        }
    }

    @Override
    public void setFile(IFile file) {
        super.setFile(file);
        combinedPreferences = null;
    }

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        super.setActiveEditor(action, targetEditor);
        combinedPreferences = null;
    }

    @Override
    public void setEditor(AbstractEditor editor) {
        super.setEditor(editor);
        combinedPreferences = null;
    }

    public CombinedPreferences getCombinedPreferences() {
        if(combinedPreferences != null){
            return combinedPreferences;
        }
        IFile file = getFile();
        IScopeContext context = null;
        if (file != null) {
            IProject project = file.getProject();
            if (project != null) {
                context = new ProjectScope(project);
            }
        }
        combinedPreferences = new CombinedPreferences(context,
                AnyEditToolsPlugin.getDefault().getPreferenceStore());
        return combinedPreferences;
    }

    protected String getLineDelimiter() {
        IFile file = getFile();
        IProject project = null;
        if (file != null) {
            project = file.getProject();
        }
        String value = getLineDelimiter(getPlatformPreferences(project));
        if (value == null) {
            value = getLineDelimiter(getPlatformPreferences(null));
        }
        if (value == null) {
            value = getLineDelimiter(Platform.getPreferencesService().getRootNode().node(DefaultScope.SCOPE));
        }
        return value != null ? value : System.getProperty(Platform.PREF_LINE_SEPARATOR, "\n");
    }

    private static Preferences getPlatformPreferences(IProject project) {
        IEclipsePreferences rootNode = Platform.getPreferencesService().getRootNode();
        if (project != null) {
            return rootNode.node(ProjectScope.SCOPE).node(project.getName());
        }
        return rootNode.node(InstanceScope.SCOPE);
    }

    private static String getLineDelimiter(Preferences node) {
        try {
            // be careful looking up for our node so not to create any nodes as side effect
            if (node.nodeExists(Platform.PI_RUNTIME)) {
                return node.node(Platform.PI_RUNTIME).get(Platform.PREF_LINE_SEPARATOR, null);
            }
        } catch (BackingStoreException e) {
            // ignore
        }
        return null;
    }

    public boolean isSaveAndTrimEnabled() {
        return getCombinedPreferences().getBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED);
    }

    private boolean isSaveAndAddLineEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE);
    }

    private boolean isSaveAndFixLineDelimitersEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.SAVE_AND_FIX_LINE_DELIMITERS);
    }

    public boolean isSaveAndConvertEnabled() {
        return getCombinedPreferences().getBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED);
    }

    private boolean isAddLineEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.ADD_NEW_LINE);
    }

    protected boolean isRemoveTrailingSpaceEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES);
    }

    protected boolean isFixLineDelimitersEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.FIX_LINE_DELIMITERS);
    }

    protected boolean isReplaceAllTabsEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES);
    }
    protected boolean isReplaceAllSpacesEnabled(CombinedPreferences prefs) {
        return prefs.getBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS);
    }

    public boolean isDefaultTabToSpaces() {
        String action = getCombinedPreferences().getString(IAnyEditConstants.CONVERT_ACTION_ON_SAVE);
        return IAnyEditConstants.ACTION_ID_CONVERT_TABS.equals(action);
    }

    public int getTabWidth(IFile file, CombinedPreferences prefs) {

        int tabWidth = -1;
        if (EclipseUtils.isJavaInput(file)
                && prefs.getBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA)) {
            tabWidth = JdtUtils.getTabWidth(file);
        } else {
            tabWidth = prefs.getInt(IAnyEditConstants.EDITOR_TAB_WIDTH);
        }
        if (tabWidth < 0) {
            tabWidth = IAnyEditConstants.DEFAULT_TAB_WIDTH;
        }
        return tabWidth;
    }
}
