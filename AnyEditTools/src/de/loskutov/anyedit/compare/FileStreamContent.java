/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * Content for workspace files without document support.
 * @author Andrei
 */
public class FileStreamContent extends ResourceNode implements StreamContent {

    private boolean dirty;
    private final ContentWrapper content;

    public FileStreamContent(ContentWrapper content) {
        super(content.getIFile());
        this.content = content;
    }

    public void setContent(byte[] contents) {
        dirty = true;
        super.setContent(contents);
    }

    public String getFullName() {
        return content.getFullName();
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        if (!dirty) {
            return true;
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

    public void dispose() {
        // noop
    }

    public boolean isDisposed() {
        return false;
    }

    public void init(AnyeditCompareInput input) {
        // noop
    }

    public StreamContent recreate() {
        return new FileStreamContent(content);
    }

}
