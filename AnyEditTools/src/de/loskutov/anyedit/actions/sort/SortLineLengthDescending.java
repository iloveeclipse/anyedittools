/*******************************************************************************
 * Copyright (c) 2011 Duncan Drysdale.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Clemens Fuchslocher - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.sort;

import java.util.Comparator;

import de.loskutov.anyedit.util.LineReplaceResult;

/**
 * @author Duncan Drysdale
 */
public class SortLineLengthDescending extends AbstractSortAction {

    @Override
    protected Comparator<LineReplaceResult> getComparator() {
        return new AbstractSortComparator<LineReplaceResult>() {
            @Override
            public int compare(LineReplaceResult left, LineReplaceResult right) {
                return -compareLineLength(left, right);
            }
        };
    }

}
