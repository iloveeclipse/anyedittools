/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Andrei
 *
 */
public interface StreamContent extends ITypedElement,
        IStructureComparator {

    public boolean isDirty();

    public boolean commitChanges(IProgressMonitor pm) throws CoreException;

    public void dispose();

    public void init(AnyeditCompareInput input);

    public StreamContent recreate();

    boolean isDisposed();

    String getFullName();
}
