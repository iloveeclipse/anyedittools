/*******************************************************************************
 * Copyright (c) 2007 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class WorkingSetImportWizard extends Wizard implements IImportWizard {

	public WorkingSetImportWizard() {
		super();
	}
    private ImportPage mainPage;


    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    public boolean performFinish() {
        return mainPage.finish();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    	// noop
    }

    public void addPages() {
        super.addPages();
        mainPage = new ImportPage("Working Set Import");
        addPage(mainPage);
    }
}
