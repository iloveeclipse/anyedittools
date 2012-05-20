/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Andrey
 *
 */
public interface StreamContent extends ITypedElement, IAdaptable,
IStructureComparator {

    public boolean isDirty();

    public void setDirty(boolean dirty);

    public boolean commitChanges(IProgressMonitor pm) throws CoreException;

    public void dispose();

    public void init(AnyeditCompareInput input);

    public StreamContent recreate();

    boolean isDisposed();

    String getFullName();
}
