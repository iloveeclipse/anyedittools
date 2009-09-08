/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Stefan Seidel - initial API and implementation
 * Contributor:  Andrei Loskutov - integration
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.util.TextReplaceResultSet;

public class ConvertUnicode extends AbstractReplaceAction {

    private static final int KEY_UNESCAPE = 0;

    private static final int KEY_ESCAPE = 1;

    private boolean warnAboutUnsupportedUnicode;
    private String charset;

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#doTextOperation(org.eclipse.jface.text.IDocument, java.lang.String, de.loskutov.anyedit.util.TextReplaceResultSet)
     */
    protected void doTextOperation(IDocument doc, String actionID,
            TextReplaceResultSet resultSet) throws BadLocationException {
        warnAboutUnsupportedUnicode = warnAboutUnsupportedUnicode();
        charset = getEditor().computeEncoding();
        super.doTextOperation(doc, actionID, resultSet);
    }

    /**
     * @return
     */
    private boolean warnAboutUnsupportedUnicode() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
                IAnyEditConstants.WARN_ABOUT_UNSUPPORTED_UNICODE);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#performReplace(java.lang.String,
     *      int)
     */
    protected String performReplace(String line, int actionKey) {
        if (KEY_ESCAPE == actionKey) {
            return textUtil.toUnicode(line);
        }
        String charsetToUse = warnAboutUnsupportedUnicode? charset : null;
        try {
            return textUtil.fromUnicode(charsetToUse, line);
        } catch (UnsupportedOperationException e) {
            MessageDialogWithToggle dialogWithToggle = MessageDialogWithToggle
                    .openYesNoQuestion(
                            AnyEditToolsPlugin.getShell(),
                            Messages.ConvertUnicode_title,
                            NLS.bind(Messages.ConvertUnicode_warn, e.getMessage()),
                            Messages.ConvertUnicode_toggleMessage,
                            false, AnyEditToolsPlugin.getDefault()
                            .getPreferenceStore(),
                            IAnyEditConstants.WARN_ABOUT_UNSUPPORTED_UNICODE);

            int returnCode = dialogWithToggle.getReturnCode();
            // refresh the value, if user changed it
            warnAboutUnsupportedUnicode = warnAboutUnsupportedUnicode();
            if (returnCode != IDialogConstants.YES_ID) {
                shouldStopReplace = true;
                return null;
            }
            // supress any warning, just continue
            return textUtil.fromUnicode(null, line);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#getActionKey(java.lang.String)
     */
    protected int getActionKey(String actionID) {
        return actionID.startsWith(ACTION_ID_UNICODIFY) ? KEY_ESCAPE : KEY_UNESCAPE;
    }
}
