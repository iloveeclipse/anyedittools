/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Clemens Fuchslocher - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.sort;

import java.util.Comparator;

/**
 * @author Clemens Fuchslocher
 */
public class SortAlphabeticallyCaseInsensitiveAscending extends AbstractSortAction {

    @Override
    protected Comparator getComparator() {
        return new AbstractSortComparator() {
            public int compare(Object left, Object right) {
                return compareLineCaseInsensitive(left, right);
            }
        };
    }

}
