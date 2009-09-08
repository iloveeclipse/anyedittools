/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.util.TextUtil;

/**
 * @author Andrei
 */
public class AndyEditPreferenceInitializer
extends
AbstractPreferenceInitializer {

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#initializeDefaultPluginPreferences()
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    public void initializeDefaultPreferences() {
        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        store.setDefault(IAnyEditConstants.EDITOR_TAB_WIDTH, "2");
        store.setDefault(
                IAnyEditConstants.CHARACTERS_DISALLOWED_IN_PATH,
                TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);
        store.setDefault(
                IAnyEditConstants.CHARACTERS_REQUIRED_IN_PATH,
                TextUtil.DEFAULT_CHARACTERS_REQUIRED_IN_PATH);
        store.setDefault(
                IAnyEditConstants.BASE64_LINE_LENGTH,
                TextUtil.DEFAULT_BASE64_LINE_LENGTH);
        store.setDefault(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE, false);
        store.setDefault(IAnyEditConstants.USE_REQUIRED_IN_PATH_CHARACTERS, true);
        store.setDefault(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA, true);
        store.setDefault(IAnyEditConstants.SAVE_DIRTY_BUFFER, true);
        store.setDefault(IAnyEditConstants.REMOVE_TRAILING_SPACES, true);
        store.setDefault(IAnyEditConstants.PRESERVE_ENTITIES, true);
        store.setDefault(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED, false);
        store.setDefault(IAnyEditConstants.SAVE_AND_ADD_LINE, false);
        store.setDefault(IAnyEditConstants.SAVE_AND_TRIM_ENABLED, true);
        store.setDefault(IAnyEditConstants.CONVERT_ACTION_ON_SAVE,
                IAnyEditConstants.ACTION_ID_CONVERT_TABS);
        store.setDefault(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES, false);
        store.setDefault(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS, false);
        store.setDefault(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST, "*.makefile,makefile,*.Makefile,Makefile,Makefile.*,*.mk,MANIFEST.MF");
        store.setDefault(IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST, "");
        store.setDefault(IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER, true);
        store.setDefault(IAnyEditConstants.INCLUDE_DERIVED_RESOURCES, false);
        store.setDefault(IAnyEditConstants.ADD_SAVE_ALL_TO_TOOLBAR, true);
        store.setDefault(IAnyEditConstants.REMOVE_PRINT_FROM_TOOLBAR, false);
        store.setDefault(IAnyEditConstants.USE_WORKSPACE_SCOPE_FOR_SEARCH, false);
        store.setDefault(IAnyEditConstants.SHOW_ONLY_BAD_WHITESPACE, true);
        store.setDefault(IAnyEditConstants.SAVE_TO_OPEN_EDITOR, false);
        store.setDefault(IAnyEditConstants.SAVE_TO_IGNORE_SELECTION, false);
        store.setDefault(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS, true);
        store.setDefault(IAnyEditConstants.WARN_ABOUT_UNSUPPORTED_UNICODE, true);
        store.setDefault(IAnyEditConstants.UNICODIFY_ALL, false);
        store.setDefault(IAnyEditConstants.BASE64_SPLIT_LINE, false);
        store.setDefault(IAnyEditConstants.HIDE_OPEN_TYPE_ACTION, false);
    }
}
