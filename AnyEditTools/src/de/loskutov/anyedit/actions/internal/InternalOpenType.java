/*******************************************************************************
 * Copyright (c) 2005 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.OpenTypeAction;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * @author Andrei
 */
public class InternalOpenType extends OpenTypeAction {

    public void run(String filter) {
        if (filter == null || (filter = filter.trim()).length() == 0) {
            super.run();
            return;
        }
        filter = "" + filter + "";
        Shell parent = AnyEditToolsPlugin.getShell();
        // begin fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=66436
        OpenTypeSelectionDialog dialog;
        try {
            dialog = new OpenTypeSelectionDialog(parent, false, PlatformUI
                    .getWorkbench().getProgressService(), null,
                    IJavaSearchConstants.TYPE);
            dialog.setInitialPattern(filter);
        } catch (OperationCanceledException e) {
            // action got canceled
            return;
        }
        // end fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=66436

        dialog.setTitle(JavaUIMessages.OpenTypeAction_dialogTitle);
        dialog.setMessage(JavaUIMessages.OpenTypeAction_dialogMessage);
        int result = dialog.open();
        if (result != IDialogConstants.OK_ID) {
            return;
        }

        Object[] types = dialog.getResult();
        if (types != null && types.length > 0) {
            IType type = (IType) types[0];
            try {
                IEditorPart part = EditorUtility.openInEditor(type, true);
                EditorUtility.revealInEditor(part, type);
            } catch (CoreException x) {
                AnyEditToolsPlugin.logError("'Open type' operation failed", x); //$NON-NLS-1$
            }
        }
    }
}
