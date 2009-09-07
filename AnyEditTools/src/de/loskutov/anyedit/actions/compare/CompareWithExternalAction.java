/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.FileDialog;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.compare.ExternalFileStreamContent;
import de.loskutov.anyedit.compare.FileStreamContent;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 */
public class CompareWithExternalAction extends CompareWithAction {

    public CompareWithExternalAction() {
        super();
    }

    protected StreamContent createRightContent(StreamContent left) throws CoreException {
        FileDialog dialog = new FileDialog(AnyEditToolsPlugin.getShell());

        preSelectPath(dialog);
        String result = dialog.open();
        if(result == null) {
            return null;
        }
        rememberPath(result);

        IFile file = EclipseUtils.getIFile(new Path(result));
        if (file == null) {
            ContentWrapper content = ContentWrapper.create(new File(result));
            if(content == null){
                return null;
            }
            return new ExternalFileStreamContent(content);
        }
        if(file.getLocation() == null) {
            ContentWrapper content = ContentWrapper.create(file.getFullPath().toFile());
            if(content == null){
                return null;
            }
            return new ExternalFileStreamContent(content);
        }
        ContentWrapper content = ContentWrapper.create(file);
        if(content == null){
            return null;
        }
        return new FileStreamContent(content);
    }

    public static void rememberPath(String path) {
        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        store.setValue(IAnyEditConstants.LAST_OPENED_EXTERNAL_FILE, path);
    }

    public static void preSelectPath(FileDialog dialog) {
        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        String lastUsedFile = store.getString(IAnyEditConstants.LAST_OPENED_EXTERNAL_FILE);
        if(lastUsedFile == null) {
            return;
        }
        IPath path = new Path(lastUsedFile);
        if(path.segmentCount() < 2) {
            if(path.segmentCount() == 1) {
                dialog.setFilterPath(path.toOSString());
            }
            return;
        }
        dialog.setFileName(path.lastSegment());
        dialog.setFilterPath(path.removeLastSegments(1).toOSString());
    }

}
