/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *      Clemens Fuchslocher - initial API and implementation
 *      Andrey Loskutov     - bugfixes
 *******************************************************************************/
package de.loskutov.anyedit.actions.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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

    @Override
    protected TextReplaceResultSet estimateActionRange(IDocument document) {
        ITextSelection selection = getSelection();
        if (selection == null || selection.isEmpty()) {
            return new TextReplaceResultSet();
        }

        TextReplaceResultSet result = new TextReplaceResultSet();
        if(selection.getLength() > 0) {
            result.setStartLine(selection.getStartLine());
            result.setStopLine(selection.getEndLine());
        } else {
            result.setStartLine(0);
            result.setStopLine(document.getNumberOfLines() - 1);
        }
        return result;
    }

    @Override
    protected void doTextOperation(IDocument document, String action, TextReplaceResultSet results) throws BadLocationException {
        List lines = getLines(results, document);
        if (lines.isEmpty() || lines.size() == 1) {
            return;
        }

        sortLines(lines);
        addLines(results, lines);
    }

    private List getLines(TextReplaceResultSet result, IDocument document) throws BadLocationException {
        List lines = new ArrayList();
        int start = result.getStartLine();
        int stop = result.getStopLine();

        for (int n = start; n <= stop; n++) {
            IRegion lineInfo = document.getLineInformation(n);
            String text = document.get(lineInfo.getOffset(), lineInfo.getLength());
            LineReplaceResult line = new LineReplaceResult();
            line.startReplaceIndex = 0;
            line.rangeToReplace = -1;
            line.textToReplace = text;
            lines.add(line);
        }
        return lines;
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
