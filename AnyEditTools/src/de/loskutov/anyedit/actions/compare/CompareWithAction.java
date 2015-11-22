/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.compare.AnyeditCompareInput;
import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.compare.ExternalFileStreamContent;
import de.loskutov.anyedit.compare.FileStreamContent;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.compare.TextStreamContent;
import de.loskutov.anyedit.ui.editor.AbstractEditor;

/**
 * @author Andrey
 *
 */
public abstract class CompareWithAction extends AbstractHandler implements IObjectActionDelegate {

    protected ContentWrapper selectedContent;
    protected AbstractEditor editor;

    public CompareWithAction() {
        super();
        editor = new AbstractEditor(null);
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        Action dummyAction = new Action(){
            @Override
            public String getId() {
                return event.getCommand().getId();
            }
        };
        setActivePart(dummyAction, activePart);
        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        selectionChanged(dummyAction, currentSelection);
        if(dummyAction.isEnabled()) {
            run(dummyAction);
        }
        return null;
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        editor = new AbstractEditor(targetPart);
    }

    @Override
    public void run(IAction action) {
        StreamContent left = createLeftContent();
        if (left == null) {
            return;
        }
        try {
            StreamContent right = createRightContent(left);
            if (right == null) {
                left.dispose();
                return;
            }
            compare(left, right);
        } catch (CoreException e) {
            left.dispose();
            AnyEditToolsPlugin.logError("Can't perform compare", e);
        } finally {
            selectedContent = null;
            editor = null;
        }
    }

    protected StreamContent createLeftContent() {
        if (!editor.isDisposed()) {
            String selectedText = editor.getSelectedText();
            if (selectedText != null && selectedText.length() != 0) {
                return new TextStreamContent(selectedContent, editor);
            }
        }
        return createContent(selectedContent);
    }

    protected abstract StreamContent createRightContent(StreamContent left)
            throws CoreException;

    protected final StreamContent createContent(ContentWrapper content) {
        if (content == null) {
            return null;
        }
        /// XXX should we really first check for the document???
        if (editor.getDocument() != null) {
            return new TextStreamContent(content, editor);
        }
        return createContentFromFile(content);
    }

    protected static final StreamContent createContentFromFile(ContentWrapper content) {
        if (content == null) {
            return null;
        }
        if (content.getIFile() != null) {
            return new FileStreamContent(content);
        }
        if (content.getFile() != null) {
            return new ExternalFileStreamContent(content);
        }
        return null;
    }

    private void compare(StreamContent left, StreamContent right) {
        AnyeditCompareInput input = new AnyeditCompareInput(left, right);
        CompareUI.openCompareEditor(input);
        if(input.getCompareResult() == null){
            // CompareEditor didn't shown up because both sides are same: we have
            // to dispose input object explicitely.
            input.internalDispose();
        }
    }


    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            // happens only on first initialization in fresh started eclipse, in editor
            if(!editor.isDisposed() || selectedContent == null){
                selectedContent = ContentWrapper.create(editor);
            }
            action.setEnabled(selectedContent != null);
            return;
        }
        IStructuredSelection sSelection = (IStructuredSelection) selection;
        Object firstElement = sSelection.getFirstElement();
        if(!editor.isDisposed()) {
            selectedContent = ContentWrapper.create(editor);
        } else {
            selectedContent = ContentWrapper.create(firstElement);
        }
        action.setEnabled(selectedContent != null && sSelection.size() == 1);
    }

}
