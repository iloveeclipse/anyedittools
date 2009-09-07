/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * Content for external files without document support.
 * @author Andrei
 */
public class ExternalFileStreamContent extends BufferedContent implements StreamContent,
    IEditableContent, IModificationDate, IEditableContentExtension {

    protected boolean dirty;
    private final ContentWrapper content;


    public ExternalFileStreamContent(ContentWrapper content) {
        super();
        this.content = content;
    }

    public void setContent(byte[] contents) {
        dirty = true;
        super.setContent(contents);
    }

    public Image getImage() {
        return CompareUI.getImage(content.getFileExtension());
    }

    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        if (!dirty) {
            return true;
        }

        byte[] bytes = getContent();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(content.getFile());
            fos.write(bytes);
            return true;
        } catch (IOException e) {
            AnyEditToolsPlugin.errorDialog(
                    "Can't store compare buffer to external file: " + content.getFile(), e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    protected InputStream createStream() throws CoreException {
        FileInputStream fis;
        try {
            fis = new FileInputStream(content.getFile());
            return fis;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getName() {
        return content.getName();
    }

    public String getFullName() {
        return content.getFullName();
    }

    public String getType() {
        return content.getFileExtension();
    }

    public Object[] getChildren() {
        return new StreamContent[0];
    }

    public boolean isEditable() {
        return true;
    }

    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
        return null;
    }

    public long getModificationDate() {
        return content.getFile().lastModified();
    }

    public boolean isReadOnly() {
        return !content.getFile().canWrite();
    }

    public IStatus validateEdit(Shell shell) {
        File file = content.getFile();
        if(file.canWrite()) {
           return Status.OK_STATUS;
        }
        FileInfo fi = new FileInfo(file.getAbsolutePath());
        fi.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
        try {
            IFileStore store = EFS.getStore(URIUtil.toURI(file.getAbsolutePath()));
            store.putInfo(fi, EFS.SET_ATTRIBUTES, null);
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError("Can't make file writable: " + file, e);
        }
        if(file.canWrite()) {
            return Status.OK_STATUS;
        }
        return Status.CANCEL_STATUS;
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
        return new ExternalFileStreamContent(content);
    }

}
