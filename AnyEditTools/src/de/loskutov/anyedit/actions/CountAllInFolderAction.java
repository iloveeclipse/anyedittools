/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.Messages;

public class CountAllInFolderAction extends ConvertAllInFolderAction implements IHandler {

    protected IResource getResource(Object selection) {
        if(selection instanceof IContainer) {
            return (IResource)selection;
        }
        if(selection instanceof IAdaptable) {
            IAdaptable adaptable= (IAdaptable) selection;
            Object adapter = adaptable.getAdapter(IResource.class);
            if(adapter instanceof IContainer){
                return (IResource) adapter;
            }
            adapter = adaptable.getAdapter(IProject.class);
            if(adapter instanceof IProject){
                return (IResource) adapter;
            }
        }
        return null;
    }

    protected boolean getEnablement() {
        return true;
    }

    public void run(final IAction action) {

        Job job = new Job("Counting resources") {
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(Messages.CollectAllInFolder_task, IProgressMonitor.UNKNOWN);
                final CountingVisitor v = new CountingVisitor();
                for (int i = 0; i < selectedResources.size() && !monitor.isCanceled(); i++) {
                    Object o = selectedResources.get(i);
                    if (o instanceof IContainer) {
                        IContainer container = (IContainer) o;
                        if(!container.isAccessible()){
                            continue;
                        }
                        try {
                            container.accept(v, IResource.DEPTH_INFINITE,
                                    IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
                        } catch (CoreException e) {
                            AnyEditToolsPlugin.logError("Failed counting resources", e);
                        }
                    }
                }
                if(selectedResources.isEmpty()){
                    try {
                        ResourcesPlugin.getWorkspace().getRoot().accept(v, IResource.DEPTH_INFINITE,
                                IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
                    } catch (CoreException e) {
                        AnyEditToolsPlugin.logError("Failed counting resources", e);
                    }
                }

                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        printSummary(v);
                    }
                });
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void printSummary(final CountingVisitor v) {
        long filesPerFolder = v.folders + v.teamFolders == 0 ? 0 : (v.files + v.teamFiles)
                / (v.folders + v.teamFolders);
        long resPerProject = v.projects == 0 ? 0
                : (v.files + v.folders + v.teamFiles + v.teamFolders) / v.projects;

        String message = selectedResources.isEmpty() ? "Workspace" : "Selection";
        message += " contains " + (v.files + v.folders + v.projects + v.teamFiles + v.teamFolders)
                + " resources:\n" + (v.files + v.teamFiles) + " files in "
                + (v.folders + v.teamFolders) + " folders and " + v.projects + " projects.";

        message += "\n\nIn average each folder contains " + filesPerFolder
                + " files, and each project " + resPerProject + " resources.";

        if (v.teamFiles > 0 || v.teamFolders > 0) {
            message += "\n\nThere are also " + v.teamFiles + " hidden team files" + " and "
                    + v.teamFolders + " hidden team folders.";

            filesPerFolder = v.folders == 0 ? 0 : v.files / v.folders;
            resPerProject = v.projects == 0 ? 0 : (v.files + v.folders) / v.projects;

            message += "\n\n*Without* team resources selection would contain "
                    + (v.files + v.folders + v.projects) + " resources:\n" + v.files
                    + " files in " + v.folders + " folders and " + v.projects + " projects.";

            message += "\n\nIn average each folder would contain " + filesPerFolder
                    + " files, and each project " + resPerProject + " resources.";
        }
        AnyEditToolsPlugin.logInfo(message);
        MessageDialog.openInformation(null, "Count all resources", message);
    }

    static class CountingVisitor implements IResourceVisitor {
        long files;
        long folders;
        long projects;
        long teamFolders;
        long teamFiles;

        public boolean visit(IResource resource) throws CoreException {
            if(!resource.isAccessible()){
                return false;
            }
            if(resource instanceof IFile){
                if(resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)){
                    teamFiles ++;
                } else {
                    files ++;
                }
                return false;
            }
            if(resource instanceof IFolder){
                if(resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)){
                    teamFolders ++;
                } else {
                    folders ++;
                }
                return true;
            }
            projects ++;
            return true;
        }
    }

    public void addHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        selectionChanged(null, HandlerUtil.getCurrentSelection(event));
        run(null);
        return null;
    }

    public void removeHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean isHandled() {
        return true;
    }
}
