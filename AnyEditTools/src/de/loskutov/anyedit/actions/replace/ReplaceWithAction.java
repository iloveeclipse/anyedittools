/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.replace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrey
 */
public abstract class ReplaceWithAction extends AbstractHandler implements IObjectActionDelegate {

    protected ContentWrapper selectedContent;
    protected AbstractEditor editor;

    public ReplaceWithAction() {
        super();
        editor = new AbstractEditor(null);
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        Action dummyAction = new Action(){
            @Override
            public String getId() {
                return event.getCommand().getId();
            }
        };
        setActivePart(dummyAction, activePart);
        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        selectionChanged(dummyAction, currentSelection);
        if(dummyAction.isEnabled()) {
            run(dummyAction);
        }
        return null;
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if (targetPart instanceof IEditorPart) {
            editor = new AbstractEditor((IEditorPart) targetPart);
        } else {
            editor = new AbstractEditor(null);
        }
    }

    @Override
    public void run(IAction action) {
        InputStream stream = createInputStream();
        if (stream == null) {
            return;
        }
        replace(stream);
    }

    private void replace(InputStream stream) {
        if(selectedContent == null || !selectedContent.isModifiable()){
            return;
        }
        IDocument document = editor.getDocument();
        if (!editor.isDisposed() && document != null) {
            // replace selection only
            String text = getChangedCompareText(stream);
            ITextSelection selection = editor.getSelection();
            if (selection == null || selection.getLength() == 0) {
                document.set(text);
            } else {
                try {
                    document.replace(selection.getOffset(), selection.getLength(), text);
                } catch (BadLocationException e) {
                    AnyEditToolsPlugin.logError("Can't update text in editor", e);
                }
            }
            return;
        }
        replace(selectedContent, stream);
    }

    private String getChangedCompareText(InputStream stream) {
        StringWriter sw = new StringWriter();
        copyStreamToWriter(stream, sw);
        return sw.toString();
    }

    private void replace(ContentWrapper content, InputStream stream) {
        IFile file = content.getIFile();
        if (file == null || file.getLocation() == null) {
            saveExternalFile(content, stream);
            return;
        }
        try {
            if (!file.exists()) {
                file.create(stream, true, new NullProgressMonitor());
            } else {
                ITextFileBuffer buffer = EclipseUtils.getBuffer(file);
                try {
                    if (AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
                            IAnyEditConstants.SAVE_DIRTY_BUFFER)) {
                        if (buffer != null && buffer.isDirty()) {
                            buffer.commit(new NullProgressMonitor(), false);
                        }
                    }
                    if (buffer != null) {
                        buffer.validateState(new NullProgressMonitor(),
                                AnyEditToolsPlugin.getShell());
                    }
                } finally {
                    EclipseUtils.disconnectBuffer(buffer);
                }
                file.setContents(stream, true, true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            AnyEditToolsPlugin.errorDialog("Can't replace file content: " + file, e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("Failed to close stream", e);
            }
        }
    }

    private void copyStreamToWriter(InputStream stream, Writer writer){
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(stream, editor.computeEncoding());
            BufferedReader br = new BufferedReader(in);

            int i;
            while ((i = br.read()) != -1) {
                writer.write(i);
            }
            writer.flush();
        } catch (IOException e) {
            AnyEditToolsPlugin.logError("Error during reading/writing streams", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("Failed to close stream", e);
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("Failed to close stream", e);
            }
        }
    }

    private void saveExternalFile(ContentWrapper content, InputStream stream) {

        File file2 = null;
        IFile iFile = content.getIFile();
        if (iFile != null) {
            file2 = new File(iFile.getFullPath().toOSString());
        } else {
            file2 = content.getFile();
        }
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException e) {
                AnyEditToolsPlugin.errorDialog("Can't create file: " + file2, e);
                return;
            }
        }
        boolean canWrite = file2.canWrite();
        if (!canWrite) {
            AnyEditToolsPlugin.errorDialog("File is read-only: " + file2);
            return;
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file2));
            copyStreamToWriter(stream, bw);
        } catch (IOException e) {
            AnyEditToolsPlugin.logError("Error on saving to file: " + file2, e);
            return;
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                AnyEditToolsPlugin.logError("Error on saving to file: " + file2, e);
            }
        }


        if (iFile != null) {
            try {
                iFile.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
            } catch (CoreException e) {
                AnyEditToolsPlugin.logError("Failed to refresh file: " + iFile, e);
            }
        }
    }

    abstract protected InputStream createInputStream();


    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            if(!editor.isDisposed()){
                selectedContent = ContentWrapper.create(editor);
            }
            action.setEnabled(selectedContent != null);
            return;
        }
        IStructuredSelection sSelection = (IStructuredSelection) selection;
        Object firstElement = sSelection.getFirstElement();
        if(!editor.isDisposed()) {
            selectedContent = ContentWrapper.create(editor);
        } else {
            selectedContent = ContentWrapper.create(firstElement);
        }
        action.setEnabled(selectedContent != null && sSelection.size() == 1);
    }


}
