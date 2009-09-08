/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.ui.editor.EditorPropertyTester;

public class SaveToFileParticipant extends SaveToFileAction implements
IConsolePageParticipant {

    private IPageBookViewPage page;

    public SaveToFileParticipant() {
        super();
    }

    public void activated() {
        // no op
    }

    public void deactivated() {
        // no op
    }

    public void init(IPageBookViewPage myPage, IConsole console) {
        page = myPage;
        IToolBarManager toolBarManager = page.getSite().getActionBars()
        .getToolBarManager();
        toolBarManager.appendToGroup(IConsoleConstants.OUTPUT_GROUP, new Separator());
        toolBarManager.appendToGroup(IConsoleConstants.OUTPUT_GROUP, new Action(
                "Save to file", getImageDescriptor()) {
            public void run() {
                SaveToFileParticipant.this.run(this);
            }
        });
    }

    private ImageDescriptor getImageDescriptor() {
        ImageDescriptor descriptor = AnyEditToolsPlugin.getDefault().getImageRegistry()
        .getDescriptor("icons/saveToFile.gif");
        if (descriptor == null) {
            descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(AnyEditToolsPlugin
                    .getId(), "icons/saveToFile.gif");
            if (descriptor != null) {
                AnyEditToolsPlugin.getDefault().getImageRegistry().put(
                        "icons/saveToFile.gif", descriptor);
            }
        }
        return descriptor;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public void dispose() {
        page = null;
        super.dispose();
    }

    public void run(IAction action) {
        if (page != null) {
            runWithViewer(EditorPropertyTester.getViewer(page));
        }
    }

}
