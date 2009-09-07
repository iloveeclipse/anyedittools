/*******************************************************************************
 * Copyright (c) 2007 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.internal.dialogs.WorkingSetLabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;

/**
 * @author Andrei
 */
public abstract class WSPage extends WizardPage {

    private CheckboxTableViewer tv;
    private Composite comp;
    protected List/* <String> */usedFiles;
    private Combo dest;

    protected WSPage(String pageName, String title, String descr,
            String imagePath) {
        super(pageName, title,
                AbstractUIPlugin.imageDescriptorFromPlugin(AnyEditToolsPlugin.getId(),
                        imagePath));
        setDescription(descr);
        usedFiles = new ArrayList();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        comp = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        comp.setLayoutData(gd);
        setControl(comp);

        tv = CheckboxTableViewer.newCheckList(comp, SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        IStructuredContentProvider contentProvider = createContentProvider();
        tv.setContentProvider(contentProvider);
        tv.setLabelProvider(new WorkingSetLabelProvider());
        tv.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tv.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                selectionChanged();
            }
        });

        setInput(contentProvider.getElements(null));

        Composite fileSelectCom = new Composite(comp, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        fileSelectCom.setLayout(layout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fileSelectCom.setLayoutData(gd);

        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        String lastUsedFile = store.getString(IAnyEditConstants.LAST_USED_WS_FILE);
        usedFiles.add(lastUsedFile);
        dest = new Combo(fileSelectCom, SWT.DROP_DOWN);
        dest.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        updateItems(lastUsedFile);

        dest.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

        Button chooserBtn = new Button(fileSelectCom, SWT.NONE);
        chooserBtn.setText("Browse...");
        chooserBtn.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                // ignored
            }

            public void widgetSelected(SelectionEvent e) {
                updateItems(getFileFromUser());
                validateInput();
            }
        });
        validateInput();
    }

    protected void selectionChanged() {
        validateInput();
    }

    abstract protected IStructuredContentProvider createContentProvider();

    protected String getFileString() {
        String text = dest.getText();
        if("".equals(text)) {
            return null;
        }
        return text;
    }

    protected boolean validateInput() {
        String errorMessage;
        String text = getFileString();
        if (text == null) {
            errorMessage = "Please select file";
        } else if (getSelectedWorkingSets().length == 0) {
            errorMessage = "Please select at least one working set";
        } else {
            errorMessage = null;
        }
        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
        return errorMessage == null;
    }

    public void dispose() {
        tv = null;
        comp.dispose();
        super.dispose();
    }

    /**
     * @param sets
     */
    protected void setInput(Object[] elements) {
        tv.setInput(elements);
        for (int i = 0; i < elements.length; i++) {
            IWorkingSet workingSet = (IWorkingSet) elements[i];
            tv.setChecked(workingSet, workingSet.isVisible() && !workingSet.isEmpty());
        }
    }

    protected Object[] getSelectedWorkingSets() {
        return tv.getCheckedElements();
    }

    protected String getFileFromUser() {
        FileDialog fd = new FileDialog(AnyEditToolsPlugin.getShell());
        String fileString = getFileString();
        if (fileString == null) {
            String property = System.getProperty("user.home");
            fd.setFilterPath(property);
        } else {
            fd.setFileName(fileString);
        }
        fd.setFilterExtensions(new String[] { "*.wst" });
        fileString = fd.open();
        if (fileString != null) {
            return fileString;
        }
        return null;
    }

    protected String[] getLastUsedPaths() {
        return (String[]) usedFiles.toArray(new String[0]);
    }

    /**
     * @param fileString
     *
     */
    protected void updateItems(String fileString) {
        if(fileString == null) {
            return;
        }
        if (!usedFiles.contains(fileString)) {
            usedFiles.add(0, fileString);
        }
        dest.setItems(getLastUsedPaths());
        if (dest.getItemCount() > 0) {
            dest.setText(dest.getItem(0));
        }
    }

}
