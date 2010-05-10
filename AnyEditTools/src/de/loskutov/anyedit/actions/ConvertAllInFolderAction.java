/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;

// TODO check how we could reuse ConvertLineDelimitersAction etc
public class ConvertAllInFolderAction extends ConvertAllAction {

    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            Iterator iterator = ssel.iterator();
            selectedFiles.clear();
            selectedResources.clear();
            while (iterator.hasNext()) {
                IResource next = getResource(iterator.next());
                if(next != null) {
                    // we do not check the folders for files here, because this could lead to
                    // big delays on large (or distributed) directories during select action
                    // any one is accepted, file or folder
                    selectedResources.add(next);
                }
            }
        }
        action.setEnabled(getEnablement());
    }

    private boolean getEnablement() {
        return !selectedResources.isEmpty();
    }

    private IResource getResource(Object selection) {
        if(selection instanceof IContainer || selection instanceof IFile) {
            return (IResource)selection;
        }
        if(selection instanceof IAdaptable) {
            IAdaptable adaptable= (IAdaptable) selection;
            Object adapter = adaptable.getAdapter(IResource.class);
            if(adapter instanceof IContainer || adapter instanceof IFile){
                return (IResource) adapter;
            }
        }
        return null;
    }

    public void run(final IAction action) {

        IPreferenceStore preferenceStore = AnyEditToolsPlugin.getDefault()
                .getPreferenceStore();

        boolean shouldAsk = preferenceStore
                .getBoolean(IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER);

        if (shouldAsk) {
            MessageDialogWithToggle dialogWithToggle = MessageDialogWithToggle
                    .openYesNoQuestion(
                            AnyEditToolsPlugin.getShell(),
                            Messages.ConvertAllInFolder_warnTitle,
                            Messages.ConvertAllInFolder_warnMessage,
                            Messages.ConvertAllInFolder_toggleMessage,
                            false, preferenceStore,
                            IAnyEditConstants.ASK_BEFORE_CONVERT_ALL_IN_FOLDER);

            int returnCode = dialogWithToggle.getReturnCode();
            if (returnCode != IDialogConstants.YES_ID) {
                return;
            }
        }
        try {
            // runs in the same thread to be able to collect all files before
            // starting main action, which will be executed in separated thread
            PlatformUI.getWorkbench().getProgressService().run(true, true,
                    new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            monitor.beginTask(Messages.ConvertAllInFolder_task,
                                    selectedResources.size() * 10);
                            try {
                                for (int i = 0; i < selectedResources.size()
                                        && !monitor.isCanceled(); i++) {

                                    Object o = selectedResources.get(i);
                                    if(o instanceof IContainer) {
                                        addAllFiles((IContainer) o,
                                                selectedFiles, monitor);
                                    } else if(o instanceof IFile){
                                        if (selectedFiles.contains(o)) {
                                            continue;
                                        }
                                        selectedFiles.add(o);
                                    }
                                }
                            } finally {
                                monitor.done();
                            }
                        }
                    });
        } catch (InvocationTargetException e) {
            AnyEditToolsPlugin.logError("'Convert all' operation: not all files converted", e);
        } catch (InterruptedException e) {
            AnyEditToolsPlugin.logError("'Convert all' operation cancelled by user", e);
            return;
        } // workspace lock
        // will run in another thread, but will block UI too
        doAction(action);
    }

    protected void doAction(IAction action) {
        selectedResources.clear();
        super.run(action);
    }

    protected void addAllFiles(IContainer container, List fileList, IProgressMonitor monitor) {
        try {
            IResource[] resources = container.members();
            for (int i = 0; i < resources.length && !monitor.isCanceled(); i++) {
                monitor.internalWorked(1);
                IResource resource = resources[i];
                if (resource.getType() == IResource.FILE) {
                    if (fileList.contains(resource)) {
                        continue;
                    }
                    fileList.add(resource);
                } else if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
                    addAllFiles((IContainer) resource, fileList, monitor);
                }
            }
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError("Couldn't collect all files for convert action in folder " + container, e);
        }
    }

}
