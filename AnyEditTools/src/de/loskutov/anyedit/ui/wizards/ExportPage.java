/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;

/**
 * @author Andrei
 */
public class ExportPage extends WSPage {

    private static final String TITLE = "Export working sets to the local file system";
    private static final String DESCRIPTION = "Select working sets to export and the file path to store";

    protected ExportPage(String pageName) {
        super(pageName, TITLE, DESCRIPTION, "icons/export_wiz.gif");
    }

    private boolean storeSets() {
        validateInput();
        String pathname = getFileString();
        if (pathname == null) {
            return false;
        }
        File file = new File(pathname);
        XMLMemento memento = XMLMemento.createWriteRoot("workingSets");
        Object[] sets = getSelectedWorkingSets();
        for (int i = 0; i < sets.length; i++) {
            IMemento childMem = memento.createChild("workingSet");
            IWorkingSet set = (IWorkingSet) sets[i];
            set.saveState(childMem);
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            memento.save(writer);
        } catch (IOException e) {
            AnyEditToolsPlugin.errorDialog("Couldn't write working set file: ", e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    AnyEditToolsPlugin.logError(null, e);
                }
            }
        }
        try {
            String lastUsedFile = file.getCanonicalPath();
            IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
            store.setValue(IAnyEditConstants.LAST_USED_WS_FILE, lastUsedFile);
        } catch (IOException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        return true;
    }

    public boolean finish() {
        return storeSets();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.ui.wizards.WSPage#createContentProvider()
     */
    protected IStructuredContentProvider createContentProvider() {
        return new WorkingSetContentProvider();
    }

    public static class WorkingSetContentProvider implements ITreeContentProvider {

        public WorkingSetContentProvider() {
            super();
        }

        public void dispose() {
            // noop
        }

        public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
            // noop
        }

        public Object[] getElements(Object inputElement) {
            IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager()
                    .getAllWorkingSets();
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

}
