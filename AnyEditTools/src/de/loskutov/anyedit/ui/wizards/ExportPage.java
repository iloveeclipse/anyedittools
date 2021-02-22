/*******************************************************************************
 * Copyright (c) 2009-2021 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 * Contributor:  Fabio Zadrozny - import local projects
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;

/**
 * @author Andrey
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
        Set<IProject> projects = new HashSet<>();

        Object[] sets = getSelectedWorkingSets();
        for (int i = 0; i < sets.length; i++) {
            IMemento childMem = memento.createChild("workingSet");
            IWorkingSet set = (IWorkingSet) sets[i];
            set.saveState(childMem);
            IAdaptable[] elements = set.getElements();
            for (IAdaptable iAdaptable : elements) {
                IProject project = iAdaptable.getAdapter(IProject.class);
                if(project != null) {
                    projects.add(project);
                }
            }
        }

        for(IProject project: projects) {
            IMemento child = memento.createChild("project");
            IPath location = project.getLocation();
            if(location != null) {
                child.putString("name", project.getName());
                child.putString("location", location.toPortableString());
            }
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

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Label label = new Label(comp, SWT.NONE);

        label.setText("Note: all "+ResourcesPlugin.getWorkspace().getRoot().getProjects().length+" projects will also be exported");
    }

    public boolean finish() {
        return storeSets();
    }

    @Override
    protected IStructuredContentProvider createContentProvider() {
        return new WorkingSetContentProvider();
    }

    public static class WorkingSetContentProvider implements ITreeContentProvider {

        public WorkingSetContentProvider() {
            super();
        }

        @Override
        public void dispose() {
            // noop
        }

        @Override
        public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
            // noop
        }

        @Override
        public Object[] getElements(Object inputElement) {
            IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager()
                    .getAllWorkingSets();
            List<IWorkingSet> sets = new ArrayList<IWorkingSet>();
            for (int i = 0; i < workingSets.length; i++) {
                IWorkingSet workingSet = workingSets[i];
                if (!workingSet.isAggregateWorkingSet()) {
                    sets.add(workingSet);
                }
            }
            return sets.toArray(new IWorkingSet[sets.size()]);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return false;
        }
    }

}
