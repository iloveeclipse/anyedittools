/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.replace;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 *
 */
public class ReplaceWithResourceAction extends ReplaceWithAction {

    public ReplaceWithResourceAction() {
        super();
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.replace.ReplaceWithAction#createInputStream()
     */
    protected InputStream createInputStream() {
        IFile file = EclipseUtils.getWorkspaceFile();
        if (file == null) {
            return null;
        }
        try {
            return file.getContents();
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError("Can't get file content: " + file, e);
        }
        return null;
    }

}
