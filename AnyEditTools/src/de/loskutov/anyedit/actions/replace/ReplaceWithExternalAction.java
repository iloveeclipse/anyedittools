/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.replace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.FileDialog;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.actions.compare.CompareWithExternalAction;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 *
 */
public class ReplaceWithExternalAction extends ReplaceWithAction {

    public ReplaceWithExternalAction() {
        super();
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.replace.ReplaceWithAction#createInputStream()
     */
    protected InputStream createInputStream() {
        FileDialog dialog = new FileDialog(AnyEditToolsPlugin.getShell());

        CompareWithExternalAction.preSelectPath(dialog);
        String result = dialog.open();
        if(result == null) {
            return null;
        }
        CompareWithExternalAction.rememberPath(result);

        IFile file = EclipseUtils.getIFile(new Path(result));
        if (file == null) {
            try {
                return new FileInputStream(new File(result));
            } catch (FileNotFoundException e) {
                AnyEditToolsPlugin.logError("File not found: " + result, e);
                return null;
            }
        }
        if(file.getLocation() == null) {
            try {
                return new FileInputStream(file.getFullPath().toFile());
            } catch (FileNotFoundException e) {
                AnyEditToolsPlugin.logError("File not found: " + file, e);
                return null;
            }
        }
        try {
            return file.getContents();
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError("Can't get file content: " + file, e);
            return null;
        }
    }

}
