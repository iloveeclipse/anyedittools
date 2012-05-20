/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import java.util.List;

import org.eclipse.ui.IWorkingSet;

/**
 * @author Andrei
 *
 */
public interface IWSAction {
    void setWorkingSets(List/* <IWorkingSet> */<IWorkingSet> workingSets);
    void run();
}
