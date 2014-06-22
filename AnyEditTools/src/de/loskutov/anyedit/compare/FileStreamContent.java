/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * Content for workspace files without document support.
 * @author Andrey
 */
public class FileStreamContent extends ResourceNode implements StreamContent {

    private boolean dirty;
    private final ContentWrapper content;
    private EditableSharedDocumentAdapter sharedDocumentAdapter;

    public FileStreamContent(ContentWrapper content) {
        super(content.getIFile());
        this.content = content;
    }

    @Override
    public void setContent(byte[] contents) {
        dirty = true;
        super.setContent(contents);
    }

    @Override
    public String getFullName() {
        return content.getFullName();
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        if (!dirty) {
            return true;
        }
        if (sharedDocumentAdapter != null) {
            boolean documentSaved = sharedDocumentAdapter.saveDocument(
                    sharedDocumentAdapter.getDocumentKey(this), true, pm);
            if(documentSaved) {
                dirty = false;
            }
            return documentSaved;
        }
        byte[] bytes = getContent();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        IFile file = content.getIFile();
        try {
            if (file.exists()) {
                file.setContents(is, true, true, pm);
            } else {
                file.create(is, true, pm);
            }
            dirty = false;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                AnyEditToolsPlugin.logError(
                        "Can't save changed compare buffer for file: " + file, e);
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        discardBuffer();
        if (sharedDocumentAdapter != null) {
            sharedDocumentAdapter.releaseBuffer();
        }
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void init(AnyeditCompareInput input) {
        getContent();
    }

    @Override
    public StreamContent recreate() {
        return new FileStreamContent(content);
    }

    @Override
    public Object getAdapter(Class adapter) {

        if (adapter == ISharedDocumentAdapter.class) {
            return getSharedDocumentAdapter();
        }
        if(adapter == IFile.class) {
            return content.getIFile();
        }
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    /**
     * The code below is copy from org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement
     * and is required to add full Java editor capabilities (content assist, navigation etc) to the compare editor
     * @return
     */
    private ISharedDocumentAdapter getSharedDocumentAdapter() {
        if (sharedDocumentAdapter == null) {
            sharedDocumentAdapter = new EditableSharedDocumentAdapter(this);
        }
        return sharedDocumentAdapter;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
