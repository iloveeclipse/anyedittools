/*******************************************************************************
 * Copyright (c) 2007 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.IWorkbenchConstants;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;

/**
 * @author Andrei
 */
public class ImportPage extends WSPage {

    private static final String TITLE = "Import working sets from the local file system";
    private static final String DESCRIPTION = "Select the file path to import working " +
    		"sets from and working sets to import";

    protected ImportPage(String pageName) {
        super(pageName, TITLE, DESCRIPTION, "icons/import_wiz.gif");
    }

    private String readSets() {
        String pathname = getFileString();
        if (pathname == null) {
            return "Please select file";
        }
        File file = new File(pathname);
        FileInputStream input = null;
        BufferedReader reader;
        IMemento memento;
        try {
            input = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
            memento = XMLMemento.createReadRoot(reader);
        } catch (FileNotFoundException e) {
            return "Couldn't read working set file: " + file + ": " + e.getMessage();
        } catch (IOException e) {
            return "Couldn't read working set file: " + file + ": " + e.getMessage();
        } catch (CoreException e) {
            return "Couldn't read working set file: " + file + ": " + e.getMessage();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    AnyEditToolsPlugin.logError(null, e);
                }
            }
        }

        IMemento[] mementos = memento.getChildren("workingSet");
        List sets = new ArrayList();
        for (int i = 0; i < mementos.length; i++) {
            IWorkingSet set = restoreWorkingSet(mementos[i]);
            if (set != null) {
                sets.add(set);
            }
        }

        setInput(sets.toArray(new IWorkingSet[0]));

        try {
            String lastUsedFile = file.getCanonicalPath();
            IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
            store.setValue(IAnyEditConstants.LAST_USED_WS_FILE, lastUsedFile);
        } catch (IOException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        return null;
    }

    // see org.eclipse.ui.internal.AbstractWorkingSet
    protected IWorkingSet restoreWorkingSet(IMemento memento) {
        String factoryID = memento.getString(IWorkbenchConstants.TAG_FACTORY_ID);

        if (factoryID == null) {
            // if the factory id was not set in the memento
            // then assume that the memento was created using
            // IMemento.saveState, and should be restored using WorkingSetFactory
            factoryID = "org.eclipse.ui.internal.WorkingSetFactory";
        }
        IElementFactory factory = PlatformUI.getWorkbench().getElementFactory(factoryID);
        if (factory == null) {
            AnyEditToolsPlugin.logError(
                    "Unable to restore working set - cannot instantiate factory: "
                            + factoryID, null);
            return null;
        }
        IAdaptable adaptable = factory.createElement(memento);
        if (adaptable == null) {
            AnyEditToolsPlugin.logError(
                    "Unable to restore working set - cannot instantiate working set: "
                            + factoryID, null);
            return null;
        }
        if (!(adaptable instanceof IWorkingSet)) {
            AnyEditToolsPlugin.logError(
                    "Unable to restore working set - element is not an IWorkingSet: "
                            + factoryID, null);
            return null;
        }
        return (IWorkingSet) adaptable;
    }

    public boolean finish() {
        importSelectedSets();
        return true;
    }

    private void importSelectedSets() {
        Object[] selected = getSelectedWorkingSets();
        if (selected == null) {
            return;
        }
        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
                .getWorkingSetManager();
        List added = new ArrayList();
        for (int i = 0; i < selected.length; i++) {
            IWorkingSet workingSet = (IWorkingSet) selected[i];
            if (workingSetManager.getWorkingSet(workingSet.getName()) == null) {
                removeNonExistingChildren(workingSet);
                workingSetManager.addWorkingSet(workingSet);
                added.add(workingSet);
            }
        }
        if (added.size() > 0) {
            try {
                /*
                 * Reflection required because some people do not have JDT installed, and
                 * thus current class would not be loaded at all with the direct reference
                 * to the action
                 */
                Class actClass = Class.forName(
                        "de.loskutov.anyedit.jdt.SelectWorkingSetsAction", true,
                        getClass().getClassLoader());
                IWSAction action = (IWSAction) actClass.newInstance();
                action.setWorkingSets(added);
                action.run();
            } catch (NoClassDefFoundError e) {
                AnyEditToolsPlugin.logError("JDT not installed", e);
            } catch (ClassNotFoundException e) {
                AnyEditToolsPlugin.logError("JDT not installed", e);
            } catch (Throwable e) {
                AnyEditToolsPlugin.logError("Failed to activate imported working sets"
                        + " in Package Explorer view", e);
            }
        }
    }

    private void removeNonExistingChildren(IWorkingSet workingSet) {
        IAdaptable[] elements = workingSet.getElements();
        List existing = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            IResource resource = (IResource) elements[i].getAdapter(IResource.class);
            if (resource != null && resource.exists()) {
                existing.add(resource);
            }
        }
        workingSet.setElements((IAdaptable[]) existing.toArray(new IAdaptable[0]));
    }

    public static class WorkingSetContentProvider implements ITreeContentProvider {
        private IWorkingSet[] workingSets;

        public void dispose() {
            // noop
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof IWorkingSet[]) {
                workingSets = (IWorkingSet[]) newInput;
            }
        }

        public Object[] getElements(Object inputElement) {
            if (workingSets == null) {
                return new Object[0];
            }
            List sets = new ArrayList();
            for (int i = 0; i < workingSets.length; i++) {
                IWorkingSet workingSet = workingSets[i];
                if (!workingSet.isAggregateWorkingSet()) {
                    sets.add(workingSet);
                }
            }
            return sets.toArray(new IWorkingSet[0]);
        }

        public Object[] getChildren(Object parentElement) {
            return null;
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.ui.wizards.WSPage#createContentProvider()
     */
    protected IStructuredContentProvider createContentProvider() {
        return new WorkingSetContentProvider();
    }

    protected void selectionChanged() {
        String errorMessage = null;
        if (getSelectedWorkingSets().length == 0) {
            errorMessage = "Please select at least one working set";
        }
        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.ui.wizards.WSPage#validateInput()
     */
    protected boolean validateInput() {
        String errorMessage = null;
        String text = getFileString();
        if (text == null) {
            errorMessage = "Please select file";
        }

        if (errorMessage != null) {
            setInput(new IWorkingSet[0]);
            setErrorMessage(errorMessage);
            setPageComplete(false);
            return false;
        }
        errorMessage = readSets();
        if (errorMessage != null) {
            errorMessage = "Working set import failed: " + errorMessage;
        } else if (getSelectedWorkingSets().length == 0) {
            errorMessage = "Please select at least one working set";
        }
        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
        if (errorMessage != null) {
            setInput(new IWorkingSet[0]);
        }
        return errorMessage == null;
    }

}
