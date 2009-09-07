/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import de.loskutov.anyedit.util.LineReplaceResult;
import de.loskutov.anyedit.util.TextReplaceResultSet;

public abstract class AbstractReplaceAction extends AbstractTextAction {

    /** contains possible error information during replace */
    protected boolean shouldStopReplace;

    protected TextReplaceResultSet estimateActionRange(IDocument doc){
        TextReplaceResultSet result = new TextReplaceResultSet();
        if (doc == null || getEditor().getSelectionProvider() == null) {
            return result;
        }
        ISelection selection = getEditor().getSelectionProvider().getSelection();
        if(selection == null || !(selection instanceof ITextSelection)){
            return result;
        }
        ITextSelection textSelection = (ITextSelection)selection;
        if(textSelection.isEmpty()){
            return result;
        }
        result.setStartLine(textSelection.getStartLine());
        result.setStopLine(textSelection.getEndLine());
        return result;
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractTextAction#doTextOperation(org.eclipse.jface.text.IDocument, java.lang.String, int, int)
     */
    protected void doTextOperation(IDocument doc,
            String actionID, TextReplaceResultSet resultSet)
            throws BadLocationException {

        ITextSelection textSelection = (ITextSelection) getEditor()
                .getSelectionProvider().getSelection();

        int actionKey = getActionKey(actionID);
        int startSelection = textSelection.getOffset();
        int stopSelection = startSelection + textSelection.getLength();

        int maxNbr = resultSet.getStartLine() + resultSet.getNumberOfLines();

        for (int i = resultSet.getStartLine(); i < maxNbr; i++) {

            IRegion lineInfo = doc.getLineInformation(i);

            boolean useStartLineOffset = lineInfo.getOffset() > startSelection;
            int startReplaceIndex;
            if(useStartLineOffset){
                startReplaceIndex = 0;
            } else {
                startReplaceIndex = startSelection - lineInfo.getOffset();
            }

            int stopReplace;
            boolean useStopLineOffset = lineInfo.getOffset() + lineInfo.getLength() > stopSelection;
            if(useStopLineOffset){
                stopReplace = stopSelection - lineInfo.getOffset();
            } else {
                stopReplace = lineInfo.getLength();
            }

            int rangeToReplace = stopReplace - startReplaceIndex;

            if(rangeToReplace <= 0){
                resultSet.add(null);
                continue;
            }

            String line = doc.get(
                lineInfo.getOffset() + startReplaceIndex, rangeToReplace);
            if(line == null){
                resultSet.add(null);
                continue;
            }

            // call abstract replace operation
            String newText = performReplace(line, actionKey);

            if(shouldStopReplace) {
                shouldStopReplace = false;
                resultSet.clear();
                return;
            }

            boolean changed = !line.equals(newText);
            if(changed){
                LineReplaceResult result = new LineReplaceResult();
                result.startReplaceIndex = startReplaceIndex;
                result.rangeToReplace = rangeToReplace;
                result.textToReplace = newText;
                resultSet.add(result);
            } else {
                resultSet.add(null);
            }
        }
    }

    /**
     * Does custom replace operation.
     * @param line text to "replace"
     * @param actionKey any one action key as delivered by getActionKey(String)
     * method
     * @return "replace" result or old unchanged string, if replace was not performed
     */
    protected abstract String performReplace(String line, int actionKey);

    /**
     * Mapping between String actions and "smarter" custom int's for
     * inernal (in performReplace() method)  use only!
     * @param actionID action id <b>starts</b> with one of AbstractTextAction.ACTION_ID_
     * constants (it starts with the constant cause we have multiple actions
     * there which are used once for key bindings and once for editor actions).
     * @return any one int value for given action
     */
    protected abstract int getActionKey(String actionID);

}
