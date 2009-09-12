/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit;

import org.eclipse.osgi.util.NLS;

public class Messages  extends NLS {
    private static final String BUNDLE_NAME = "de.loskutov.anyedit.messages";//$NON-NLS-1$

    private Messages() {
        // Do not instantiate
    }

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    public static String pref_tab_convert;
    public static String pref_tab_misc;
    public static String pref_tab_auto;
    public static String pref_tab_open;
    public static String pref_tab_saveTo;

    public static String pref_saveToIntro;
    public static String pref_saveToShowOptions;
    public static String pref_saveToShowOptionsTip;
    public static String pref_saveToOpenEditor;
    public static String pref_saveToOpenEditorTip;
    public static String pref_saveToNoSelection;
    public static String pref_saveToNoSelectionTip;



    public static String pref_tabWidth;
    public static String pref_tabWidthTip;

    public static String pref_useModulo4Tabs;
    public static String pref_useModulo4TabsTip;

    public static String pref_javaTabWidthForJava;
    public static String pref_javaTabWidthForJavaTip;

    public static String pref_openFileIntro;
    public static String pref_spacesIntro;
    public static String pref_entitiesIntro;
    public static String pref_toolbarIntro;
    public static String pref_saveIntro;
    public static String pref_convertChoiceIntro;

    public static String pref_addSaveAll;
    public static String pref_addSaveAllTip;

    public static String pref_removePrint;
    public static String pref_removePrintTip;

    public static String pref_preserveEntities;
    public static String pref_preserveEntitiesTip;

    public static String pref_saveDirtyEditor;
    public static String pref_saveDirtyEditorTip;

    public static String pref_removeTrailingSpaces;
    public static String pref_removeTrailingSpacesTip;

    public static String pref_replaceAllTabs;
    public static String pref_replaceAllTabsTip;

    public static String pref_replaceAllSpaces;
    public static String pref_replaceAllSpacesTip;

    public static String pref_disallowedInPath;
    public static String pref_disallowedInPathTip;

    public static String pref_requiredInPath;
    public static String pref_requiredInPathTip;

    public static String pref_requiredInPathEnabled;
    public static String pref_requiredInPathEnabledTip;

    public static String pref_lineSeparatorRegex;
    public static String pref_lineSeparatorRegexTip;

    public static String pref_saveAndTrim;
    public static String pref_saveAndTrimTip;

    public static String pref_saveAndAddLine;
    public static String pref_saveAndAddLineTip;

    public static String pref_saveAndConvert;
    public static String pref_saveAndConvertTip;

    public static String pref_convertTabsOnSave;
    public static String pref_convertTabsOnSaveTip;
    public static String pref_convertSpacesOnSave;
    public static String pref_convertSpacesOnSaveTip;

    public static String pref_saveHookNotEnabled;

    public static String pref_askBeforeConvertAll;
    public static String pref_askBeforeConvertAllTip;

    public static String pref_group_exclude;
    public static String pref_group_excludeTip;

    public static String pref_Defined_file_filters;
    public static String pref_Add_filter;
    public static String pref_Add_filterTip;
    public static String pref_RemoveFilter;
    public static String pref_RemoveFilterTip;
    public static String pref_Enable_all;
    public static String pref_Enable_allTip;
    public static String pref_Disable_all;
    public static String pref_Disable_allTip;
    public static String pref_Invalid_file_filter;
    public static String pref_hideOpenType;
    public static String pref_hideOpenTypeTip;
    public static String pref_includeDerived;
    public static String pref_includeDerivedTip;
    public static String pref_searchFileIntro;
    public static String pref_useWorkspaceScope;
    public static String pref_useWorkspaceScopeTip;

    public static String pref_unicodifyIntro;

    public static String pref_unicodifyAll;
    public static String pref_unicodifyAllTip;

    public static String pref_base64SplitLine;
    public static String pref_base64SplitLineTip;

    public static String pref_base64Intro;
    public static String pref_base64LineLength;
    public static String pref_base64LineLengthTip;

    public static String /*AnyEdit_*/continueOperationMessage;
    public static String /*AnyEdit_*/title;
    public static String /*AnyEdit_*/error;
    public static String /*AnyEdit_*/fileIsReadOnly;

    public static String ConvertAllInFolder_warnTitle;
    public static String ConvertAllInFolder_warnMessage;
    public static String ConvertAllInFolder_toggleMessage;
    public static String ConvertAllInFolder_task;
    public static String ConvertAll_task;
    public static String ConvertUnicode_title;
    public static String ConvertUnicode_warn;
    public static String ConvertUnicode_toggleMessage;


    public static String SaveTo_FileExists;
    public static String SaveTo_Override;
    public static String SaveTo_Append;

    public static String SaveTo_ShouldOpen;
    public static String SaveTo_MessageSelection;
    public static String SaveTo_MessageNoSelection;
    public static String SaveTo_MessageToggle;

    public static String OpenLineSeparatorRegex_WarningInvalidRegex;
}
