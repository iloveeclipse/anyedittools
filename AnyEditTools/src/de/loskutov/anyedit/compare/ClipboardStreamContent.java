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
    private String clipboardContent;
    private final String charset;

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
    }

    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    public String getName() {
        return "Clipboard";
    }

    public String getType() {
        return type != null? type : ITypedElement.UNKNOWN_TYPE;
    }

    public Object[] getChildren() {
        return new StreamContent[0];
    }

    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        return true;
    }

    public boolean isDirty() {
        return false;
    }

    public InputStream getContents() throws CoreException {
        if(clipboardContent != null){
            String property = System.getProperty("line.separator");
            if(newLine == null || newLine.equals(property)){
                try {
                    return new ByteArrayInputStream(clipboardContent.getBytes(charset));
                } catch (UnsupportedEncodingException e) {
                    return new ByteArrayInputStream(clipboardContent.getBytes());
                }
            }
            String replaceAll = clipboardContent.replaceAll(property, newLine);
            try {
                return new ByteArrayInputStream(replaceAll.getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                return new ByteArrayInputStream(replaceAll.getBytes());
            }
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    public void dispose() {
        clipboardContent = null;
    }

    public boolean isDisposed() {
        return clipboardContent == null;
    }

    public void init(AnyeditCompareInput input) {
        // noop
    }

    public StreamContent recreate() {
        ClipboardStreamContent newContent = new ClipboardStreamContent(type, newLine, charset,
                clipboardContent);
        return newContent;
    }

    public String getFullName() {
        return getName();
    }
}
