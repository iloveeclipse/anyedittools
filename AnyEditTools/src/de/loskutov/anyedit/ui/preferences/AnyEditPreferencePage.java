/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.actions.AbstractTextAction;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.TextUtil;

/**
 *
 */
public class AnyEditPreferencePage extends PreferencePage implements
IWorkbenchPreferencePage, SelectionListener {

    private static final String DEFAULT_BASE64_LENGTH = "" + TextUtil.DEFAULT_BASE64_LINE_LENGTH;

    private static final String DEFAULT_TAB_WIDTH = "2";

    private static final String DEFAULT_NEW_FILTER_TEXT = "";

    protected Text tabWidthText;

    protected Text base64LineLengthText;

    protected Text disallowedInPathText;

    protected Text requiredInPathText;

    protected Button requiredInPathEnabledCheck;

    protected Button useJavaTabsCheck;

    protected Button useModulo4TabsCheck;

    protected Button saveDirtyBufferCheck;

    protected Button removeTrailingSpacesCheck;

    protected Button replaceAllTabsCheck;

    protected Button replaceAllSpacesCheck;

    protected Button preserveEntitiesCheck;

    protected Button unicodifyAllCheck;

    protected Button base64SplitLineCheck;

    protected Button addSaveAllCheck;

    protected Button removePrintCheck;

    protected Button hideOpenTypeCheck;

    protected Group saveComposite;

    protected Button saveAndTrimCheck;

    protected Button saveAndAddLineCheck;

    protected Button askBeforeConvertAllCheck;

    protected Button saveAndConvertCheck;

    protected Group convertChoiceComposite;

    protected Button convertTabsOnSaveRadio;

    protected Button convertSpacesOnSaveRadio;

    protected TabFolder tabFolder;

    private Color red;

    protected FilterContentProvider fileFilterContentProvider;

    private CheckboxTableViewer filterViewer;

    private TableEditor tableEditor;

    private Table filterTable;

    private Label tableLabel;

    private Button addFilterButton;

    protected Button removeFilterButton;

    private Button enableAllButton;

    private Button disableAllButton;

    protected Text editorText;

    private Filter newFilter;

    private TableItem newTableItem;

    protected String invalidEditorText;

    private Button includeDerivedCheck;

    private Button showSaveToDialogCheck;

    private Button saveToNoSelectionCheck;

    private Button saveToOpenEditorCheck;



    private Button useWorkspaceScopeCheck;

    public AnyEditPreferencePage() {
        super();
        setPreferenceStore(AnyEditToolsPlugin.getDefault().getPreferenceStore());
    }

    /*
     * @see PreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent) {

        tabFolder = new TabFolder(parent, SWT.TOP);

        createTabAutoSave();
        createTabConvert();
        createTabOpen();
        createTabSave();
        createTabMisc();

        return tabFolder;
    }

    private void createTabSave() {
        GridLayout layout;
        GridData gridData;
        TabItem tabOpen = new TabItem(tabFolder, SWT.NONE);
        tabOpen.setText(Messages.pref_tab_saveTo);

        Composite defPanel3 = createContainer(tabFolder);
        tabOpen.setControl(defPanel3);

        IPreferenceStore store = getPreferenceStore();

        Group saveToComposite = new Group(defPanel3, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        saveToComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        saveToComposite.setLayoutData(gridData);
        saveToComposite.setText(Messages.pref_saveToIntro);

        saveToOpenEditorCheck = createLabeledCheck(Messages.pref_saveToOpenEditor,
                Messages.pref_saveToOpenEditorTip,
                store.getBoolean(IAnyEditConstants.SAVE_TO_OPEN_EDITOR),
                saveToComposite);

        Composite tmp = new Composite(saveToComposite, SWT.SHADOW_ETCHED_IN);// SWT.SHADOW_NONE
        layout = new GridLayout();
        layout.numColumns = 2;

        tmp.setLayout(layout);
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        tmp.setLayoutData(gridData);
        //        tmp.setText(Messages.pref_convertChoiceIntro);

        showSaveToDialogCheck = createLabeledCheck(Messages.pref_saveToShowOptions,
                Messages.pref_saveToShowOptionsTip,
                store.getBoolean(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS),
                tmp);
        saveToNoSelectionCheck = createLabeledCheck(Messages.pref_saveToNoSelection,
                Messages.pref_saveToNoSelectionTip,
                store.getBoolean(IAnyEditConstants.SAVE_TO_IGNORE_SELECTION),
                saveToComposite);
    }

    private void createTabOpen() {
        GridLayout layout;
        GridData gridData;
        TabItem tabOpen = new TabItem(tabFolder, SWT.NONE);
        tabOpen.setText(Messages.pref_tab_open);

        Composite defPanel3 = createContainer(tabFolder);
        tabOpen.setControl(defPanel3);

        IPreferenceStore store = getPreferenceStore();

        hideOpenTypeCheck = createLabeledCheck(Messages.pref_hideOpenType,
                Messages.pref_hideOpenTypeTip,
                store.getBoolean(IAnyEditConstants.HIDE_OPEN_TYPE_ACTION),
                defPanel3);


        Group searchComposite = new Group(defPanel3, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        searchComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        searchComposite.setLayoutData(gridData);
        searchComposite.setText(Messages.pref_searchFileIntro);

        includeDerivedCheck = createLabeledCheck(Messages.pref_includeDerived,
                Messages.pref_includeDerivedTip,
                store.getBoolean(IAnyEditConstants.INCLUDE_DERIVED_RESOURCES),
                searchComposite);

        useWorkspaceScopeCheck = createLabeledCheck(Messages.pref_useWorkspaceScope,
                Messages.pref_useWorkspaceScopeTip,
                store.getBoolean(IAnyEditConstants.USE_WORKSPACE_SCOPE_FOR_SEARCH),
                searchComposite);

        Group openFileComposite = new Group(defPanel3, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        openFileComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        openFileComposite.setLayoutData(gridData);

        openFileComposite.setText(Messages.pref_openFileIntro);

        disallowedInPathText = createLabeledText(Messages.pref_disallowedInPath,
                Messages.pref_disallowedInPathTip,
                store.getString(IAnyEditConstants.CHARACTERS_DISALLOWED_IN_PATH),
                openFileComposite, true, SWT.NONE);

        requiredInPathEnabledCheck = createLabeledCheck(Messages.pref_requiredInPathEnabled,
                Messages.pref_requiredInPathEnabledTip,
                store.getBoolean(IAnyEditConstants.USE_REQUIRED_IN_PATH_CHARACTERS),
                openFileComposite);

        requiredInPathText = createLabeledText(Messages.pref_requiredInPath,
                Messages.pref_requiredInPathTip,
                store.getString(IAnyEditConstants.CHARACTERS_REQUIRED_IN_PATH),
                openFileComposite, true, SWT.NONE);


        requiredInPathEnabledCheck.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                requiredInPathText.setEditable(requiredInPathEnabledCheck.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                // ignored
            }
        });
        requiredInPathText.setEditable(requiredInPathEnabledCheck.getSelection());
    }

    private void createTabConvert() {
        GridLayout layout;
        GridData gridData;
        TabItem tabManual = new TabItem(tabFolder, SWT.NONE);
        tabManual.setText(Messages.pref_tab_convert);

        Composite defPanel = createContainer(tabFolder);
        tabManual.setControl(defPanel);

        IPreferenceStore store = getPreferenceStore();

        saveDirtyBufferCheck = createLabeledCheck(Messages.pref_saveDirtyEditor,
                Messages.pref_saveDirtyEditorTip,
                store.getBoolean(IAnyEditConstants.SAVE_DIRTY_BUFFER), defPanel);

        Group spacesComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        spacesComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        spacesComposite.setLayoutData(gridData);

        spacesComposite.setText(Messages.pref_spacesIntro);

        tabWidthText = createLabeledText(Messages.pref_tabWidth, Messages.pref_tabWidthTip,
                store.getString(IAnyEditConstants.EDITOR_TAB_WIDTH), spacesComposite,
                false, SWT.NONE);
        tabWidthText.setTextLimit(2);
        tabWidthText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String number = ((Text) e.widget).getText();
                number = number == null ? DEFAULT_TAB_WIDTH : number.trim();
                try {
                    int value = Integer.parseInt(number);
                    if (value <= 0) {
                        tabWidthText.setText(DEFAULT_TAB_WIDTH);
                    }
                } catch (NumberFormatException ex) {
                    tabWidthText.setText(DEFAULT_TAB_WIDTH);
                }
            }
        });

        useJavaTabsCheck = createLabeledCheck(Messages.pref_javaTabWidthForJava,
                Messages.pref_javaTabWidthForJavaTip,
                store.getBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA),
                spacesComposite);

        useModulo4TabsCheck = createLabeledCheck(Messages.pref_useModulo4Tabs,
                Messages.pref_useModulo4TabsTip,
                store.getBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE),
                spacesComposite);


        removeTrailingSpacesCheck = createLabeledCheck(Messages.pref_removeTrailingSpaces,
                Messages.pref_removeTrailingSpacesTip,
                store.getBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES),
                spacesComposite);

        replaceAllTabsCheck = createLabeledCheck(Messages.pref_replaceAllTabs,
                Messages.pref_replaceAllTabsTip,
                store.getBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES),
                spacesComposite);

        replaceAllSpacesCheck = createLabeledCheck(Messages.pref_replaceAllSpaces,
                Messages.pref_replaceAllSpacesTip,
                store.getBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS),
                spacesComposite);

        // -------------------------------------------------------------------------------

        Group entitiesComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        entitiesComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        entitiesComposite.setLayoutData(gridData);
        entitiesComposite.setText(Messages.pref_entitiesIntro);

        preserveEntitiesCheck = createLabeledCheck(Messages.pref_preserveEntities,
                Messages.pref_preserveEntitiesTip,
                store.getBoolean(IAnyEditConstants.PRESERVE_ENTITIES), entitiesComposite);

        // -------------------------------------------------------------------------------

        Group base64Composite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        base64Composite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        base64Composite.setLayoutData(gridData);
        base64Composite.setText(Messages.pref_base64Intro);

        base64SplitLineCheck = createLabeledCheck(Messages.pref_base64SplitLine,
                Messages.pref_base64LineLengthTip,
                store.getBoolean(IAnyEditConstants.BASE64_SPLIT_LINE), base64Composite);

        base64LineLengthText = createLabeledText(Messages.pref_base64LineLength,
                Messages.pref_base64LineLengthTip,
                store.getString(IAnyEditConstants.BASE64_LINE_LENGTH), base64Composite, false, SWT.NONE);
        base64LineLengthText.setTextLimit(3);
        base64LineLengthText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String number = ((Text) e.widget).getText();
                number = number == null ? DEFAULT_BASE64_LENGTH : number.trim();
                try {
                    int value = Integer.parseInt(number);
                    if (value <= 0) {
                        base64LineLengthText.setText(DEFAULT_BASE64_LENGTH);
                    }
                } catch (NumberFormatException ex) {
                    base64LineLengthText.setText(DEFAULT_BASE64_LENGTH);
                }
            }
        });

        // -------------------------------------------------------------------------------

        Group unicodeComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        unicodeComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        unicodeComposite.setLayoutData(gridData);
        unicodeComposite.setText(Messages.pref_unicodifyIntro);

        unicodifyAllCheck = createLabeledCheck(Messages.pref_unicodifyAll,
                Messages.pref_unicodifyAllTip,
                store.getBoolean(IAnyEditConstants.UNICODIFY_ALL), unicodeComposite);

    }

    private void createTabMisc() {
        GridLayout layout;
        GridData gridData;
        TabItem tabManual = new TabItem(tabFolder, SWT.NONE);
        tabManual.setText(Messages.pref_tab_misc);

        Composite defPanel = createContainer(tabFolder);
        tabManual.setControl(defPanel);

        IPreferenceStore store = getPreferenceStore();


        Group toolbarComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        toolbarComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        toolbarComposite.setLayoutData(gridData);
        toolbarComposite.setText(Messages.pref_toolbarIntro);

        addSaveAllCheck = createLabeledCheck(Messages.pref_addSaveAll,
                Messages.pref_addSaveAllTip,
                store.getBoolean(IAnyEditConstants.ADD_SAVE_ALL_TO_TOOLBAR), toolbarComposite);

        removePrintCheck = createLabeledCheck(Messages.pref_removePrint,
                Messages.pref_removePrintTip,
                store.getBoolean(IAnyEditConstants.REMOVE_PRINT_FROM_TOOLBAR), toolbarComposite);


        Group ratingComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        ratingComposite.setLayout(layout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        ratingComposite.setLayoutData(gridData);
        ratingComposite.setText("Rate AnyEdit plugin on EPIC");

        RateIt.createTextArea(ratingComposite);
    }

    private void createTabAutoSave() {
        TabItem tabAuto = new TabItem(tabFolder, SWT.NONE);
        tabAuto.setText(Messages.pref_tab_auto);

        Composite defPanel = createContainer(tabFolder);
        tabAuto.setControl(defPanel);

        boolean isSaveHookEnabled = AnyEditToolsPlugin.isSaveHookInitialized();

        saveComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        GridLayout layout = new GridLayout();
        saveComposite.setLayout(layout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL
                | GridData.GRAB_HORIZONTAL);
        saveComposite.setLayoutData(gridData);
        String saveGroupText = Messages.pref_saveIntro;
        if (!isSaveHookEnabled) {
            saveComposite.setToolTipText(saveGroupText);
            Label label = new Label(saveComposite, SWT.WRAP);
            red = new Color(tabFolder.getDisplay(), 255, 0, 0);
            label.setForeground(red);
            label.setText(Messages.pref_saveHookNotEnabled);
        }
        saveComposite.setText(saveGroupText);


        Composite firstRow = new Composite(saveComposite, SWT.NONE);
        layout = new GridLayout(2, false);
        firstRow.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gridData.horizontalIndent = -5;
        firstRow.setLayoutData(gridData);


        IPreferenceStore store = getPreferenceStore();
        saveAndTrimCheck = createLabeledCheck(Messages.pref_saveAndTrim,
                Messages.pref_saveAndTrimTip,
                store.getBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED), firstRow);

        saveAndAddLineCheck = createLabeledCheck(Messages.pref_saveAndAddLine,
                Messages.pref_saveAndAddLineTip,
                store.getBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE), firstRow);

        saveAndConvertCheck = createLabeledCheck(Messages.pref_saveAndConvert,
                Messages.pref_saveAndConvertTip,
                store.getBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED),
                saveComposite);

        convertChoiceComposite = new Group(saveComposite, SWT.SHADOW_ETCHED_IN);// SWT.SHADOW_NONE
        layout = new GridLayout();
        layout.numColumns = 2;

        convertChoiceComposite.setLayout(layout);
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        convertChoiceComposite.setLayoutData(gridData);
        convertChoiceComposite.setText(Messages.pref_convertChoiceIntro);

        boolean convertTabsAction = AbstractTextAction.ACTION_ID_CONVERT_TABS
        .equals(store.getString(IAnyEditConstants.CONVERT_ACTION_ON_SAVE));

        convertTabsOnSaveRadio = createLabeledRadio(Messages.pref_convertTabsOnSave,
                Messages.pref_convertTabsOnSaveTip,
                convertTabsAction, convertChoiceComposite);

        convertSpacesOnSaveRadio = createLabeledRadio(Messages.pref_convertSpacesOnSave,
                Messages.pref_convertSpacesOnSaveTip,
                !convertTabsAction, convertChoiceComposite);

        saveAndConvertCheck.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                convertTabsOnSaveRadio.setEnabled(saveAndConvertCheck.getSelection());
                convertSpacesOnSaveRadio.setEnabled(saveAndConvertCheck.getSelection());
                convertChoiceComposite.setEnabled(saveAndConvertCheck.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                // ignored
            }
        });

        convertTabsOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertSpacesOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertChoiceComposite.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        saveAndTrimCheck.setEnabled(isSaveHookEnabled);
        saveAndAddLineCheck.setEnabled(isSaveHookEnabled);
        saveAndConvertCheck.setEnabled(isSaveHookEnabled);
        saveComposite.setEnabled(isSaveHookEnabled);

        askBeforeConvertAllCheck = createLabeledCheck(Messages.pref_askBeforeConvertAll,
                Messages.pref_askBeforeConvertAllTip,
                store.getBoolean(IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER),
                defPanel);

        createExclusionGroup(defPanel);
    }

    private Control createExclusionGroup(Composite parent) {
        // PreferenceLinkArea contentTypeArea = new PreferenceLinkArea(parent, SWT.NONE,
        // "org.eclipse.ui.preferencePages.ContentTypes",
        // getText("AnyEditPreferencePage.linkToContentTypes"),
        // (IWorkbenchPreferenceContainer) getContainer(), null);
        //
        // GridData data = new GridData(GridData.FILL_HORIZONTAL |
        // GridData.GRAB_HORIZONTAL);
        // contentTypeArea.getControl().setLayoutData(data);

        Group exclGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
        exclGroup.setText(Messages.pref_group_exclude);
        exclGroup.setToolTipText(Messages.pref_group_excludeTip);
        GridLayout layout = new GridLayout();
        exclGroup.setLayout(layout);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL
                | GridData.GRAB_HORIZONTAL);
        exclGroup.setLayoutData(gridData);

        createFilterPreferences(exclGroup);
        return exclGroup;
    }

    protected static Composite createContainer(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        return composite;
    }

    /*
     * @see IWorkbenchPreferencePage#init(IWorkbench)
     */
    public void init(IWorkbench workbench) {
        // ignored
    }

    /*
     * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
     */
    public void widgetDefaultSelected(SelectionEvent selectionEvent) {
        widgetSelected(selectionEvent);
    }

    /*
     * @see SelectionListener#widgetSelected(SelectionEvent)
     */
    public void widgetSelected(SelectionEvent selectionEvent) {
        // ignored
    }

    public boolean performOk() {
        IPreferenceStore store = getPreferenceStore();

        fileFilterContentProvider.saveFilters();

        store.setValue(IAnyEditConstants.EDITOR_TAB_WIDTH, tabWidthText.getText());
        store.setValue(IAnyEditConstants.CHARACTERS_REQUIRED_IN_PATH, requiredInPathText
                .getText());
        store.setValue(IAnyEditConstants.CHARACTERS_DISALLOWED_IN_PATH,
                disallowedInPathText.getText());
        store.setValue(IAnyEditConstants.USE_REQUIRED_IN_PATH_CHARACTERS,
                requiredInPathEnabledCheck.getSelection());
        store.setValue(IAnyEditConstants.HIDE_OPEN_TYPE_ACTION,
                hideOpenTypeCheck.getSelection());
        store.setValue(IAnyEditConstants.INCLUDE_DERIVED_RESOURCES,
                includeDerivedCheck.getSelection());
        store.setValue(IAnyEditConstants.USE_WORKSPACE_SCOPE_FOR_SEARCH,
                useWorkspaceScopeCheck.getSelection());

        store.setValue(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA, useJavaTabsCheck
                .getSelection());
        store.setValue(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE, useModulo4TabsCheck
                .getSelection());
        store.setValue(IAnyEditConstants.SAVE_DIRTY_BUFFER, saveDirtyBufferCheck
                .getSelection());
        store.setValue(IAnyEditConstants.REMOVE_TRAILING_SPACES,
                removeTrailingSpacesCheck.getSelection());
        store.setValue(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES,
                replaceAllTabsCheck.getSelection());
        store.setValue(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS,
                replaceAllSpacesCheck.getSelection());
        store.setValue(IAnyEditConstants.PRESERVE_ENTITIES, preserveEntitiesCheck
                .getSelection());
        store.setValue(IAnyEditConstants.ADD_SAVE_ALL_TO_TOOLBAR, addSaveAllCheck
                .getSelection());
        store.setValue(IAnyEditConstants.REMOVE_PRINT_FROM_TOOLBAR, removePrintCheck
                .getSelection());
        store.setValue(IAnyEditConstants.SAVE_AND_TRIM_ENABLED, saveAndTrimCheck
                .getSelection());
        store.setValue(IAnyEditConstants.SAVE_AND_ADD_LINE, saveAndAddLineCheck
                .getSelection());
        store.setValue(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED, saveAndConvertCheck
                .getSelection());
        store.setValue(IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER,
                askBeforeConvertAllCheck.getSelection());

        store.setValue(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS,
                showSaveToDialogCheck.getSelection());
        store.setValue(IAnyEditConstants.SAVE_TO_OPEN_EDITOR,
                saveToOpenEditorCheck.getSelection());
        store.setValue(IAnyEditConstants.SAVE_TO_IGNORE_SELECTION,
                saveToNoSelectionCheck.getSelection());

        store.setValue(IAnyEditConstants.BASE64_LINE_LENGTH,
                base64LineLengthText.getText());
        store.setValue(IAnyEditConstants.UNICODIFY_ALL,
                unicodifyAllCheck.getSelection());
        store.setValue(IAnyEditConstants.BASE64_SPLIT_LINE,
                base64SplitLineCheck.getSelection());


        if (convertSpacesOnSaveRadio.getSelection()) {
            store.setValue(IAnyEditConstants.CONVERT_ACTION_ON_SAVE,
                    AbstractTextAction.ACTION_ID_CONVERT_SPACES);
        } else {
            store.setValue(IAnyEditConstants.CONVERT_ACTION_ON_SAVE,
                    AbstractTextAction.ACTION_ID_CONVERT_TABS);
        }

        TextUtil.updateTextUtils();
        return true;
    }


    protected static Text createLabeledText(String title, String tooltip, String value, Composite defPanel,
            boolean fillAllSpace, int style) {
        Composite commonPanel = new Composite(defPanel, SWT.NONE);
        GridData gridData = new GridData (SWT.FILL, SWT.FILL, true, true);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);

        Label label = new Label(commonPanel, SWT.LEFT);
        label.setText(title);
        label.setToolTipText(tooltip);

        Text fText = new Text(commonPanel, SWT.SHADOW_IN | SWT.BORDER | style);
        if (fillAllSpace) {
            gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
            fText.setLayoutData(gridData);
        } else {
            gridData = new GridData();
            gridData.widthHint = 20;
            fText.setLayoutData(gridData);
        }
        fText.setText(value);
        fText.setToolTipText(tooltip);
        return fText;
    }

    protected static Button createLabeledCheck(String title, String tooltip, boolean value, Composite defPanel) {
        Button fButton = new Button(defPanel, SWT.CHECK | SWT.LEFT);
        GridData data = new GridData();
        fButton.setLayoutData(data);
        fButton.setText(title);
        fButton.setSelection(value);
        fButton.setToolTipText(tooltip);
        return fButton;
    }

    protected static Button createLabeledRadio(String title, String tooltip, boolean value, Composite defPanel) {
        Button fButton = new Button(defPanel, SWT.RADIO);
        fButton.setText(title);
        fButton.setSelection(value);
        fButton.setToolTipText(tooltip);
        return fButton;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    protected void performDefaults() {
        super.performDefaults();

        fileFilterContentProvider.setDefaults();

        IPreferenceStore store = getPreferenceStore();
        tabWidthText.setText(store.getDefaultString(IAnyEditConstants.EDITOR_TAB_WIDTH));
        disallowedInPathText.setText(store
                .getDefaultString(IAnyEditConstants.CHARACTERS_DISALLOWED_IN_PATH));
        requiredInPathText.setText(store
                .getDefaultString(IAnyEditConstants.CHARACTERS_REQUIRED_IN_PATH));
        requiredInPathEnabledCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.USE_REQUIRED_IN_PATH_CHARACTERS));
        hideOpenTypeCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.HIDE_OPEN_TYPE_ACTION));
        includeDerivedCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.INCLUDE_DERIVED_RESOURCES));
        useWorkspaceScopeCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.USE_WORKSPACE_SCOPE_FOR_SEARCH));
        requiredInPathText.setEditable(requiredInPathEnabledCheck.getSelection());

        useJavaTabsCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA));

        useModulo4TabsCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE));

        saveDirtyBufferCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_DIRTY_BUFFER));
        removeTrailingSpacesCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES));
        replaceAllTabsCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES));
        replaceAllSpacesCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS));
        preserveEntitiesCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.PRESERVE_ENTITIES));
        addSaveAllCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.ADD_SAVE_ALL_TO_TOOLBAR));
        removePrintCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.REMOVE_PRINT_FROM_TOOLBAR));
        saveAndTrimCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED));
        saveAndAddLineCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE));
        saveAndConvertCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED));
        askBeforeConvertAllCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER));

        showSaveToDialogCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS));
        saveToOpenEditorCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_TO_OPEN_EDITOR));
        saveToNoSelectionCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.SAVE_TO_IGNORE_SELECTION));

        base64LineLengthText.setText(store
                .getDefaultString(IAnyEditConstants.BASE64_LINE_LENGTH));
        unicodifyAllCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.UNICODIFY_ALL));
        base64SplitLineCheck.setSelection(store
                .getDefaultBoolean(IAnyEditConstants.BASE64_SPLIT_LINE));

        boolean convertTabsAction = AbstractTextAction.ACTION_ID_CONVERT_TABS
        .equals(getPreferenceStore().getDefaultString(
                IAnyEditConstants.CONVERT_ACTION_ON_SAVE));

        boolean isSaveHookEnabled = AnyEditToolsPlugin.isSaveHookInitialized();
        convertTabsOnSaveRadio.setSelection(convertTabsAction);
        convertSpacesOnSaveRadio.setSelection(!convertTabsAction);

        convertTabsOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertSpacesOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertChoiceComposite.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
    }

    public void dispose() {
        if (tabFolder != null) {
            tabFolder.dispose();
        }
        if (red != null && !red.isDisposed()) {
            red.dispose();
        }
        super.dispose();
    }

    /**
     * Create a group to contain the step filter related widgetry
     */
    private void createFilterPreferences(Composite parent) {
        // top level container
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        container.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_FILL);
        container.setLayoutData(gd);

        // table label
        tableLabel = new Label(container, SWT.WRAP);
        tableLabel.setText(Messages.pref_Defined_file_filters);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        tableLabel.setLayoutData(gd);

        filterTable = new Table(container, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        filterTable.setHeaderVisible(false);
        // fFilterTable.setLinesVisible(true);
        filterTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableLayout tlayout = new TableLayout();
        tlayout.addColumnData(new ColumnWeightData(100, true));
        filterTable.setLayout(tlayout);

        TableColumn tableCol = new TableColumn(filterTable, SWT.LEFT);
        tableCol.setResizable(true);

        filterViewer = new CheckboxTableViewer(filterTable);
        tableEditor = new TableEditor(filterTable);
        filterViewer.setLabelProvider(new FilterLabelProvider());
        filterViewer.setComparator(new FilterViewerSorter());
        fileFilterContentProvider = new FilterContentProvider(filterViewer);
        filterViewer.setContentProvider(fileFilterContentProvider);
        // @todo table width input just needs to be non-null
        filterViewer.setInput(this);

        filterViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                Filter filter = (Filter) event.getElement();
                fileFilterContentProvider.toggleFilter(filter);
            }
        });
        filterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection.isEmpty()) {
                    removeFilterButton.setEnabled(false);
                } else {
                    removeFilterButton.setEnabled(true);
                }
            }
        });

        createFilterButtons(container);
    }

    private void createFilterButtons(Composite container) {
        // button container
        Composite buttonContainer = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        buttonContainer.setLayoutData(gd);
        GridLayout buttonLayout = new GridLayout();
        buttonLayout.numColumns = 1;
        buttonLayout.marginHeight = 0;
        buttonLayout.marginWidth = 0;
        buttonContainer.setLayout(buttonLayout);

        // Add filter button
        addFilterButton = new Button(buttonContainer, SWT.PUSH);
        addFilterButton.setText(Messages.pref_Add_filter);
        addFilterButton.setToolTipText(Messages.pref_Add_filterTip);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        addFilterButton.setLayoutData(gd);
        addFilterButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                editFilter();
            }
        });

        // Remove button
        removeFilterButton = new Button(buttonContainer, SWT.PUSH);
        removeFilterButton.setText(Messages.pref_RemoveFilter);
        removeFilterButton.setToolTipText(Messages.pref_RemoveFilterTip);
        gd = getButtonGridData(removeFilterButton);
        removeFilterButton.setLayoutData(gd);
        removeFilterButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                removeFilters();
            }
        });
        removeFilterButton.setEnabled(false);

        enableAllButton = new Button(buttonContainer, SWT.PUSH);
        enableAllButton.setText(Messages.pref_Enable_all);
        enableAllButton.setToolTipText(Messages.pref_Enable_allTip);
        gd = getButtonGridData(enableAllButton);
        enableAllButton.setLayoutData(gd);
        enableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(true);
            }
        });

        disableAllButton = new Button(buttonContainer, SWT.PUSH);
        disableAllButton.setText(Messages.pref_Disable_all);
        disableAllButton.setToolTipText(Messages.pref_Disable_allTip);
        gd = getButtonGridData(disableAllButton);
        disableAllButton.setLayoutData(gd);
        disableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(false);
            }
        });

    }

    protected static GridData getButtonGridData(Button button) {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING);
        GC gc = new GC(button);
        gc.setFont(button.getFont());
        FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();
        int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics,
                IDialogConstants.BUTTON_WIDTH);
        gd.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT,
                true).x);

        // gd.heightHint = Dialog.convertVerticalDLUsToPixels(fontMetrics,
        // IDialogConstants.BUTTON_HEIGHT);
        return gd;
    }

    protected void checkAllFilters(boolean check) {

        Object[] filters = fileFilterContentProvider.getElements(null);
        for (int i = 0; i != filters.length; i++) {
            ((Filter) filters[i]).setChecked(check);
        }

        filterViewer.setAllChecked(check);
    }

    /**
     * Create a new filter in the table (with the default 'new filter' value), then open
     * up an in-place editor on it.
     */
    protected void editFilter() {
        // if a previous edit is still in progress, finish it
        if (editorText != null) {
            validateChangeAndCleanup();
        }

        newFilter = fileFilterContentProvider.addFilter(DEFAULT_NEW_FILTER_TEXT,
                true);
        newTableItem = filterTable.getItem(0);

        // create & configure Text widget for editor
        // Fix for bug 1766. Border behavior on for text fields varies per platform.
        // On Motif, you always get a border, on other platforms,
        // you don't. Specifying a border on Motif results in the characters
        // getting pushed down so that only there very tops are visible. Thus,
        // we have to specify different style constants for the different platforms.
        int textStyles = SWT.SINGLE | SWT.LEFT;
        if (!SWT.getPlatform().equals("motif")) {
            textStyles |= SWT.BORDER;
        }
        editorText = new Text(filterTable, textStyles);
        GridData gd = new GridData(GridData.FILL_BOTH);
        editorText.setLayoutData(gd);

        // set the editor
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.grabHorizontal = true;
        tableEditor.setEditor(editorText, newTableItem, 0);

        // get the editor ready to use
        editorText.setText(newFilter.getName());
        editorText.selectAll();
        setEditorListeners(editorText);
        editorText.setFocus();
    }

    private void setEditorListeners(Text text) {
        // CR means commit the changes, ESC means abort and don't commit
        text.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.CR) {
                    if (invalidEditorText != null) {
                        String infoText = Messages.pref_Invalid_file_filter;
                        if (!invalidEditorText.equals(editorText.getText())
                                && !infoText.equals(editorText.getText())) {
                            validateChangeAndCleanup();
                        } else {
                            editorText.setText(infoText);
                        }
                        invalidEditorText = null;
                    } else {
                        validateChangeAndCleanup();
                    }
                } else if (event.character == SWT.ESC) {
                    removeNewFilter();
                    cleanupEditor();
                }
            }
        });
        // Consider loss of focus on the editor to mean the same as CR
        text.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent event) {
                if (invalidEditorText != null) {
                    String infoText = Messages.pref_Invalid_file_filter;
                    if (!invalidEditorText.equals(editorText.getText())
                            && !infoText.equals(editorText.getText())) {
                        validateChangeAndCleanup();
                    } else {
                        editorText.setText(infoText);
                    }
                    invalidEditorText = null;
                } else {
                    validateChangeAndCleanup();
                }
            }
        });
        // Consume traversal events from the text widget so that CR doesn't
        // traverse away to dialog's default button. Without this, hitting
        // CR in the text field closes the entire dialog.
        text.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
    }

    protected void validateChangeAndCleanup() {
        String trimmedValue = editorText.getText().trim();
        // if the new value is blank, remove the filter
        if (trimmedValue.length() < 1) {
            removeNewFilter();
        }
        // if it's invalid, beep and leave sitting in the editor
        else if (!validateEditorInput(trimmedValue)) {
            invalidEditorText = trimmedValue;
            editorText.setText(Messages.pref_Invalid_file_filter);
            getShell().getDisplay().beep();
            return;
            // otherwise, commit the new value if not a duplicate
        } else {

            Object[] filters = fileFilterContentProvider.getElements(null);
            for (int i = 0; i < filters.length; i++) {
                Filter filter = (Filter) filters[i];
                if (filter.getName().equals(trimmedValue)) {
                    removeNewFilter();
                    cleanupEditor();
                    return;
                }
            }
            newTableItem.setText(trimmedValue);
            newFilter.setName(trimmedValue);
            filterViewer.refresh();
        }
        cleanupEditor();
    }

    /**
     * Cleanup all widgetry & resources used by the in-place editing
     */
    protected void cleanupEditor() {
        if (editorText != null) {
            newFilter = null;
            newTableItem = null;
            tableEditor.setEditor(null, null, 0);
            editorText.dispose();
            editorText = null;
        }
    }

    protected void removeNewFilter() {
        fileFilterContentProvider.removeFilters(new Object[] { newFilter });
    }

    /**
     * A valid filter is either *.[\w] or [\w].* or [\w]
     */
    protected static boolean validateEditorInput(String trimmedValue) {
        char firstChar = trimmedValue.charAt(0);
        if (firstChar == '*' && trimmedValue.length() == 1) {
            return false;
        }
        if (firstChar == '*') {
            // '*' should be followed by '.'
            if (trimmedValue.charAt(1) != '.') {
                return false;
            }
        }
        char lastChar = trimmedValue.charAt(trimmedValue.length() - 1);
        if (lastChar == '*') {
            // '*' should be preceeded by '.' and it should exist only once
            if (trimmedValue.charAt(trimmedValue.length() - 2) != '.') {
                return false;
            }
        }
        if (TextUtil.count(trimmedValue, '*') > 1) {
            return false;
        }
        return true;
    }

    protected void removeFilters() {
        IStructuredSelection selection = (IStructuredSelection) filterViewer
        .getSelection();
        fileFilterContentProvider.removeFilters(selection.toArray());
    }

    /**
     * Serializes the array of strings into one comma separated string.
     *
     * @param list
     *            array of strings
     * @return a single string composed of the given list
     */
    public static String serializeList(String[] list) {
        if (list == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < list.length; i++) {
            if (i > 0) {
                buffer.append(',');
            }
            buffer.append(list[i]);
        }
        return buffer.toString();
    }

    protected void updateActions() {
        if (enableAllButton != null) {
            boolean enabled = filterViewer.getTable().getItemCount() > 0;
            enableAllButton.setEnabled(enabled);
            disableAllButton.setEnabled(enabled);
        }
    }

    /**
     * Content provider for the table. Content consists of instances of StepFilter.
     */
    protected class FilterContentProvider implements IStructuredContentProvider {

        private final CheckboxTableViewer fViewer;

        private final List fFilters;

        public FilterContentProvider(CheckboxTableViewer viewer) {
            fViewer = viewer;
            List active = createActiveStepFiltersList();
            List inactive = createInactiveStepFiltersList();
            fFilters = new ArrayList(active.size() + inactive.size());
            populateList(inactive, false);
            populateList(active, true);
            updateActions();
        }

        public void setDefaults() {
            fViewer.remove(fFilters.toArray());
            List defaultlist = createDefaultStepFiltersList();
            fFilters.clear();
            populateList(defaultlist, true);
        }

        protected final void populateList(List list, boolean checked) {
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                String name = (String) iterator.next();
                addFilter(name, checked);
            }
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected final List createActiveStepFiltersList() {
            String[] strings = EclipseUtils.parseList(getPreferenceStore().getString(
                    IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST));
            return Arrays.asList(strings);
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected List createDefaultStepFiltersList() {
            String[] strings = EclipseUtils.parseList(getPreferenceStore()
                    .getDefaultString(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST));
            return Arrays.asList(strings);
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected final List createInactiveStepFiltersList() {
            String[] strings = EclipseUtils.parseList(getPreferenceStore().getString(
                    IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST));
            return Arrays.asList(strings);
        }

        public Filter addFilter(String name, boolean checked) {
            Filter filter = new Filter(name, checked);
            if (!fFilters.contains(filter)) {
                fFilters.add(filter);
                fViewer.add(filter);
                fViewer.setChecked(filter, checked);
            }
            updateActions();
            return filter;
        }

        public void saveFilters() {

            int filtersSize = fFilters.size();
            List active = new ArrayList(filtersSize);
            List inactive = new ArrayList(filtersSize);
            Iterator iterator = fFilters.iterator();
            while (iterator.hasNext()) {
                Filter filter = (Filter) iterator.next();
                String name = filter.getName();
                if (filter.isChecked()) {
                    active.add(name);
                } else {
                    inactive.add(name);
                }
            }
            String pref = serializeList((String[]) active.toArray(new String[active
                                                                             .size()]));
            IPreferenceStore prefStore = getPreferenceStore();
            prefStore.setValue(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST,
                    pref);
            pref = serializeList((String[]) inactive.toArray(new String[inactive.size()]));
            prefStore.setValue(IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST,
                    pref);
        }

        public void removeFilters(Object[] filters) {
            for (int i = 0; i < filters.length; i++) {
                Filter filter = (Filter) filters[i];
                fFilters.remove(filter);
            }
            fViewer.remove(filters);
            updateActions();
        }

        public void toggleFilter(Filter filter) {
            boolean newState = !filter.isChecked();
            filter.setChecked(newState);
            fViewer.setChecked(filter, newState);
        }

        /**
         * @see IStructuredContentProvider#getElements(Object)
         */
        public Object[] getElements(Object inputElement) {
            return fFilters.toArray();
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            /** ignored */
        }

        public void dispose() {
            /** ignored */
        }
    }

}

/**
 * Model object that represents a single entry in a filter table.
 */
final class Filter {

    private String fName;

    private boolean fChecked;

    public Filter(String name, boolean checked) {
        setName(name);
        setChecked(checked);
    }

    public String getName() {
        return fName;
    }

    public void setName(String name) {
        fName = name;
    }

    public boolean isChecked() {
        return fChecked;
    }

    public void setChecked(boolean checked) {
        fChecked = checked;
    }

    public boolean equals(Object o) {
        if (o instanceof Filter) {
            Filter other = (Filter) o;
            if (getName().equals(other.getName())) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return getName().hashCode();
    }
}

/**
 * Label provider for Filter model objects
 */
class FilterLabelProvider extends LabelProvider implements ITableLabelProvider {

    private final Image imgPkg = PlatformUI.getWorkbench().getSharedImages()
    .getImage(ISharedImages.IMG_OBJ_FILE);

    /**
     * @see ITableLabelProvider#getColumnText(Object, int)
     */
    public String getColumnText(Object object, int column) {
        if (column == 0) {
            return ((Filter) object).getName();
        }
        return "";
    }

    /**
     * @see ILabelProvider#getText(Object)
     */
    public String getText(Object element) {
        return ((Filter) element).getName();
    }

    /**
     * @see ITableLabelProvider#getColumnImage(Object, int)
     */
    public Image getColumnImage(Object object, int column) {
        return imgPkg;
    }
}

class FilterViewerSorter extends WorkbenchViewerComparator {
    public int compare(Viewer viewer, Object e1, Object e2) {
        ILabelProvider lprov = (ILabelProvider) ((ContentViewer) viewer)
        .getLabelProvider();
        String name1 = lprov.getText(e1);
        String name2 = lprov.getText(e2);
        if (name1 == null) {
            name1 = "";
        }
        if (name2 == null) {
            name2 = "";
        }
        if (name1.length() > 0 && name2.length() > 0) {
            char char1 = name1.charAt(name1.length() - 1);
            char char2 = name2.charAt(name2.length() - 1);
            if (char1 == '*' && char1 != char2) {
                return -1;
            }
            if (char2 == '*' && char2 != char1) {
                return 1;
            }
        }
        return name1.compareTo(name2);
    }
}
