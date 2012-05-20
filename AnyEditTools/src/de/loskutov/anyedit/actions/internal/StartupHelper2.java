/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.menus.CommandContributionItem;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.actions.Spaces;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrey
 */
public class StartupHelper2 {

    private static final String FILE_SAVE_ALL = "org.eclipse.ui.file.saveAll";

    private static final String FILE_SAVE = "org.eclipse.ui.file.save";

    private static final class DummyAction extends Action {
        public DummyAction(String actionId) {
            setId(actionId);
        }
    }

    /**
     * Will be run after workbench is started and w.window is opened
     */
    public StartupHelper2() {
        super();
    }

    public void init() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(new DirtyHookRunnable());
    }

    /**
     * Very dirty trick to get internal handle to WorkbenchWindowConfigurer.
     * @param ww
     */
    static IWorkbenchWindowConfigurer getWorkbenchWindowConfigurer(IWorkbenchWindow ww) {
        if (!(ww instanceof WorkbenchWindow)) {
            return null;
        }
        try {
            Method method = WorkbenchWindow.class.getDeclaredMethod(
                    "getWindowConfigurer", null);
            method.setAccessible(true);
            Object object = method.invoke(ww, null);
            if (object instanceof IWorkbenchWindowConfigurer) {
                return (IWorkbenchWindowConfigurer) object;
            }
        } catch (Exception e) {
            AnyEditToolsPlugin.logError(
                    "Can't get handle for WorkbenchWindowConfigurer", e); //$NON-NLS-1$
        }
        return null;
    }

    private void hookOnCommand(String commandId) {
        ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(
                ICommandService.class);
        Command command = service.getCommand(commandId);
        PreExecutionHandler listener = new PreExecutionHandler(commandId);
        command.addExecutionListener(listener);
    }

    private static class PreExecutionHandler implements IExecutionListener {

        final Spaces spacesAction;
        final IAction spacesToTabs = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_SPACES);
        final IAction tabsToSpaces = new DummyAction(IAnyEditConstants.ACTION_ID_CONVERT_TABS);
        final String commandId;

        PreExecutionHandler(String commandId) {
            this.commandId = commandId;
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

        public void notHandled(String command, NotHandledException exception) {
            //
        }

        public void postExecuteFailure(String command, ExecutionException exception) {
            //
        }

        public void postExecuteSuccess(String command, Object returnValue) {
            //
        }

        public void preExecute(String command, ExecutionEvent event) {
            runBeforeSave(event);
        }


        /**
         * Performs the 'convert spaces' action before the editor buffer is saved
         */
        public void runBeforeSave(ExecutionEvent event) {
            try {
                if(FILE_SAVE.equals(commandId)) {
                    runSpecial(event);
                } else {
                    runSpecial2(event);
                }
            } catch (Throwable e) {
                // to avoid any problems with any possible environements
                // we trying to catch all errors to allow perform base save action
                AnyEditToolsPlugin.logError("Cannot perform custom pre-save action", e); //$NON-NLS-1$
            } finally {
                spacesAction.setEditor(null);
            }
        }

        private void runSpecial(ExecutionEvent event) {

            IEditorPart part = HandlerUtil.getActiveEditor(event);
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

        private void runSpecial2(ExecutionEvent event) {
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
            if (window == null) {
                // action has been disposed
                return;
            }
            IWorkbenchPage page = window.getActivePage();
            final IEditorPart[] dirtyEditors = page.getDirtyEditors();

            final int editorsCount = dirtyEditors.length;
            if(editorsCount == 0) {
                return;
            }
            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {
                    monitor.beginTask("Converting tabs <-> spaces before save", editorsCount);
                    try {
                        for (int i = 0; i < editorsCount; i++) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            monitor.worked(1);
                            IEditorPart part = dirtyEditors[i];
                            spacesAction.setActiveEditor(null, part);
                            boolean trim = spacesAction.isSaveAndTrimEnabled();
                            boolean convert = spacesAction.isSaveAndConvertEnabled();
                            if (trim || convert) {
                                if (EclipseUtils.matchFilter(part,
                                        spacesAction.getCombinedPreferences())) {
                                    continue;
                                }
                                monitor.subTask(part.getTitle());
                                final IAction action;
                                if (spacesAction.isDefaultTabToSpaces()) {
                                    action = tabsToSpaces;
                                } else {
                                    action = spacesToTabs;
                                }
                                spacesAction.run(action);
                            }
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

    private final class DirtyHookRunnable implements Runnable {

        private static final String PRINT_BUTTON_ID = "print";

        private static final String FILE_TOOLBAR = "org.eclipse.ui.workbench.file";

        private DirtyHookRunnable() {
            super();
        }

        public void run() {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                return;
            }
            try{
                run(window);
            } catch (Throwable t){
                AnyEditToolsPlugin.logError("Can't run dirty code to replace default actions", t);
            }
        }

        private void run(IWorkbenchWindow window) {
            IWorkbenchWindowConfigurer wwConf = getWorkbenchWindowConfigurer(window);
            if (wwConf == null) {
                return;
            }

            hookOnCommand(FILE_SAVE);
            hookOnCommand(FILE_SAVE_ALL);

            IActionBarConfigurer configurer = wwConf.getActionBarConfigurer();

            configurer.getMenuManager();

            // get "file" menu group
            IContributionItem item;
            ICoolBarManager coolBar = configurer.getCoolBarManager();
            // get "file" toolbar group
            item = coolBar.find(FILE_TOOLBAR);
            if (item instanceof ToolBarContributionItem) {
                ToolBarContributionItem item2 = (ToolBarContributionItem) item;
                ToolBarManager manager = (ToolBarManager) item2.getToolBarManager();

                boolean removePrint = getPref(IAnyEditConstants.REMOVE_PRINT_FROM_TOOLBAR);
                if (removePrint) {
                    remove(manager, PRINT_BUTTON_ID);
                }
                // to resize toolbars after changes...
                coolBar.update(true);
            }
        }
    }



    private static void remove(IContributionManager manager, String id) {
        IContributionItem[] items = manager.getItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i].isSeparator() || items[i] instanceof ActionContributionItem
                    || items[i] instanceof CommandContributionItem) {
                if (id.equals(items[i].getId())) {
                    IContributionItem item = manager.remove(items[i]);
                    // refresh menu gui
                    manager.update(true);
                    if (item != null) {
                        item.dispose();
                    }
                    break;
                }
            }
        }
    }

    private static boolean getPref(String prefkey) {
        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(prefkey);
    }

}
