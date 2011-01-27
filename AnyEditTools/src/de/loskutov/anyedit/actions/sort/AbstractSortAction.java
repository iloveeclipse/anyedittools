/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Clemens Fuchslocher - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import de.loskutov.anyedit.actions.AbstractTextAction;
import de.loskutov.anyedit.util.LineReplaceResult;
import de.loskutov.anyedit.util.TextReplaceResultSet;

/**
 * @author Clemens Fuchslocher
 */
public abstract class AbstractSortAction extends AbstractTextAction {

    protected abstract Comparator getComparator();

    protected TextReplaceResultSet estimateActionRange(IDocument document) {
        ITextSelection selection = getSelection();
        if (selection == null || selection.isEmpty()) {
            return new TextReplaceResultSet();
        }

        TextReplaceResultSet result = new TextReplaceResultSet();
        result.setStartLine(selection.getStartLine());
        result.setStopLine(selection.getEndLine());
        return result;
    }

    protected void doTextOperation(IDocument document, String action, TextReplaceResultSet results) throws BadLocationException {
        List lines = getLines(results, document);
        if (lines.isEmpty() || lines.size() == 1) {
            return;
        }

        sortLines(lines);
        addLines(results, lines);
    }

    private List getLines(TextReplaceResultSet results, IDocument document) throws BadLocationException {
        List lines = new ArrayList();
        int start = results.getStartLine();
        int stop = results.getStopLine();

        for (int n = start; n <= stop; n++) {
            int offset = document.getLineOffset(n);
            int length = document.getLineLength(n);
            String text = document.get(offset, length);

            // Is this the last line?
            if (n == stop) {
                // Yes.
                text += getLastLineDelimiter(document, n);
            }

            LineReplaceResult line = new LineReplaceResult();
            line.startReplaceIndex = 0;
            line.rangeToReplace = length;
            line.textToReplace = text;
            lines.add(line);
        }

        return lines;
    }

    private String getLastLineDelimiter(IDocument document, int n) throws BadLocationException {
        // Does the last line already contain a line delimiter?
        String delimiter = document.getLineDelimiter(n);
        if (delimiter != null) {
            // Yes.
            return "";
        }

        // Is there a previous line?
        if (n > 1) {
            // Yes. So get the line delimiter from there.
            delimiter = document.getLineDelimiter(n - 1);
            if (delimiter != null) {
                return delimiter;
            }
        }

        // Last resort.
        return System.getProperty("line.separator");
    }

    private void sortLines(List lines) {
        Collections.sort(lines, getComparator());
    }

    private void addLines(TextReplaceResultSet results, List lineList) {
        Iterator lines = lineList.iterator();
        while (lines.hasNext()) {
            results.add((LineReplaceResult) lines.next());
        }
    }

    private ITextSelection getSelection() {
        ISelectionProvider selectionProvider = getEditor().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }

        ISelection selection = selectionProvider.getSelection();
        if (selection == null) {
            return null;
        }

        if (!(selection instanceof ITextSelection)) {
            return null;
        }

        return (ITextSelection) selection;
    }

}
