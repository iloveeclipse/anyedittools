/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import java.lang.reflect.Field;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.PageEventAction;
import org.eclipse.ui.internal.SaveAction;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.actions.Spaces;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;


/**
 * @author Andrei
 */
public class SpecialSaveAction extends SaveAction implements IDirtyWorkaround {

    private final Spaces spacesAction;
    private final IAction spacesToTabs = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_SPACES);
    private final IAction tabsToSpaces = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_TABS);

    /**
     * @param window
     */
    public SpecialSaveAction(IWorkbenchWindow window) {
        super(window);
        spacesAction = new Spaces(){
            @Override
            protected AbstractEditor createActiveEditorDelegate() {
                // this just returns the editor instance we already know, see runSpecial()
                return getEditor();
            }
            @Override
            public void setEditor(AbstractEditor editor) {
                if(editor == null && getEditor() != null){
                    getEditor().dispose();
                }
                this.editor = editor;
            }
        };
        spacesAction.setUsedOnSave(true);
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

    @Override
    public void run() {
        runBeforeSave();
        super.run();
    }

    private void runSpecial() {
        if (getWindow() == null) {
            // action has been disposed
            return;
        }
        IEditorPart part = getActiveEditor();
        if(part == null) {
            return;
        }
        spacesAction.setActiveEditor(null, part);
        boolean trim = spacesAction.isSaveAndTrimEnabled();
        boolean convert = spacesAction.isSaveAndConvertEnabled();
        if(trim || convert){
            if(EclipseUtils.matchFilter(part, spacesAction.getCombinedPreferences())){
                return;
            }
            final IAction action;
            if(spacesAction.isDefaultTabToSpaces()){
                action = tabsToSpaces;
            } else {
                action = spacesToTabs;
            }
            spacesAction.run(action);
        }
    }

    public void copyStateAndDispose(IContributionItem oldItem){
        if(oldItem == null || !(oldItem instanceof ActionContributionItem)){
            return;
        }
        IAction action = ((ActionContributionItem) oldItem).getAction();
        if(! (action instanceof SaveAction)){
            return;
        }
        PageEventAction oldAction = (PageEventAction) action;
        IWorkbenchPart activePart = oldAction.getActivePart();
        if(activePart != null){
            partActivated(activePart);
        }
        IWorkbenchPage activePage = oldAction.getActivePage();
        if(activePage != null){
            pageActivated(activePage);
        }
        // this will remove page/window listeners but will lead to crash on 3.3 shutdown
        IWorkbenchWindow window = oldAction.getWorkbenchWindow();
        if(window != null){
            oldAction.dispose();
            try {
                Field field = PageEventAction.class.getDeclaredField("workbenchWindow");
                field.setAccessible(true);
                field.set(oldAction, window);
            } catch (Exception e) {
                AnyEditToolsPlugin.logError("Cannot properly dispose save action", e);
            }
        }

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
