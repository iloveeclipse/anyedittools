/*******************************************************************************
 * Copyright (c) 2012 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class WorkingSetImportWizard extends Wizard implements IImportWizard {

    private ImportPage mainPage;
    private IStructuredSelection selection;

    public WorkingSetImportWizard() {
        super();
    }

    @Override
    public boolean performFinish() {
        return mainPage != null? mainPage.finish() : false;
    }

    public void init(IWorkbench workbench, IStructuredSelection sel) {
        this.selection = sel;
    }

    @Override
    public void addPages() {
        super.addPages();
        mainPage = new ImportPage("Working Set Import");
        mainPage.setInitialSelection(selection);
        addPage(mainPage);
    }
}
