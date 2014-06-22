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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;

import de.loskutov.anyedit.util.EclipseUtils;

public class ClipboardStreamContent implements StreamContent, IStreamContentAccessor {

    private final String type;
    private final String newLine;
    private final String clipboardContent;
    private final String charset;
    private final String lineSeparator;
    private byte [] bytes;

    /**
     * @param type NOT null
     * @param newLine might be null
     */
    public ClipboardStreamContent(String type, String newLine, String charset) {
        this(type, newLine, charset, EclipseUtils.getClipboardContent());
    }

    private ClipboardStreamContent(String type, String newLine, String charset,
            String clipboardContent) {
        this.charset = charset;
        this.type = type;
        this.newLine = newLine;
        this.clipboardContent = clipboardContent;
        lineSeparator = System.getProperty("line.separator");
    }

    @Override
    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    @Override
    public String getName() {
        return "Clipboard";
    }

    @Override
    public String getType() {
        return type != null? type : ITypedElement.UNKNOWN_TYPE;
    }

    @Override
    public Object[] getChildren() {
        return new StreamContent[0];
    }

    @Override
    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        return true;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public InputStream getContents() throws CoreException {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void dispose() {
        bytes = null;
    }

    @Override
    public boolean isDisposed() {
        return bytes == null;
    }

    @Override
    public void init(AnyeditCompareInput input) {
        if(clipboardContent != null){
            if(newLine == null || newLine.equals(lineSeparator)){
                try {
                    bytes = clipboardContent.getBytes(charset);
                } catch (UnsupportedEncodingException e) {
                    bytes = clipboardContent.getBytes();
                }
                return;
            }
            String replaceAll = clipboardContent.replaceAll(lineSeparator, newLine);
            try {
                bytes = replaceAll.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                bytes = replaceAll.getBytes();
            }
            return;
        }
        bytes = new byte[0];
    }

    @Override
    public StreamContent recreate() {
        ClipboardStreamContent newContent = new ClipboardStreamContent(type, newLine, charset,
                clipboardContent);
        return newContent;
    }

    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public void setDirty(boolean dirty) {
        // noop
    }
}
