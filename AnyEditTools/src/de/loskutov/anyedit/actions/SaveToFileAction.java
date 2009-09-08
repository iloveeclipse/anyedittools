/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.util.EclipseUtils;

public class SaveToFileAction extends AbstractOpenAction {

    private static final int CANCEL = -1;

    private static final int APPEND = 0;

    private static final int OVERRIDE = 1;

    private static String lastUsedFile;

    public SaveToFileAction() {
        super();
    }

    protected void handleAction(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput) {

        IPreferenceStore prefs = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        boolean shouldAsk = prefs.getBoolean(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS);
        boolean shouldOpenEditor = prefs
                .getBoolean(IAnyEditConstants.SAVE_TO_OPEN_EDITOR);
        boolean ignoreSelection = prefs
                .getBoolean(IAnyEditConstants.SAVE_TO_IGNORE_SELECTION);
        ITextSelection selection = (ITextSelection) selectionProvider.getSelection();
        boolean hasSelection = false;
        if (!ignoreSelection && selection != null && selection.getLength() > 0) {
            hasSelection = true;
        } else {
            selection = new TextSelection(doc, 0, doc.getLength());
        }

        /*
         * Show dialog if prefs is set, asking for open in editor
         */
        if (shouldAsk) {
            MessageDialogWithToggle dialogWithToggle = MessageDialogWithToggle
                    .openYesNoCancelQuestion(AnyEditToolsPlugin.getShell(),
                            Messages.SaveTo_ShouldOpen,
                            hasSelection ? Messages.SaveTo_MessageSelection
                                    : Messages.SaveTo_MessageNoSelection,
                            Messages.SaveTo_MessageToggle, false, prefs,
                            IAnyEditConstants.SAVE_TO_SHOW_OPTIONS);

            int returnCode = dialogWithToggle.getReturnCode();
            if (returnCode != IDialogConstants.YES_ID
                    && returnCode != IDialogConstants.NO_ID) {
                return;
            }
            shouldOpenEditor = returnCode == IDialogConstants.YES_ID;
            if (!prefs.getBoolean(IAnyEditConstants.SAVE_TO_SHOW_OPTIONS)) {
                prefs.setValue(IAnyEditConstants.SAVE_TO_OPEN_EDITOR, shouldOpenEditor);
            }
        }

        /*
         * open file selection dialog (external)
         */
        File file = getFileFromUser();
        if (file == null) {
            return;
        }

        /*
         * if selected file exists, ask for override/append/another file
         */
        int overrideOrAppend = checkForExisting(file);
        if (overrideOrAppend == CANCEL) {
            return;
        }

        IFile iFile = EclipseUtils.getIFile(new Path(file.getAbsolutePath()));
        /*
         * if selected file is in the workspace, checkout it or show error message
         */
        if (iFile == null || !checkout(iFile, overrideOrAppend)) {
            return;
        }

        /*
         * save it
         */
        doSave(doc, selection, file, overrideOrAppend);

        /*
         * and if option is on, open in editor
         */
        if (shouldOpenEditor) {
            new DefaultOpenEditorParticipant().openEditor(doc, selectionProvider,
                    currentInput, iFile);
        }
    }

    private void doSave(IDocument doc, ITextSelection selection, File file,
            int overrideOrAppend) {

        int startSelection = selection.getOffset();
        int stopSelection = startSelection + selection.getLength();
        int endLine = selection.getEndLine();

        FileWriter fw;
        try {
            fw = new FileWriter(file, overrideOrAppend == APPEND);
        } catch (IOException e) {
            AnyEditToolsPlugin.errorDialog("Couldn't open file for writing: " + file, e);
            return;
        }

        try {
            for (int i = selection.getStartLine(); i <= endLine; i++) {
                IRegion lineInfo = doc.getLineInformation(i);

                boolean useStartLineOffset = lineInfo.getOffset() > startSelection;
                int startIndex;
                if (useStartLineOffset) {
                    startIndex = 0;
                } else {
                    startIndex = startSelection - lineInfo.getOffset();
                }

                int stopIndex;
                boolean useStopLineOffset = lineInfo.getOffset() + lineInfo.getLength() > stopSelection;
                if (useStopLineOffset) {
                    stopIndex = stopSelection - lineInfo.getOffset();
                } else {
                    stopIndex = lineInfo.getLength();
                }
                String line = doc.get(lineInfo.getOffset() + startIndex, stopIndex
                        - startIndex);

                if (line == null) {
                    continue;
                }
                fw.write(line);
                String lineDelimiter = doc.getLineDelimiter(doc
                        .getLineOfOffset(stopIndex));
                if (lineDelimiter != null) {
                    fw.write(lineDelimiter);
                }
                fw.flush();
            }
        } catch (Exception e) {
            AnyEditToolsPlugin.errorDialog("Error during writing to file: " + file, e);
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("Couldn't close file: " + file, e);
            }
        }

    }

    /**
     * @param file
     *            non null
     * @param overrideOrAppend
     * @return true if file doesn't exist and was created or writable
     */
    private boolean checkout(IFile file, int overrideOrAppend) {
        if (file.getLocation() == null) {
            File file2 = new File(file.getFullPath().toOSString());
            if (!file2.exists()) {
                try {
                    file2.createNewFile();
                } catch (IOException e) {
                    AnyEditToolsPlugin.errorDialog("Couldn't create file: " + file, e);
                    return false;
                }
            }
            boolean canWrite = file2.canWrite();
            if (!canWrite) {
                AnyEditToolsPlugin.errorDialog("File is read-only: " + file);
            }
            return canWrite;
        }
        try {
            if (overrideOrAppend == APPEND && file.exists()) {
                file.appendContents(new ByteArrayInputStream(new byte[0]), true, true,
                        new NullProgressMonitor());
            } else {
                if (file.exists()) {
                    file.delete(true, new NullProgressMonitor());
                }
                file.create(new ByteArrayInputStream(new byte[0]), true,
                        new NullProgressMonitor());
            }
        } catch (CoreException e) {
            AnyEditToolsPlugin.errorDialog("File is read-only: " + file, e);
            return false;
        }
        return true;
    }

    /**
     * @param file
     *            non null
     * @return OVERRIDE if file not exists or exists and may be overriden, APPEND if it
     *         exists and should be reused, CANCEL if action should be cancelled
     */
    private int checkForExisting(File file) {
        if (file.exists()) {
            MessageDialog md = new MessageDialog(AnyEditToolsPlugin.getShell(),
                    Messages.SaveTo_ShouldOpen, null, Messages.SaveTo_FileExists,
                    MessageDialog.WARNING, new String[] { Messages.SaveTo_Append,
                            Messages.SaveTo_Override, "Cancel" }, 0);
            int result = md.open();
            switch (result) {
            case APPEND: // Append btn index
                return APPEND;
            case OVERRIDE: // Override btn index
                return OVERRIDE;
            default:
                return CANCEL;
            }
        }
        return OVERRIDE;
    }

    private File getFileFromUser() {
        FileDialog fd = new FileDialog(AnyEditToolsPlugin.getShell());
        if (lastUsedFile == null) {
            String property = System.getProperty("user.home");
            fd.setFilterPath(property);
        } else {
            fd.setFileName(lastUsedFile);
        }
        fd.setFilterExtensions(new String[] { "*.log", "*.txt" });
        String fileStr = fd.open();
        if (fileStr != null) {
            lastUsedFile = fileStr;
            return new File(fileStr);
        }
        return null;
    }

}
