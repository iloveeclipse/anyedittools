/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.handlers.HandlerUtil;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 */
public abstract class AbstractAction extends AbstractHandler implements IWorkbenchWindowActionDelegate, IViewActionDelegate {

    protected AbstractEditor editor;
    private IFile file;
    private IWorkbenchWindow window;
    private IWorkbenchPart part;

    public AbstractAction() {
        super();
    }

    public Object execute(final ExecutionEvent event) throws ExecutionException {
        window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null && isEnabled()) {
            part = HandlerUtil.getActivePart(event);
            run(new Action(){
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

    public void run(IAction action) {
        if(action == null){
            return;
        }
        setEditor( createActiveEditorDelegate());
    }

    protected AbstractEditor createActiveEditorDelegate() {
        return new AbstractEditor(EclipseUtils.getActiveEditor());
    }

    public void dispose() {
        if(editor != null){
            editor.dispose();
            editor = null;
        }
        window = null;
        part = null;
    }

    protected void setEditor(AbstractEditor editor) {
        if(getEditor() != null){
            getEditor().dispose();
        }
        this.editor = editor;
    }

    protected AbstractEditor getEditor() {
        return editor;
    }

    public void init(IWorkbenchWindow window1) {
        window = window1;
    }

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
            myFile = getEditor().getFile();
        }
        return myFile;
    }

    /**
     * @param file to perform operation on
     */
    public void setFile(IFile file) {
        this.file = file;
    }

    /**
     * @return may be null if this action is not yet initialized
     */
    public IWorkbenchWindow getWindow(){
        return window;
    }

    public final void init(IViewPart view) {
        this.part = view;
    }
}
