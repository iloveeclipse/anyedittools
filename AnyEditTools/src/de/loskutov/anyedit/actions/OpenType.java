/*****************************************************************************************
 * Copyright (c) 2009 Andrei Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributor: Andrei Loskutov -
 * initial API and implementation
 ****************************************************************************************/

package de.loskutov.anyedit.actions;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.actions.internal.InternalOpenType;
import de.loskutov.anyedit.jdt.JdtUtils;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.TextUtil;

public class OpenType extends AbstractOpenAction {
    private InternalOpenType defaultAction;
    protected void handleAction(IDocument doc,
        ISelectionProvider selectionProvider, IEditorInput currentInput) {

        String selectedText = guessType(doc, selectionProvider);


        if (selectedText == null || selectedText.length() == 0) {
            selectedText = EclipseUtils.getSelectedText(selectionProvider);
            if(selectedText != null){
                selectedText = "*" + selectedText + "*";
            }
            runDefault(selectedText);
            // TODO ??? why we do not jump to the line here??? I'm missing something???
            return;
        }
        int typeOpened = 0;
        try {
            typeOpened = JdtUtils.searchAndOpenType(selectedText);
        } catch (OperationCanceledException e) {
            //fix similar to https://bugs.eclipse.org/bugs/show_bug.cgi?id=66436
            return;
        }
        if (typeOpened != 1) {
            if(typeOpened > 1){
                // multiple types found, so restrict to exact type name with space at the end
                selectedText = selectedText + " ";
            } else if(typeOpened < 1){
                // no type found, so relax the type name
                selectedText = "*" + selectedText + "*";
            }
            runDefault(selectedText);
        }
        // TODO ??? why we do not jump to the line here??? I'm missing something???
    }

    private void runDefault(String selectedText) {
        if (defaultAction == null) {
            defaultAction = new InternalOpenType();
        }
        defaultAction.run(selectedText);
    }

    private String guessType(IDocument doc, ISelectionProvider selectionProvider) {
        String selectedText = EclipseUtils.getSelectedText(selectionProvider);
        TextUtil textUtil = TextUtil.getDefaultTextUtilities();
        selectedText = textUtil.trimJavaType(selectedText);
        if (!textUtil.isJavaType(selectedText) && doc != null) {
            // try to search around caret
            int caretPosition = EclipseUtils
                .getCaretPosition(selectionProvider);
            try {
                IRegion line = doc.getLineInformation(doc
                    .getLineOfOffset(caretPosition));
                String lineText = doc.get(line.getOffset(), line.getLength());
                selectedText = textUtil.findJavaType(
                    lineText, caretPosition - line.getOffset());
            } catch (BadLocationException e) {
                AnyEditToolsPlugin.logError(null, e);
                selectedText = null;
            }
        }
        //System.out.println("found type: " + selectedText);
        return selectedText;
    }



}