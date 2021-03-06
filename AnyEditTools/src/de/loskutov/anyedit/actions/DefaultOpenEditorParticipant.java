/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IOpenEditorsParticipant;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.TextUtil;
import de.loskutov.anyedit.util.TextUtil.LineAndCaret;

/**
 * @author Andrey
 */
public class DefaultOpenEditorParticipant implements IOpenEditorsParticipant {

    public DefaultOpenEditorParticipant() {
        super();
    }

    @Override
    public int getPriority() {
        // a bit higher as default
        return PRIO_DEFAULT + 1;
    }

    @Override
    public int guessLine(IDocument doc, ISelectionProvider selectionProvider,
            IWorkbenchPart editorPart) {
        if (doc == null) {
            return -1;
        }
        // only if line information available
        int caretPosition = EclipseUtils.getCaretPosition(selectionProvider);
        String line = getLinePart(doc, caretPosition);
        int lineReference = TextUtil.getDefaultTextUtilities().findLineReferenceRegex(line, 0);
        return lineReference;
    }

    private static String getLinePart(IDocument doc, int caretPosition) {
        String linePart = null;
        // try to get line after caret
        try {
            IRegion line = doc.getLineInformation(doc.getLineOfOffset(caretPosition));
            linePart = doc.get(line.getOffset(), line.getLength());
            int startOffset = caretPosition - line.getOffset();
            if (linePart != null && startOffset < linePart.length()) {
                linePart = linePart.substring(startOffset);
            }
        } catch (BadLocationException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        return linePart;
    }

    @Override
    public IEditorPart openEditor(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IFile file) throws OperationCanceledException {
        IEditorPart editorPart = null;
        if (file != null) {
            try {
                // external files do not have location in workspace...
                if (file.getLocation() == null) {
                    editorPart = openExternalEditor(file);
                } else {
                    editorPart = openInternalEditor(file);
                }
            } catch (CoreException e) {
                AnyEditToolsPlugin.logError("'Open file' operation failed", e);
            }
        }
        return editorPart;
    }

    private static IEditorPart openInternalEditor(IFile file) throws PartInitException {
        return IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage(), file, true);
    }

    private static IEditorPart openExternalEditor(IFile file) throws CoreException {
        File file2 = new File(file.getFullPath().toOSString());
        if (!file2.isFile()) {
            return null;
        }
        String editorId = getEditorId(file2);
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage();
        IFileStore fileStore;
        try {
            fileStore = EFS.getLocalFileSystem().getStore(
                    new Path(file2.getCanonicalPath()));
            IEditorInput input = new FileStoreEditorInput(fileStore);
            return page.openEditor(input, editorId);
        } catch (IOException e) {
            AnyEditToolsPlugin.logError("Could not get canonical file path", e);
        }
        return null;
    }

    private static String getEditorId(File file) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IEditorRegistry editorRegistry = workbench.getEditorRegistry();
        IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(file.getName(),
                getContentType(file));
        if (descriptor != null) {
            return descriptor.getId();
        }
        return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
    }

    private static IContentType getContentType(File file) {
        if (file == null) {
            return null;
        }

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return Platform.getContentTypeManager().findContentTypeFor(stream,
                    file.getName());
        } catch (IOException e) {
            AnyEditToolsPlugin.logError("'Open file' operation failed", e);
            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("'Open file' operation failed", e);
            }
        }
    }

    @Override
    public IFile guessFile(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IWorkbenchPart currentPart) throws OperationCanceledException {
        List<IFile> files = guessFiles(doc, selectionProvider, currentInput, currentPart);
        return files.isEmpty()? null : files.get(0);
    }

    @Override
    public List<IFile> guessFiles(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IWorkbenchPart currentPart) throws OperationCanceledException {
        String selection = EclipseUtils.getSelectedText(selectionProvider);
        List<String> pathStrings = guessPaths(doc, selectionProvider, selection);
        List<IFile> files = getFromPath(currentInput, pathStrings, selection, currentPart);
        return files;
    }

    private static List<String> guessPaths(IDocument doc, ISelectionProvider selectionProvider,
            String selection) {
        TextUtil textUtil = TextUtil.getDefaultTextUtilities();
        String selectedText = textUtil.trimPath(selection);
        if(selectedText == null) {
            return Collections.EMPTY_LIST;
        }
        boolean mayBeFilePath = textUtil.isFilePath(selectedText);
        if (!mayBeFilePath) {
            // try to search around caret
            if (doc != null) {
                int caretPosition = EclipseUtils.getCaretPosition(selectionProvider);
                try {
                    IRegion line = doc.getLineInformation(doc.getLineOfOffset(caretPosition));
                    String lineText = doc.get(line.getOffset(), line.getLength());
                    selectedText = textUtil.trimPath(lineText);
                    int index = lineText.indexOf(selectedText);
                    selectedText = textUtil.findPath(new LineAndCaret(selectedText, caretPosition - index - line.getOffset()));
                } catch (BadLocationException e) {
                    AnyEditToolsPlugin.logError("", e);
                    selectedText = textUtil.findPath(new LineAndCaret(selectedText, selectedText.length() / 2));
                }
            } else {
                // virtual caret in the middle of string
                selectedText = textUtil.findPath(new LineAndCaret(selectedText, selectedText.length() / 2));
            }
            if(selectedText == null) {
                return Collections.EMPTY_LIST;
            }
        }
        if (selectedText.length() == 0) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(selectedText);
    }

    private static List<IFile> getFromPath(IEditorInput currentInput, List<String> pathStrings, String selection,
            IWorkbenchPart currentPart) throws OperationCanceledException {
        if (pathStrings.isEmpty()) {
            return getFromSelection(selection);
        }
        IProject project = EclipseUtils.getProject(currentInput);
        // nothing found with current input? try with viewPart
        if (project == null) {
            project = EclipseUtils.getProject(currentPart);
        }
        List<IFile> files = new ArrayList<>();
        for (String path : pathStrings) {
            IFile resource = EclipseUtils.getResource(project, currentInput, path);
            if (resource != null && !files.contains(resource)) {
                files.add(resource);
            }
        }
        if(!files.isEmpty()) {
            return files;
        }
        // no luck with guessed path string - so try the entire selection instead
        return getFromSelection(selection);
    }

    private static List<IFile> getFromSelection(String selection) throws OperationCanceledException {

        if (selection == null || selection.length() == 0) {
            return EclipseUtils.queryFiles(null, ResourcesPlugin.getWorkspace().getRoot());
        }
        Path path = new Path(selection);
        // remove all stuff but let only the file name there
        String pathString = selection;
        if (path.segmentCount() > 1) {
            pathString = path.lastSegment();
        }
        return EclipseUtils.queryFiles(pathString, ResourcesPlugin.getWorkspace()
                .getRoot());
    }

}
