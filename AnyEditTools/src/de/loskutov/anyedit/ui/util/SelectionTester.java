/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.util;

import java.io.File;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.util.EclipseUtils;


public class SelectionTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        ISelection selection =  (ISelection) receiver;
        if ("isComparable".equals(property)){
            if (selection.isEmpty() || !(selection instanceof StructuredSelection)) {
                return false;
            }
            File file = EclipseUtils.getFile(selection, true);
            if(file != null){
                return true;
            }
            ContentWrapper wrapper = EclipseUtils.getAdapter(receiver, ContentWrapper.class);
            if(wrapper != null) {
                return true;
            }
        }
        return false;
    }

}
