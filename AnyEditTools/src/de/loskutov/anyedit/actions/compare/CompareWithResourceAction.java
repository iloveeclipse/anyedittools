/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrei
 *
 */
public class CompareWithResourceAction extends CompareWithAction {

    public CompareWithResourceAction() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.actions.CompareWithAction#createRightContent()
     */
    protected StreamContent createRightContent(StreamContent left) throws CoreException {
        IFile file = EclipseUtils.getWorkspaceFile();
        if (file == null) {
            return null;
        }
        return createContentFromFile(ContentWrapper.create(file));
    }

}
