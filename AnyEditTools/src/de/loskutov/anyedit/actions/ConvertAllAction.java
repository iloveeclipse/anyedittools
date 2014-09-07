/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.ui.preferences.CombinedPreferences;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.LineReplaceResult;
import de.loskutov.anyedit.util.TextReplaceResultSet;

public class ConvertAllAction extends Action implements IActionDelegate, IWorkbenchWindowActionDelegate {

    protected List<IFile> selectedFiles;

    protected List<IResource> selectedResources;

    protected static final int MODIFIED = 1 << 0;

    protected static final int SKIPPED = 1 << 1;

    protected static final int ERROR = 1 << 2;

    private final Spaces spacesAction;

    protected final IContentType text_type = Platform.getContentTypeManager()
            .getContentType("org.eclipse.core.runtime.text");

    private Shell shell;

    public ConvertAllAction() {
        super();
        selectedFiles = new ArrayList<IFile>();
        selectedResources = new ArrayList<IResource>();
        spacesAction = new Spaces();
        spacesAction.setUsedOnSave(false);
    }

    @Override
    public void run(IAction action) {
        shell = AnyEditToolsPlugin.getShell();
        // selectedFiles contains all files for convert.
        WorkspaceJob job = new ConvertRunner("Converting 'Tabs<->Spaces'");
        job.schedule();
        PlatformUI.getWorkbench().getProgressService().showInDialog(shell, job);
    }

    protected int performAction(IFile file, ITextFileBufferManager manager,
            boolean saveIfDirty, IProgressMonitor monitor) throws CoreException {

        // set current file to action (required to get tab width from)
        spacesAction.setFile(file);
        CombinedPreferences preferences = spacesAction.getCombinedPreferences();

        String filterPerf = preferences.getString(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST);
        String[] filters = EclipseUtils.parseList(filterPerf);
        String actionId;
        if (spacesAction.isDefaultTabToSpaces()) {
            actionId = AbstractTextAction.ACTION_ID_CONVERT_TABS;
        } else {
            actionId = AbstractTextAction.ACTION_ID_CONVERT_SPACES;
        }

        // 1) get file name. filter all excluded in preferences
        if (matchFilter(file, filters)) {
            return SKIPPED;
        }

        // 2) get content type. filter all non-text files
        if (hasWrongContentType(file, monitor)) {
            return SKIPPED;
        }

        // do the main work
        return convertFile(actionId, file, manager, saveIfDirty, monitor);
    }

    private int convertFile(final String actionId, IFile file,
            ITextFileBufferManager manager, boolean saveIfDirty, IProgressMonitor monitor) throws CoreException {
        int result = ERROR;
        IPath fullPath = file.getFullPath();
        try {
            manager.connect(fullPath, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
            monitor.subTask(fullPath.makeRelative().toString());
            final ITextFileBuffer fileBuffer = manager.getTextFileBuffer(fullPath, LocationKind.IFILE);

            if(!file.isSynchronized(IResource.DEPTH_ZERO)){
                file.refreshLocal(IResource.DEPTH_ZERO, monitor);
            }
            // check if buffer is opened by some editor - save it first, if required
            boolean wasDirty = fileBuffer.isDirty();
            if (saveIfDirty && wasDirty && fileBuffer.isCommitable()) {
                fileBuffer.commit(new SubProgressMonitor(monitor, 2), false);
            }

            // 4) perform convert in-memory
            result = convertBuffer(actionId, file, fileBuffer, monitor);

            if (result == MODIFIED && (!wasDirty || saveIfDirty)) {
                if(fileBuffer.isShared()){
                    // convertBuffer() should checkout the file, but...
                    // because convertBuffer() operation was running in the UI thread,
                    // the checkout file operation task may be still incomplete
                    // second call to check-out file from VCS, if any
                    fileBuffer.validateState(monitor, shell);
                }
                fileBuffer.commit(new SubProgressMonitor(monitor, 2), false);
            }

        } finally {
            // clean up - action shouldn't have old file reference
            spacesAction.setFile(null);
            manager.disconnect(fullPath, LocationKind.IFILE, new SubProgressMonitor(monitor, 1));
        }
        return result;
    }

    private int convertBuffer(String actionId, final IFile file,
            ITextFileBuffer fileBuffer, IProgressMonitor monitor) throws CoreException {
        final IDocument document = fileBuffer.getDocument();
        final TextReplaceResultSet resultSet = spacesAction.estimateActionRange(document);
        // no lines affected - return immediately
        if (resultSet.getNumberOfLines() == 0) {
            return SKIPPED;
        }

        // perform memory based replace, the result will contain all changed lines
        try {
            spacesAction.doTextOperation(document, actionId, resultSet);
        } catch (Exception ex) {
            AnyEditToolsPlugin.logError("doTextOperation() failed on: " + file, ex);
            return ERROR;
        }

        if (!resultSet.areResultsChanged()) {
            return SKIPPED;
        }

        int result = ERROR;

        // check-out file from VCS, if any
        fileBuffer.validateState(monitor, shell);

        // if buffer is shared, then it means, that this operation could affect
        // changes in the UI thread because of associated editors and we *must*
        // to run it in the UI Thread too...
        if (fileBuffer.isShared()) {
            shell.getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    writeDocument(file, document, resultSet);
                }
            });
        } else {
            writeDocument(file, document, resultSet);
        }
        if (resultSet.getException() != null) {
            result = ERROR;
        } else {
            result = MODIFIED;
        }
        return result;
    }

    void writeDocument(IFile file, IDocument document, TextReplaceResultSet resultSet) {
        int docLinesNbr = document.getNumberOfLines();
        int changedLinesNbr = resultSet.getNumberOfLines();
        boolean rewriteWholeDoc = changedLinesNbr >= docLinesNbr;

        // some oddities with document??? prevent overflow in changedLinesNbr
        if (rewriteWholeDoc) {
            changedLinesNbr = docLinesNbr;
        }

        // this operation could affect changes in UI thread because of associated editors
        final DocumentRewriteSession rewriteSession = startSequentialRewriteMode(document);
        try {
            for (int i = 0; i < changedLinesNbr; i++) {
                LineReplaceResult trr = resultSet.get(i);
                if (trr != null) {
                    IRegion lineInfo = document.getLineInformation(i
                            + resultSet.getStartLine());
                    document.replace(lineInfo.getOffset() + trr.startReplaceIndex,
                            trr.rangeToReplace, trr.textToReplace);
                }
            }
        } catch (Exception e) {
            resultSet.setException(e);
            AnyEditToolsPlugin.logError(
                    "Error during write document for file: " + file, e);
        } finally {
            stopSequentialRewriteMode(document, rewriteSession);
            resultSet.clear();
        }
    }

    private DocumentRewriteSession startSequentialRewriteMode(IDocument document) {
        if (document instanceof IDocumentExtension4) {
            IDocumentExtension4 extension = (IDocumentExtension4) document;
            return extension.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
        }
        if (document instanceof IDocumentExtension) {
            IDocumentExtension extension = (IDocumentExtension) document;
            extension.startSequentialRewrite(false);
        }
        return null;
    }

    private void stopSequentialRewriteMode(IDocument document,
            DocumentRewriteSession rewriteSession) {
        if (document instanceof IDocumentExtension4) {
            IDocumentExtension4 extension = (IDocumentExtension4) document;
            extension.stopRewriteSession(rewriteSession);
        } else if (document instanceof IDocumentExtension) {
            IDocumentExtension extension = (IDocumentExtension) document;
            extension.stopSequentialRewrite();
        }
    }

    private boolean hasWrongContentType(IFile file, IProgressMonitor monitor) {
        try {
            IContentDescription contentDescr = file.getContentDescription();
            if (contentDescr == null) {
                return true;
            }
            IContentType contentType = contentDescr.getContentType();
            if (contentType == null) {
                return true;
            }
            return !contentType.isKindOf(text_type);
            //
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError("Could not get content type for: " + file, e);
        }
        return false;
    }

    private static boolean matchFilter(IFile file, String[] filters) {
        return EclipseUtils.matchFilter(file.getName(), filters);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            Iterator<?> iterator = ssel.iterator();
            selectedFiles.clear();
            selectedResources.clear();
            while (iterator.hasNext()) {
                // by definition in plugin.xml, we are called only on IFile's
                selectedFiles.add((IFile) iterator.next());
            }
        }
    }

    protected final class ConvertRunner extends WorkspaceJob {

        public ConvertRunner(String name) {
            super(name);
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) {
            monitor.beginTask(Messages.ConvertAll_task, selectedFiles.size());
            int filesToConvert = selectedFiles.size();
            IPreferenceStore preferenceStore = AnyEditToolsPlugin.getDefault()
                    .getPreferenceStore();

            boolean saveIfDirty = preferenceStore
                    .getBoolean(IAnyEditConstants.SAVE_DIRTY_BUFFER);
            int modified = 0;
            int skipped = 0;
            List<IStatus> errors = new ArrayList<IStatus>();
            long start = System.currentTimeMillis();

            try {
                ITextFileBufferManager manager1 = FileBuffers.getTextFileBufferManager();
                for (int i = 0; i < filesToConvert && !monitor.isCanceled(); i++) {
                    monitor.internalWorked(1);
                    IFile file = selectedFiles.get(i);
                    try {
                        int result = performAction(file, manager1, saveIfDirty, monitor);
                        if (result == ERROR) {
                            errors.add(new Status(IStatus.ERROR, AnyEditToolsPlugin.getId(),
                                    "'Tabs<->Spaces' operation failed for file: " + file, null));
                        } else if (result == MODIFIED) {
                            modified++;
                        } else if (result == SKIPPED) {
                            skipped++;
                        }
                    } catch (CoreException e) {
                        errors.add(new Status(IStatus.ERROR, AnyEditToolsPlugin.getId(),
                                "'Tabs<->Spaces' operation failed for file: " + file, e));
                    }
                }
            } finally {
                monitor.done();
                selectedFiles.clear();
                selectedResources.clear();
                long stop = System.currentTimeMillis();
                long msec = (stop - start);
                AnyEditToolsPlugin.logInfo("Tabs<->Spaces: modified " +
                        modified + " files from "
                        + filesToConvert + ", ignored "
                        + skipped + ", failed on: " + errors.size() + " ("
                        + msec + " ms)");

            }
            if(errors.size() == 0){
                if(monitor.isCanceled()){
                    AnyEditToolsPlugin.logError("'Tabs<->Spaces' operation cancelled by user", null);
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
            MultiStatus error = new MultiStatus(AnyEditToolsPlugin.getId(), IStatus.ERROR, "'Tabs<->Spaces' operation failed for "
                    + errors.size()
                    + " files. Please check log for details.", null);
            for (IStatus status : errors) {
                error.add(status);
            }
            return error;
        }
    }

    @Override
    public void dispose() {
        selectedFiles.clear();
        selectedResources.clear();
    }

    @Override
    public void init(IWorkbenchWindow window) {
        // do nothing
    }

}
