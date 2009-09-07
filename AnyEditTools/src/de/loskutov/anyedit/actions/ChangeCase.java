/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import de.loskutov.anyedit.util.TextUtil;

/**
 * Initiated by Ray Vanderborght.
 * @author Andrei
 */
public class ChangeCase extends AbstractReplaceAction {

    private static final int KEY_TO_LOWER = 0;
    private static final int KEY_TO_UPPER = 1;
    private static final int KEY_INVERT_CASE = 2;
    private static final int KEY_CAPITALIZE = 3;
    private static final int KEY_CAMEL = 4;

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#performReplace(java.lang.String, int)
     */
    protected String performReplace(String line, int actionKey) {

        switch (actionKey) {
            case KEY_TO_LOWER :{
                return line.toLowerCase();
            }
            case KEY_TO_UPPER :{
                return line.toUpperCase();
            }
            case KEY_CAPITALIZE :{
                return TextUtil.capitalize(line);
            }
            case KEY_CAMEL :{
                if(line.indexOf('_') < 0) {
                    return TextUtil.fromCamelToUnderscore(line);
                }
                return TextUtil.fromUnderscoreToCamel(line);
            }

            default :
                // fall througth

            case KEY_INVERT_CASE :{
                return TextUtil.invertCase(line);
            }
        }
    }

    /* (non-Javadoc)
     * @see de.loskutov.anyedit.actions.AbstractReplaceAction#getActionKey(java.lang.String)
     */
    protected int getActionKey(String actionID) {
        if(actionID.startsWith(ACTION_ID_TO_LOWER)){
            return KEY_TO_LOWER;
        } else if(actionID.startsWith(ACTION_ID_TO_UPPER)){
            return KEY_TO_UPPER;
        } else if(actionID.startsWith(ACTION_ID_CAPITALIZE)){
            return KEY_CAPITALIZE;
        } else if(actionID.startsWith(ACTION_ID_CAMEL)){
            return KEY_CAMEL;
        }
        return KEY_INVERT_CASE;
    }
}
