/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;

public interface IDirtyWorkaround extends IAction {
    void copyStateAndDispose(IContributionItem oldItem);
    /**
     * Performs the 'convert spaces' action before the editor buffer is saved
     */
    void runBeforeSave();

    IWorkbenchWindow getWindow();
}
