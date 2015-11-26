/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.actions;

import de.loskutov.anyedit.util.TextUtil;

/**
 * Initiated by Ray Vanderborght.
 * @author Andrey
 */
public class ChangeCase extends AbstractReplaceAction {

    private static final int KEY_TO_LOWER = 0;
    private static final int KEY_TO_UPPER = 1;
    private static final int KEY_INVERT_CASE = 2;
    private static final int KEY_CAPITALIZE = 3;
    private static final int KEY_CAMEL = 4;
    private static final int KEY_CAMEL_TO_PASCAL = 5;

    @Override
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
        case KEY_CAMEL_TO_PASCAL :{
            boolean b = line.matches("[A-Z]+.*");
            String s = null;
            if (b) {
                s = TextUtil.fromCamelCaseToPascalCaseBidirectional(line, true);
            } else {
                s = TextUtil.fromCamelCaseToPascalCaseBidirectional(line, false);
            }
            return s;
        }
        }
    }

    @Override
    protected int getActionKey(String actionID) {
        if(actionID.startsWith(ACTION_ID_TO_LOWER)){
            return KEY_TO_LOWER;
        } else if(actionID.startsWith(ACTION_ID_TO_UPPER)){
            return KEY_TO_UPPER;
        } else if(actionID.startsWith(ACTION_ID_CAPITALIZE)){
            return KEY_CAPITALIZE;
        } else if (actionID.startsWith(ACTION_ID_CAMEL_TO_PASCAL)) {
            return KEY_CAMEL_TO_PASCAL;
        } else if(actionID.startsWith(ACTION_ID_CAMEL)){
            return KEY_CAMEL;
        }
        return KEY_INVERT_CASE;
    }
}
