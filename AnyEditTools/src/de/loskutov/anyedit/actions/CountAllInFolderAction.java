/*******************************************************************************
 * Copyright (c) 2010 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
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
import de.loskutov.anyedit.util.EclipseUtils;

public class CountAllInFolderAction extends ConvertAllInFolderAction implements IHandler {

    @Override
    protected IResource getResource(Object selection) {
        if(selection instanceof IContainer) {
            return (IResource)selection;
        }
        if(selection != null) {
            IResource adapter = EclipseUtils.getResource(selection);
            if(adapter instanceof IContainer){
                return adapter;
            }
            adapter = EclipseUtils.getAdapter(selection, IProject.class);
            return adapter;
        }
        return null;
    }

    @Override
    protected boolean getEnablement() {
        return true;
    }

    @Override
    public void run(final IAction action) {

        Job job = new Job("Counting resources") {
            @Override
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
                    @Override
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
        long filesPerFolder = v.folders == 0 ? 0 : v.files / v.folders;
        long resPerProject = v.projects == 0 ? 0 : (v.files + v.folders) / v.projects;

        String message = selectedResources.isEmpty() ? "Workspace" : "Selection";
        message += " contains "
                + (v.files + v.folders + v.projects) + " resources:\n" + v.files
                + " files in " + v.folders + " folders and " + v.projects + " projects.";

        message += "\n\nIn average each folder contains " + filesPerFolder
                + " files, and each project " + resPerProject + " resources.\n";

        boolean hasDerived = v.derivedFiles > 0 || v.derivedFolders > 0;
        if (hasDerived) {
            message += "\nThere are " + v.derivedFiles + " derived files" + " and "
                    + v.derivedFolders + " derived folders.";
        }
        boolean hasTeam = v.teamFiles > 0 || v.teamFolders > 0;
        if (hasTeam) {
            message += "\nThere are " + v.teamFiles + " hidden team files" + " and "
                    + v.teamFolders + " hidden team folders.";
        }

        if (hasDerived || hasTeam) {
            filesPerFolder = v.folders + v.teamFolders + v.derivedFolders == 0 ? 0 : (v.files + v.teamFiles + v.derivedFiles)
                    / (v.folders + v.teamFolders + v.derivedFolders);
            resPerProject = v.projects == 0 ? 0
                    : (v.files + v.folders + v.derivedFolders + v.derivedFiles + v.teamFiles + v.teamFolders) / v.projects;

            message += "\n\nWith derived and team files ";
            message += selectedResources.isEmpty() ? "workspace" : "selection";
            message += " would contain "
                    + (v.files + v.folders + v.projects + v.derivedFolders + v.derivedFiles + v.teamFiles + v.teamFolders)
                    + " resources:\n"
                    + (v.files + v.teamFiles + v.derivedFiles) + " files in "
                    + (v.folders + v.teamFolders + v.derivedFolders) + " folders and " + v.projects + " projects.";
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
        long derivedFolders;
        long derivedFiles;

        @Override
        public boolean visit(IResource resource) throws CoreException {
            if(!resource.isAccessible()){
                return false;
            }
            if(resource instanceof IFile){
                if(resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)){
                    teamFiles ++;
                } else {
                    if(resource.isDerived(IResource.CHECK_ANCESTORS)) {
                        derivedFiles ++;
                    } else {
                        files ++;
                    }
                }
                return false;
            }
            if(resource instanceof IFolder){
                if(resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)){
                    teamFolders ++;
                } else {
                    if(resource.isDerived(IResource.CHECK_ANCESTORS)) {
                        derivedFolders ++;
                    } else {
                        folders ++;
                    }
                }
                return true;
            }
            projects ++;
            return true;
        }
    }

    @Override
    public void addHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        selectionChanged(null, HandlerUtil.getCurrentSelection(event));
        run(null);
        return null;
    }

    @Override
    public void removeHandlerListener(IHandlerListener handlerListener) {
        // noop
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isHandled() {
        return true;
    }
}
