/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBookView;

import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.ui.editor.EditorPropertyTester;

/**
 * @author Andrei
 */
public abstract class AbstractOpenAction
extends AbstractAction
implements IEditorActionDelegate {

    public AbstractOpenAction() {
        super();
    }

    public final void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if(targetEditor == null){
            return;
        }
        setEditor(new AbstractEditor(targetEditor));
    }

    public void run(IAction action) {
        super.run(action);
        if (getViewPart() != null) {
            // view Action
            IViewPart vp = getViewPart();
            if (vp instanceof PageBookView) {
                doPageBookViewAction((PageBookView) vp);
                return;
            }
            TextViewer viewer = (TextViewer)vp.getAdapter(TextViewer.class);
            if(viewer != null) {
                runWithViewer(viewer);
            }
            ISelectionProvider sp = vp.getViewSite().getSelectionProvider();
            if(sp instanceof ITextViewer){
                runWithViewer((ITextViewer) sp);
            }
        } else {
            // editor action
            if (getEditor() != null) {
                AbstractEditor currEditor = getEditor();
                IDocument doc = currEditor.getDocument();
                handleAction(doc, currEditor.getSelectionProvider(),
                        currEditor.getInput());
            }
        }
    }

    /**
     * @param viewer
     */
    protected void runWithViewer(ITextViewer viewer) {
        if(viewer == null){
            return;
        }
        ISelectionProvider selProvider = viewer.getSelectionProvider();
        IDocument doc = viewer.getDocument();
        handleAction(doc, selProvider, null);
    }

    /**
     * @param cv
     */
    private void doPageBookViewAction(PageBookView cv){
        IPage page = cv.getCurrentPage();
        runWithViewer(EditorPropertyTester.getViewer(page));
    }


    protected abstract void handleAction(
            IDocument doc,
            ISelectionProvider selectionProvider,
            IEditorInput currentInput);

}