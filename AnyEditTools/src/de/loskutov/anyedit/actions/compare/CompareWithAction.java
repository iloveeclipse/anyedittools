/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.compare.AnyeditCompareInput;
import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.compare.ExternalFileStreamContent;
import de.loskutov.anyedit.compare.FileStreamContent;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.compare.TextStreamContent;
import de.loskutov.anyedit.ui.editor.AbstractEditor;

/**
 * @author Andrei
 *
 */
public abstract class CompareWithAction implements IObjectActionDelegate /*, IWorkbenchWindowActionDelegate*/ {

    protected ContentWrapper selectedContent;
    protected AbstractEditor editor;

    public CompareWithAction() {
        super();
        editor = new AbstractEditor(null);
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if (targetPart instanceof IEditorPart) {
            editor = new AbstractEditor((IEditorPart) targetPart);
        } else {
            editor = new AbstractEditor(null);
        }

    }

    public void run(IAction action) {
        StreamContent left = null;
        StreamContent right = null;
        try {
            left = createLeftContent();
            if (left == null) {
                return;
            }
            right = createRightContent(left);
            if (right == null) {
                left.dispose();
                return;
            }
            compare(left, right);
        } catch (CoreException e) {
            if(left != null) {
                left.dispose();
            }
            AnyEditToolsPlugin.logError("Can't perform compare", e);
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


    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            // happens only on first initialization in fresh started eclipse, in editor
            if(!editor.isDisposed()){
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
