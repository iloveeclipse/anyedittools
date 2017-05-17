/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An extension to the {@link IOpenEditorParticipant}, which is capable to guess <b>multiple</b> files
 *
 * @author Andrey
 */
public interface IOpenEditorsParticipant extends IOpenEditorParticipant {

    /**
     * Try to guess multiple files in given context
     *
     * @param doc
     *            document with possible editor/file reference, might be null
     * @param selectionProvider
     *            selection in the document, might be null or empty
     * @param currentInput
     *            document input, might be null
     * @param currentPart
     *            current part, if any (might be null)
     * @return empty list if no file information was found, otherwise the list of guessed files, never null
     * @throws OperationCanceledException
     *             if user decided to cancel operation
     */
    List<IFile> guessFiles(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IWorkbenchPart currentPart)
                    throws OperationCanceledException;

}
