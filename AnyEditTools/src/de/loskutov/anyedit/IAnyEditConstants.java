/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit;

/**
 * Some constant names copied from org.eclipse.jdt.ui.PreferenceConstants
 *
 * @author Andrei
 */
public interface IAnyEditConstants {

    /**
     * A named preference that holds the number of spaces used per tab in the
     * editor.
     * <p>
     * Value is of type <code>Int</code>: positive int value specifying the
     * number of spaces per tab.
     * </p>
     */
    String EDITOR_TAB_WIDTH = "org.eclipse.jdt.ui.editor.tab.width";

    /**
     * A named preference that controls if the special characters are required
     * for valid file path.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String USE_REQUIRED_IN_PATH_CHARACTERS = "useRequiredChars";

    /**
     * A named preference that holds the characters, disallowed in file
     * name/path
     * <p>
     * Value is of type <code>String</code>
     */
    String CHARACTERS_DISALLOWED_IN_PATH = "disallowedInPath";

    /**
     * A named preference that holds the characters, required in file name/path
     * <p>
     * Value is of type <code>String</code>
     */
    String CHARACTERS_REQUIRED_IN_PATH = "requiredInPath";

    /**
     * A named preference that controls if java tab width used for java input
     * files
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String USE_JAVA_TAB_WIDTH_FOR_JAVA = "javaTabWidthForJava";

    /**
     * A named preference that controls if dirty editor buffer should be saved
     * before performing actions
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String SAVE_DIRTY_BUFFER = "saveDirtyBuffer";

    /**
     * A named preference that controls if while "tabs to spaces" we should
     * remove trailing spaces too
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String REMOVE_TRAILING_SPACES = "removeTrailingSpaces";

    /**
     * A named preference that controls if while "tabs to spaces" we should
     * replace all existing tabs (not only leading)
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String REPLACE_ALL_TABS_WITH_SPACES = "replaceAllTabs";

    /**
     * A named preference that controls if while "spaces to tabs" we should
     * replace all existing spaces (not only leading)
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String REPLACE_ALL_SPACES_WITH_TABS = "replaceAllSpaces";

    /**
     * Calculate number of tabs to replace with spaces using modulo operation, based on
     * current settings for tab width
     */
    String USE_MODULO_CALCULATION_FOR_TABS_REPLACE = "useModulo4Tabs";

    /**
     * A named preference that controls if while "chars to entities" we should
     * keep already existing entities
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String PRESERVE_ENTITIES = "preserveEntities";

    /**
     * A named preference that controls if before "save" action in editor the
     * "removeTrailing" action should be executed.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String SAVE_AND_TRIM_ENABLED = "saveAndTrim";

    /**
     * A named preference that controls if before "save" action in editor the
     * "addNewLine" action should be executed.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String SAVE_AND_ADD_LINE = "saveAndAddLine";

    /**
     * A named preference that controls if before "save" action in editor the
     * one of "convert whitespace" action should be executed.
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     */
    String SAVE_AND_CONVERT_ENABLED = "saveAndConvert";

    /**
     * A named preference that controls which operation should be default if
     * before "save" action in editor the one of "convert whitespace" action
     * should be executed - tabs or spaces.
     * <p>
     * Value is of type <code>String</code>, either ACTION_ID_CONVERT_TABS or
     * ACTION_ID_CONVERT_SPACES
     * </p>
     */
    String CONVERT_ACTION_ON_SAVE = "convertActionOnSaave";

    /**
     * one of two possible values for CONVERT_ACTION_ON_SAVE property
     */
    String ACTION_ID_CONVERT_TABS = "AnyEdit.CnvrtTabToSpaces";

    /**
     * one of two possible values for CONVERT_ACTION_ON_SAVE property
     */
    String ACTION_ID_CONVERT_SPACES = "AnyEdit.CnvrtSpacesToTabs";

    String PREF_ACTIVE_FILTERS_LIST = "activeContentFilterList";

    String PREF_INACTIVE_FILTERS_LIST = "inActiveContentFilterList";

    String ASK_BEFORE_CONVERT_ALL_IN_FOLDER = "askBeforeConvertAll";

    String WARN_ABOUT_UNSUPPORTED_UNICODE = "warnAboutUnsupportedUnicode";

    String INCLUDE_DERIVED_RESOURCES = "incudeDerivedResources";

    /** true to add "save all" action to the global toolbar */
    String ADD_SAVE_ALL_TO_TOOLBAR = "addSaveAllToToolbar";

    /** true to remove "print" action from the global toolbar */
    String REMOVE_PRINT_FROM_TOOLBAR = "removePrintFromToolbar";

    /** true to hide "Open Type" from editor/console menu */
    String HIDE_OPEN_TYPE_ACTION = "hideOpenTypeAction";

    /** not to limit the search scope to referenced projects if multiple references found */
    String USE_WORKSPACE_SCOPE_FOR_SEARCH = "useWorkspaceForSearch";

    String PROJECT_PROPS_ENABLED = "projectPropsEnabled";

    String SHOW_ONLY_BAD_WHITESPACE = "showOnlyBadWhitespace";

    /**
     * setiongs for saveToFile from console
     */
    String SAVE_TO_SHOW_OPTIONS = "saveToShowOptions";

    String SAVE_TO_OPEN_EDITOR = "saveToOpenEditor";

    String SAVE_TO_IGNORE_SELECTION = "saveToIgnoreSelection";

    int DEFAULT_TAB_WIDTH = 4;

    /** last used working set import/export file */
    String LAST_USED_WS_FILE = "lastUsedWsFile";

    /** last opened external file (compare/replace with...) */
    String LAST_OPENED_EXTERNAL_FILE = "lastOpenedExternalFile";

    /** base64 line length for encoding */
    String BASE64_LINE_LENGTH = "base64LineLength";

    /** to convert all characters to unicode, even ascii */
    String UNICODIFY_ALL = "unicodifyAll";

    /** to split lines on base64 encoding */
    String BASE64_SPLIT_LINE = "base64SplitLine";
}
