/*******************************************************************************
 * Copyright (c) 2007 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A participant in "open file" action, which is capable to guess the file/line and/or
 * open appropriated editor based on provided information.
 * <p>
 * The method of participants would be called in the followed order:
 * guessFile-&gt;openEditor-&gt;guessLine
 * <p>
 * Because there might be multiple participants with same priority, the order of operation
 * might be undefined. If one of participants was successful in guessing the file/opening
 * the editor, other participants would be ignored.
 *
 * @author Andrei
 */
public interface IOpenEditorParticipant {

    /** default prio with value 5 */
    int PRIO_DEFAULT = 5;

    /** low prio with value 0 */
    int PRIO_LOW = 0;

    /** high prio with value 10 */
    int PRIO_HIGH = 10;

    /**
     * Try to guess file under cursor
     *
     * @param doc
     *            document with possible editor/file reference, might be null
     * @param selectionProvider
     *            selection in the document, might be null or empty
     * @param currentInput
     *            document input, might be null
     * @param currentPart
     *            current part, if any (might be null)
     * @return null if no file information was found, otherwise the file object
     * @throws OperationCanceledException
     *             if user decided to cancel operation
     */
    IFile guessFile(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IWorkbenchPart currentPart)
            throws OperationCanceledException;

    /**
     * Opens editor
     * <p>
     * There is always at least one default participant which is capable to open file if
     * it is not null, therefore it is ok to return null in this case and don't
     * reimplement the standart file opening strategy.
     *
     * @param doc
     *            document with possible editor/file reference, might be null
     * @param selectionProvider
     *            selection in the document, might be null or empty
     * @param currentInput
     *            document input, might be null
     * @param file
     *            document file, might be null
     * @return null if editor part was not opened, otherwise opened editor reference.
     * @throws OperationCanceledException
     *             if user decided to cancel operation
     */
    IEditorPart openEditor(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IFile file) throws OperationCanceledException;

    /**
     * Try to guess the line under cursor. Would be called only if the editor part was
     * opened.
     *
     * @param doc
     *            document with possible line reference, might be null
     * @param selectionProvider
     *            selection in the document, might be null or empty
     * @param currentPart
     *            might be null, the sourse part from where we trying to guess the line
     *            information, NOT the target part which is opened before
     * @return -1 if operation was not successful, otherwise the line number
     * @throws OperationCanceledException
     *             if user decided to cancel operation
     */
    int guessLine(IDocument doc, ISelectionProvider selectionProvider,
            IWorkbenchPart currentPart) throws OperationCanceledException;

    /**
     * Priority defines the order of participation, the range is PRIO_LOW to PRIO_HIGH,
     * participant with prio PRIO_HIGH is the first participant. If there are more then
     * one participant, then order is not guaranteed. It is recommended to use
     * PRIO_DEFAULT as default :)
     *
     * @return one of PRIO_* constants
     */
    int getPriority();
}
