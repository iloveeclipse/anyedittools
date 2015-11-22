/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.ui.preferences.CombinedPreferences;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrey
 */
public abstract class AbstractAction extends AbstractHandler
implements IWorkbenchWindowActionDelegate, IViewActionDelegate, IEditorActionDelegate {

    protected AbstractEditor editor;
    private IFile file;
    private IWorkbenchWindow window;
    private IWorkbenchPart part;
    private CombinedPreferences combinedPreferences;

    public AbstractAction() {
        super();
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null && isEnabled()) {
            part = HandlerUtil.getActivePart(event);
            run(new Action(){
                @Override
                public String getId() {
                    return event.getCommand().getId();
                }
            });
        } else {
            part = null;
        }
        return null;
    }

    protected IViewPart getViewPart() {
        return part instanceof IViewPart? (IViewPart)part : null;
    }

    @Override
    public void run(IAction action) {
        if(action == null){
            return;
        }
        setEditor( createActiveEditorDelegate());
    }

    protected AbstractEditor createActiveEditorDelegate() {
        return new AbstractEditor(EclipseUtils.getActiveEditor());
    }

    @Override
    public void dispose() {
        if(editor != null){
            editor.dispose();
            editor = null;
        }
        window = null;
        part = null;
    }

    public void setEditor(AbstractEditor editor) {
        if(getEditor() != null){
            getEditor().dispose();
        }
        this.editor = editor;
        combinedPreferences = null;
    }

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        combinedPreferences = null;
        if(targetEditor == null){
            return;
        }
        setEditor(new AbstractEditor(targetEditor));
    }

    protected AbstractEditor getEditor() {
        return editor;
    }

    @Override
    public void init(IWorkbenchWindow window1) {
        window = window1;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // unused
    }

    /**
     * @return could return null, if we do not have associated file and operating on
     * editor inputs instead
     */
    public IFile getFile() {
        IFile myFile = file;
        if(myFile == null && getEditor() != null){
            myFile = getEditor().getIFile();
        }
        return myFile;
    }

    /**
     * @param file to perform operation on
     */
    public void setFile(IFile file) {
        this.file = file;
        combinedPreferences = null;
    }

    /**
     * @return may be null if this action is not yet initialized
     */
    public IWorkbenchWindow getWindow(){
        return window;
    }

    @Override
    public final void init(IViewPart view) {
        this.part = view;
    }

    public CombinedPreferences getCombinedPreferences() {
        if(combinedPreferences != null){
            return combinedPreferences;
        }
        IFile file1 = getFile();
        IScopeContext context = null;
        if (file1 != null) {
            IProject project = file1.getProject();
            if (project != null) {
                context = new ProjectScope(project);
            }
        }
        combinedPreferences = new CombinedPreferences(context,
                AnyEditToolsPlugin.getDefault().getPreferenceStore());
        return combinedPreferences;
    }
}
