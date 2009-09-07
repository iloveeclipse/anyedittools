/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.PageEventAction;
import org.eclipse.ui.internal.SaveAllAction;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.actions.AbstractTextAction;
import de.loskutov.anyedit.actions.Spaces;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 */
public class SpecialSaveAllAction extends SaveAllAction implements IDirtyWorkaround {

    private final Spaces spacesAction;

    private final IAction spacesToTabs = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_SPACES);

    private final IAction tabsToSpaces = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_TABS);



    /**
     * @param window
     */
    public SpecialSaveAllAction(IWorkbenchWindow window) {
        super(window);
        spacesAction = new Spaces() {
            protected AbstractEditor createActiveEditorDelegate() {
                // this just returns the editor instance we already know, see runSpecial()
                return getEditor();
            }

            public void setEditor(AbstractEditor editor) {
                if (editor == null && getEditor() != null) {
                    getEditor().dispose();
                }
                this.editor = editor;
            }
        };
        spacesAction.setUsedOnSave(true);

        IWorkbenchPage page = getActivePage();
        if (page != null) {
            IEditorReference[] editorReferences = page.getEditorReferences();
            for (int i = 0; i < editorReferences.length; i++) {
                IWorkbenchPart part = editorReferences[i].getPart(false);
                if (part != null) {
                    partOpened(part);
                }
            }
            IViewReference[] viewReferences = page.getViewReferences();
            for (int i = 0; i < viewReferences.length; i++) {
                IWorkbenchPart part = viewReferences[i].getPart(false);
                if (part != null) {
                    partOpened(part);
                }
            }
        }
    }

    /**
     * Performs the 'convert spaces' action before the editor buffer is saved
     */
    public void runBeforeSave() {
        try {
            runSpecial();
        } catch (Throwable e) {
            // to avoid any problems with any possible environements
            // we trying to catch all errors to allow perform base save action
            AnyEditToolsPlugin.logError("Cannot perform custom pre-save action", e); //$NON-NLS-1$
        } finally {
            spacesAction.setEditor(null);
        }
    }

    /* (non-Javadoc)
     * Method declared on IAction.
     * Performs the <code>Save</code> action by calling the
     * <code>IEditorPart.doSave</code> method on the active editor.
     */
    public void run() {
        runBeforeSave();
        super.run();
    }

    /**
     *
     */
    private void runSpecial() {
        if (getWindow() == null) {
            // action has been disposed
            return;
        }
        boolean trim = AbstractTextAction.isSaveAndTrimEnabled();
        boolean convert = AbstractTextAction.isSaveAndConvertEnabled();
        if (trim || convert) {
            IWorkbenchPage page = getActivePage();
            final IEditorPart[] dirtyEditors = page.getDirtyEditors();

            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Converting tabs <-> spaces before save",
                            dirtyEditors.length);
                    try {
                        for (int i = 0; i < dirtyEditors.length; i++) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            IEditorPart part = dirtyEditors[i];
                            if (EclipseUtils.matchFilter(part)) {
                                monitor.worked(1);
                                continue;
                            }
                            monitor.subTask(part.getTitle());
                            spacesAction.setActiveEditor(null, part);
                            final IAction action;
                            if(spacesAction.isDefaultTabToSpaces(spacesAction.getCombinedPreferences())){
                                action = tabsToSpaces;
                            } else {
                                action = spacesToTabs;
                            }
                            spacesAction.run(action);
                            monitor.worked(1);
                        }
                    } finally {
                        monitor.done();
                    }
                }
            };

            try {
                PlatformUI.getWorkbench().getProgressService().run(false, true, runnable);
            } catch (InvocationTargetException e) {
                 AnyEditToolsPlugin.logError("Error during custom pre-save action", e);
            } catch (InterruptedException e) {
                // user cancel
            }
        }
    }

    public void copyStateAndDispose(IContributionItem oldItem) {
        if (oldItem == null || !(oldItem instanceof ActionContributionItem)) {
            return;
        }
        IAction action = ((ActionContributionItem) oldItem).getAction();
        if (!(action instanceof SaveAllAction)) {
            return;
        }
        PageEventAction oldAction = (PageEventAction) action;
        IWorkbenchPart activePart = oldAction.getActivePart();
        if (activePart != null) {
            partActivated(activePart);
        }
        IWorkbenchPage activePage = oldAction.getActivePage();
        if (activePage != null) {
            pageActivated(activePage);
        }
        // this should remove page/window listeners
        oldAction.dispose();
    }

    public IWorkbenchWindow getWindow() {
        return getWorkbenchWindow();
    }

    private static final class DummyAction extends Action {

        /**
         * @param actionId
         */
        public DummyAction(String actionId) {
            setId(actionId);
        }

    }
}
