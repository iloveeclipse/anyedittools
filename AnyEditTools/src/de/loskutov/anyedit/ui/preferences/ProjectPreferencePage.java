package de.loskutov.anyedit.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.actions.AbstractTextAction;
import de.loskutov.anyedit.util.EclipseUtils;

public class ProjectPreferencePage extends PropertyPage {

    private Color red;

    private TabFolder tabFolder;

    private Group saveComposite;

    private Button enableProjectCheck;

    private Button saveAndTrimCheck;

    private Button saveAndAddLineCheck;

    private Button saveAndConvertCheck;

    private Button useModulo4TabsCheck;

    private Group convertChoiceComposite;

    private Button convertTabsOnSaveRadio;

    private Button convertSpacesOnSaveRadio;

    private Label fTableLabel;

    private Table fFilterTable;

    private CheckboxTableViewer fFilterViewer;

    private TableEditor fTableEditor;

    private FilterContentProvider fStepFilterContentProvider;

    private Button fRemoveFilterButton;

    private Button fAddFilterButton;

    private Button fEnableAllButton;

    private Button fDisableAllButton;

    private TableItem fNewTableItem;

    private Filter fNewStepFilter;

    private Text fEditorText;

    private Text tabWidthText;

    private Button useJavaTabsCheck;

    private Button removeTrailingSpacesCheck;

    private Button replaceAllTabsCheck;

    private Button replaceAllSpacesCheck;

    private IEclipsePreferences prefs;

    private String fInvalidEditorText;

    private Link workspaceSettingsLink;

    public ProjectPreferencePage() {
        super();
    }

    private void initPreferences() {
        IScopeContext projectScope = new ProjectScope((IProject) getElement());
        prefs = projectScope.getNode(AnyEditToolsPlugin.getDefault().getBundle()
                .getSymbolicName());
    }

    protected Control createContents(Composite parent) {
        initPreferences();

        createWorkspaceButtons(parent);

        tabFolder = new TabFolder(parent, SWT.TOP);

        createTabAutoSave();
        createTabConvert();
        setProjectEnabled(enableProjectCheck.getSelection());
        return tabFolder;
    }

    private void createWorkspaceButtons(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        boolean projectPropsEnabled = prefs.getBoolean(
                IAnyEditConstants.PROJECT_PROPS_ENABLED, false);

        enableProjectCheck = AnyEditPreferencePage.createLabeledCheck(
                "Enable project specific settings",
                "These settings would be used for all files from the current project",
                projectPropsEnabled, composite);

        enableProjectCheck.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                setProjectEnabled(enableProjectCheck.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                // ignored
            }
        });

        workspaceSettingsLink = createLink(composite, "Configure Workspace Settings...");
        workspaceSettingsLink
                .setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
    }

    private Link createLink(Composite composite, String text) {
        Link link = new Link(composite, SWT.NONE);
        link.setFont(composite.getFont());
        link.setText("<A>" + text + "</A>");
        link.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                String id = "AnyEditPreferencePage";
                PreferencesUtil.createPreferenceDialogOn(getShell(), id,
                        new String[] { id }, null).open();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                String id = "AnyEditPreferencePage";
                PreferencesUtil.createPreferenceDialogOn(getShell(), id,
                        new String[] { id }, null).open();
            }
        });
        return link;
    }

    public boolean performOk() {
        prefs.putBoolean(IAnyEditConstants.PROJECT_PROPS_ENABLED, enableProjectCheck.getSelection());

        fStepFilterContentProvider.saveFilters();

        prefs.put(IAnyEditConstants.EDITOR_TAB_WIDTH, tabWidthText.getText());
        prefs.putBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA, useJavaTabsCheck
                .getSelection());
        prefs.putBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE,
                useModulo4TabsCheck.getSelection());

        prefs.putBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES,
                removeTrailingSpacesCheck.getSelection());
        prefs.putBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES,
                replaceAllTabsCheck.getSelection());
        prefs.putBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS,
                replaceAllSpacesCheck.getSelection());
        prefs.putBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED, saveAndTrimCheck
                .getSelection());
        prefs.putBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE, saveAndAddLineCheck
                .getSelection());
        prefs.putBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED, saveAndConvertCheck
                .getSelection());

        if (convertSpacesOnSaveRadio.getSelection()) {
            prefs.put(IAnyEditConstants.CONVERT_ACTION_ON_SAVE,
                    AbstractTextAction.ACTION_ID_CONVERT_SPACES);
        } else {
            prefs.put(IAnyEditConstants.CONVERT_ACTION_ON_SAVE,
                    AbstractTextAction.ACTION_ID_CONVERT_TABS);
        }

        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            AnyEditToolsPlugin.errorDialog("Couldn't save project preferences", e);
        }
        return true;
    }

    protected void performDefaults() {
        if (!enableProjectCheck.getSelection()) {
            return;
        }

        super.performDefaults();


        IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                .getPreferenceStore();
        tabWidthText.setText(defaultStore
                .getDefaultString(IAnyEditConstants.EDITOR_TAB_WIDTH));

        useJavaTabsCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA));

        useModulo4TabsCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE));

        removeTrailingSpacesCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES));
        replaceAllTabsCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES));
        replaceAllSpacesCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS));

        saveAndTrimCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED));
        saveAndAddLineCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE));


        saveAndConvertCheck.setSelection(defaultStore
                .getDefaultBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED));

        boolean convertTabsAction = AbstractTextAction.ACTION_ID_CONVERT_TABS
                .equals(defaultStore
                        .getDefaultString(IAnyEditConstants.CONVERT_ACTION_ON_SAVE));

        boolean isSaveHookEnabled = AnyEditToolsPlugin.isSaveHookInitialized();
        convertTabsOnSaveRadio.setSelection(convertTabsAction);
        convertSpacesOnSaveRadio.setSelection(!convertTabsAction);

        fStepFilterContentProvider.setDefaults();
        convertTabsOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertSpacesOnSaveRadio.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());
        convertChoiceComposite.setEnabled(isSaveHookEnabled
                && saveAndConvertCheck.getSelection());

    }

    private void createTabAutoSave() {
        TabItem tabAuto = new TabItem(tabFolder, SWT.NONE);
        tabAuto.setText(Messages.pref_tab_auto);

        Composite defPanel = AnyEditPreferencePage.createContainer(tabFolder);
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

        IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                .getPreferenceStore();

        saveAndTrimCheck = AnyEditPreferencePage.createLabeledCheck(
                Messages.pref_saveAndTrim, Messages.pref_saveAndTrimTip, prefs
                        .getBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED, defaultStore
                                .getBoolean(IAnyEditConstants.SAVE_AND_TRIM_ENABLED)),
                                firstRow);

        saveAndAddLineCheck = AnyEditPreferencePage.createLabeledCheck(
                Messages.pref_saveAndAddLine, Messages.pref_saveAndAddLineTip, prefs
                .getBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE, defaultStore
                        .getBoolean(IAnyEditConstants.SAVE_AND_ADD_LINE)),
                        firstRow);

        saveAndConvertCheck = AnyEditPreferencePage.createLabeledCheck(
                Messages.pref_saveAndConvert, Messages.pref_saveAndConvertTip,
                prefs.getBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED, defaultStore
                        .getBoolean(IAnyEditConstants.SAVE_AND_CONVERT_ENABLED)),
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
                .equals(prefs.get(IAnyEditConstants.CONVERT_ACTION_ON_SAVE, defaultStore
                        .getString(IAnyEditConstants.CONVERT_ACTION_ON_SAVE)));

        convertTabsOnSaveRadio = AnyEditPreferencePage.createLabeledRadio(
                Messages.pref_convertTabsOnSave, Messages.pref_convertTabsOnSaveTip,
                convertTabsAction, convertChoiceComposite);

        convertSpacesOnSaveRadio = AnyEditPreferencePage.createLabeledRadio(
                Messages.pref_convertSpacesOnSave, Messages.pref_convertSpacesOnSaveTip,
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

        createExclusionGroup(defPanel);
    }

    /**
     * Enable/disable all UI elements except for project props enabled check
     * @param selection
     */
    protected void setProjectEnabled(boolean selection) {
        tabFolder.setEnabled(selection);
        saveComposite.setEnabled(selection);
        saveAndTrimCheck.setEnabled(selection);
        saveAndAddLineCheck.setEnabled(selection);
        saveAndConvertCheck.setEnabled(selection);
        saveComposite.setEnabled(selection);
        convertChoiceComposite.setEnabled(selection);
        convertTabsOnSaveRadio.setEnabled(selection);
        convertSpacesOnSaveRadio.setEnabled(selection);
        fTableLabel.setEnabled(selection);
        fFilterTable.setEnabled(selection);
        tabWidthText.setEnabled(selection);
        useJavaTabsCheck.setEnabled(selection);
        useModulo4TabsCheck.setEnabled(selection);
        removeTrailingSpacesCheck.setEnabled(selection);
        replaceAllTabsCheck.setEnabled(selection);
        replaceAllSpacesCheck.setEnabled(selection);
        setFilterButtonsEnabled(selection);
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
        fTableLabel = new Label(container, SWT.WRAP);
        fTableLabel.setText(Messages.pref_Defined_file_filters);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        fTableLabel.setLayoutData(gd);

        fFilterTable = new Table(container, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        fFilterTable.setHeaderVisible(false);
        // fFilterTable.setLinesVisible(true);
        fFilterTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableLayout tlayout = new TableLayout();
        tlayout.addColumnData(new ColumnWeightData(100, true));
        fFilterTable.setLayout(tlayout);

        TableColumn tableCol = new TableColumn(fFilterTable, SWT.LEFT);
        tableCol.setResizable(true);

        fFilterViewer = new CheckboxTableViewer(fFilterTable);
        fTableEditor = new TableEditor(fFilterTable);
        fFilterViewer.setLabelProvider(new FilterLabelProvider());
        fFilterViewer.setComparator(new FilterViewerSorter());
        fStepFilterContentProvider = new FilterContentProvider(fFilterViewer);
        fFilterViewer.setContentProvider(fStepFilterContentProvider);
        // @todo table width input just needs to be non-null
        fFilterViewer.setInput(this);

        fFilterViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                Filter filter = (Filter) event.getElement();
                fStepFilterContentProvider.toggleFilter(filter);
            }
        });
        fFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection.isEmpty()) {
                    fRemoveFilterButton.setEnabled(false);
                } else {
                    fRemoveFilterButton.setEnabled(true);
                }
            }
        });

        createFilterButtons(container);
    }

    private void setFilterButtonsEnabled(boolean enabled) {
        fAddFilterButton.setEnabled(enabled);
        if (enabled) {
            fRemoveFilterButton.setEnabled(!fFilterViewer.getSelection().isEmpty());
        } else {
            fRemoveFilterButton.setEnabled(enabled);
        }
        fEnableAllButton.setEnabled(enabled);
        fDisableAllButton.setEnabled(enabled);

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
        fAddFilterButton = new Button(buttonContainer, SWT.PUSH);
        fAddFilterButton.setText(Messages.pref_Add_filter);
        fAddFilterButton.setToolTipText(Messages.pref_Add_filterTip);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        fAddFilterButton.setLayoutData(gd);
        fAddFilterButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                editFilter();
            }
        });

        // Remove button
        fRemoveFilterButton = new Button(buttonContainer, SWT.PUSH);
        fRemoveFilterButton.setText(Messages.pref_RemoveFilter);
        fRemoveFilterButton.setToolTipText(Messages.pref_RemoveFilterTip);
        gd = AnyEditPreferencePage.getButtonGridData(fRemoveFilterButton);
        fRemoveFilterButton.setLayoutData(gd);
        fRemoveFilterButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                removeFilters();
            }
        });
        fRemoveFilterButton.setEnabled(false);

        fEnableAllButton = new Button(buttonContainer, SWT.PUSH);
        fEnableAllButton.setText(Messages.pref_Enable_all);
        fEnableAllButton.setToolTipText(Messages.pref_Enable_allTip);
        gd = AnyEditPreferencePage.getButtonGridData(fEnableAllButton);
        fEnableAllButton.setLayoutData(gd);
        fEnableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(true);
            }
        });

        fDisableAllButton = new Button(buttonContainer, SWT.PUSH);
        fDisableAllButton.setText(Messages.pref_Disable_all);
        fDisableAllButton.setToolTipText(Messages.pref_Disable_allTip);
        gd = AnyEditPreferencePage.getButtonGridData(fDisableAllButton);
        fDisableAllButton.setLayoutData(gd);
        fDisableAllButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                checkAllFilters(false);
            }
        });

    }

    protected void checkAllFilters(boolean check) {

        Object[] filters = fStepFilterContentProvider.getElements(null);
        for (int i = 0; i != filters.length; i++) {
            ((Filter) filters[i]).setChecked(check);
        }

        fFilterViewer.setAllChecked(check);
    }

    protected void removeFilters() {
        IStructuredSelection selection = (IStructuredSelection) fFilterViewer
                .getSelection();
        fStepFilterContentProvider.removeFilters(selection.toArray());
    }

    /**
     * Cleanup all widgetry & resources used by the in-place editing
     */
    protected void cleanupEditor() {
        if (fEditorText != null) {
            fNewStepFilter = null;
            fNewTableItem = null;
            fTableEditor.setEditor(null, null, 0);
            fEditorText.dispose();
            fEditorText = null;
        }
    }

    protected void removeNewFilter() {
        fStepFilterContentProvider.removeFilters(new Object[] { fNewStepFilter });
    }

    protected void validateChangeAndCleanup() {
        String trimmedValue = fEditorText.getText().trim();
        // if the new value is blank, remove the filter
        if (trimmedValue.length() < 1) {
            removeNewFilter();
        }
        // if it's invalid, beep and leave sitting in the editor
        else if (!AnyEditPreferencePage.validateEditorInput(trimmedValue)) {
            fInvalidEditorText = trimmedValue;
            fEditorText.setText(Messages.pref_Invalid_file_filter);
            getShell().getDisplay().beep();
            return;
            // otherwise, commit the new value if not a duplicate
        } else {

            Object[] filters = fStepFilterContentProvider.getElements(null);
            for (int i = 0; i < filters.length; i++) {
                Filter filter = (Filter) filters[i];
                if (filter.getName().equals(trimmedValue)) {
                    removeNewFilter();
                    cleanupEditor();
                    return;
                }
            }
            fNewTableItem.setText(trimmedValue);
            fNewStepFilter.setName(trimmedValue);
            fFilterViewer.refresh();
        }
        cleanupEditor();
    }

    /**
     * Create a new filter in the table (with the default 'new filter' value), then open
     * up an in-place editor on it.
     */
    protected void editFilter() {
        // if a previous edit is still in progress, finish it
        if (fEditorText != null) {
            validateChangeAndCleanup();
        }

        fNewStepFilter = fStepFilterContentProvider.addFilter("", true);
        fNewTableItem = fFilterTable.getItem(0);

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
        fEditorText = new Text(fFilterTable, textStyles);
        GridData gd = new GridData(GridData.FILL_BOTH);
        fEditorText.setLayoutData(gd);

        // set the editor
        fTableEditor.horizontalAlignment = SWT.LEFT;
        fTableEditor.grabHorizontal = true;
        fTableEditor.setEditor(fEditorText, fNewTableItem, 0);

        // get the editor ready to use
        fEditorText.setText(fNewStepFilter.getName());
        fEditorText.selectAll();
        setEditorListeners(fEditorText);
        fEditorText.setFocus();
    }

    private void setEditorListeners(Text text) {
        // CR means commit the changes, ESC means abort and don't commit
        text.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.character == SWT.CR) {
                    if (fInvalidEditorText != null) {
                        String infoText = Messages.pref_Invalid_file_filter;
                        if (!fInvalidEditorText.equals(fEditorText.getText())
                                && !infoText.equals(fEditorText.getText())) {
                            validateChangeAndCleanup();
                        } else {
                            fEditorText.setText(infoText);
                        }
                        fInvalidEditorText = null;
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
                if (fInvalidEditorText != null) {
                    String infoText = Messages.pref_Invalid_file_filter;
                    if (!fInvalidEditorText.equals(fEditorText.getText())
                            && !infoText.equals(fEditorText.getText())) {
                        validateChangeAndCleanup();
                    } else {
                        fEditorText.setText(infoText);
                    }
                    fInvalidEditorText = null;
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

    protected void updateActions() {
        if (fEnableAllButton != null) {
            boolean enabled = fFilterViewer.getTable().getItemCount() > 0;
            fEnableAllButton.setEnabled(enabled);
            fDisableAllButton.setEnabled(enabled);
        }
    }

    /**
     * Content provider for the table. Content consists of instances of StepFilter.
     */
    protected class FilterContentProvider implements IStructuredContentProvider {

        private CheckboxTableViewer fViewer;

        private List fFilters;

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
            IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                    .getPreferenceStore();
            String[] strings = EclipseUtils.parseList(prefs.get(
                    IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST, defaultStore
                            .getString(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST)));
            return Arrays.asList(strings);
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected List createDefaultStepFiltersList() {
            IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                    .getPreferenceStore();
            String[] strings = EclipseUtils.parseList(defaultStore
                    .getDefaultString(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST));
            return Arrays.asList(strings);
        }

        /**
         * Returns a list of active step filters.
         *
         * @return list
         */
        protected final List createInactiveStepFiltersList() {
            IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                    .getPreferenceStore();
            String[] strings = EclipseUtils.parseList(prefs.get(
                    IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST, defaultStore
                            .getString(IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST)));
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
            String pref = AnyEditPreferencePage.serializeList((String[]) active
                    .toArray(new String[active.size()]));

            prefs.put(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST, pref);
            pref = AnyEditPreferencePage.serializeList((String[]) inactive
                    .toArray(new String[inactive.size()]));
            prefs.put(IAnyEditConstants.PREF_INACTIVE_FILTERS_LIST, pref);
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

    private void createTabConvert() {
        GridLayout layout;
        GridData gridData;
        TabItem tabManual = new TabItem(tabFolder, SWT.NONE);
        tabManual.setText(Messages.pref_tab_convert);

        Composite defPanel = AnyEditPreferencePage.createContainer(tabFolder);
        tabManual.setControl(defPanel);

        Group spacesComposite = new Group(defPanel, SWT.SHADOW_ETCHED_IN);
        layout = new GridLayout();
        spacesComposite.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        spacesComposite.setLayoutData(gridData);

        spacesComposite.setText(Messages.pref_spacesIntro);

        IPreferenceStore defaultStore = AnyEditToolsPlugin.getDefault()
                .getPreferenceStore();

        tabWidthText = AnyEditPreferencePage.createLabeledText(Messages.pref_tabWidth,
                Messages.pref_tabWidthTip, prefs.get(IAnyEditConstants.EDITOR_TAB_WIDTH,
                        defaultStore.getString(IAnyEditConstants.EDITOR_TAB_WIDTH)),
                spacesComposite, false, SWT.NONE);
        tabWidthText.setTextLimit(2);

        useJavaTabsCheck = AnyEditPreferencePage
                .createLabeledCheck(
                        Messages.pref_javaTabWidthForJava,
                        Messages.pref_javaTabWidthForJavaTip,
                        prefs
                                .getBoolean(
                                        IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA,
                                        defaultStore
                                                .getBoolean(IAnyEditConstants.USE_JAVA_TAB_WIDTH_FOR_JAVA)),
                        spacesComposite);

        useModulo4TabsCheck = AnyEditPreferencePage
        .createLabeledCheck(
                Messages.pref_useModulo4Tabs,
                Messages.pref_useModulo4TabsTip,
                prefs
                        .getBoolean(
                                IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE,
                                defaultStore
                                        .getBoolean(IAnyEditConstants.USE_MODULO_CALCULATION_FOR_TABS_REPLACE)),
                spacesComposite);

        removeTrailingSpacesCheck = AnyEditPreferencePage.createLabeledCheck(
                Messages.pref_removeTrailingSpaces,
                Messages.pref_removeTrailingSpacesTip, prefs.getBoolean(
                        IAnyEditConstants.REMOVE_TRAILING_SPACES, defaultStore
                                .getBoolean(IAnyEditConstants.REMOVE_TRAILING_SPACES)),
                spacesComposite);

        replaceAllTabsCheck = AnyEditPreferencePage
                .createLabeledCheck(
                        Messages.pref_replaceAllTabs,
                        Messages.pref_replaceAllTabsTip,
                        prefs
                                .getBoolean(
                                        IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES,
                                        defaultStore
                                                .getBoolean(IAnyEditConstants.REPLACE_ALL_TABS_WITH_SPACES)),
                        spacesComposite);
        replaceAllSpacesCheck = AnyEditPreferencePage
        .createLabeledCheck(
                Messages.pref_replaceAllSpaces,
                Messages.pref_replaceAllSpacesTip,
                prefs
                .getBoolean(
                        IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS,
                        defaultStore
                        .getBoolean(IAnyEditConstants.REPLACE_ALL_SPACES_WITH_TABS)),
                        spacesComposite);

    }
}
