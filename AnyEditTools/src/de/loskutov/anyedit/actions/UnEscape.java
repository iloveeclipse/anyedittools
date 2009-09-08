/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.util.TextReplaceResultSet;
import de.loskutov.anyedit.util.TextUtil;

public class UnEscape extends AbstractReplaceAction {

    private static final int KEY_UNESCAPE = 0;
    private static final int KEY_ESCAPE = 1;
    private boolean preserveEntities;

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#doTextOperation(org.eclipse.jface.text.IDocument, java.lang.String, de.loskutov.anyedit.util.TextReplaceResultSet)
     */
    protected void doTextOperation(IDocument doc, String actionID,
            TextReplaceResultSet resultSet) throws BadLocationException {
        preserveEntities = isPreserveEntitiesEnabled();
        super.doTextOperation(doc, actionID, resultSet);
    }

    protected static boolean isPreserveEntitiesEnabled() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
            IAnyEditConstants.PRESERVE_ENTITIES);
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#performReplace(java.lang.String, int)
     */
    protected String performReplace(String line, int actionKey) {
        if(KEY_UNESCAPE == actionKey){
            return TextUtil.unescapeText(line);
        }
        if(preserveEntities){
            // escape(unescape(line)) instead of escape(line)
            // prevent converting from parts
            // of already converted entities
            return TextUtil.escapeText(TextUtil.unescapeText(line));
        }
        return TextUtil.escapeText(line);
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#getActionKey(java.lang.String)
     */
    protected int getActionKey(String actionID) {
        return actionID.startsWith(ACTION_ID_UNESCAPE)? KEY_UNESCAPE : KEY_ESCAPE;
    }
}
