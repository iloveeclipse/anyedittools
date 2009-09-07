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
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author Andrei
 */
public class WorkingSetExportWizard extends Wizard implements IExportWizard {

    private ExportPage mainPage;

    public WorkingSetExportWizard() {
        super();
    }

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
        mainPage = new ExportPage("Working Set Export");
        addPage(mainPage);
        setWindowTitle(mainPage.getName());
    }
}
