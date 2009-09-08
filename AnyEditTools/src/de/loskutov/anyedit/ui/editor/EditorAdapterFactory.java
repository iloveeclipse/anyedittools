/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.editor;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.util.EclipseUtils;

public class EditorAdapterFactory implements IAdapterFactory {

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if(adaptableObject instanceof FileStoreEditorInput) {
            FileStoreEditorInput editorInput = (FileStoreEditorInput) adaptableObject;
            if(adapterType == File.class) {
                URI uri = editorInput.getURI();
                return EclipseUtils.getLocalFile(uri);
            }
        }

        // "Unnamed editor" support, see org.eclipse.ui.internal.editors.text.NonExistingFileEditorInput
        if(adaptableObject instanceof ILocationProvider) {
            //            ILocationProvider provider = (ILocationProvider) adaptableObject;
            //            if(adapterType == File.class) {
            //                IPath path = provider.getPath(provider);
            //                return path == null ? null : path.toFile();
            //            }
            IEditorPart activeEditor = EclipseUtils.getActiveEditor();
            if(activeEditor instanceof ITextEditor) {
                if(adapterType == ContentWrapper.class) {
                    AbstractEditor editor = new AbstractEditor(activeEditor);
                    ContentWrapper content = new ContentWrapper(activeEditor.getTitle(),
                            ContentWrapper.UNKNOWN_CONTENT_TYPE, editor);
                    content.setModifiable(true);
                    return content;
                }
            }
        }
        if(adaptableObject instanceof IClassFileEditorInput) {
            IEditorPart activeEditor = EclipseUtils.getActiveEditor();
            if(activeEditor instanceof ITextEditor) {
                if(adapterType == ContentWrapper.class) {
                    AbstractEditor editor = new AbstractEditor(activeEditor);
                    ContentWrapper content = new ContentWrapper(activeEditor.getTitle(), "java",
                            editor);
                    content.setModifiable(false);
                    return content;
                }
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] {File.class, ContentWrapper.class};
    }

}
