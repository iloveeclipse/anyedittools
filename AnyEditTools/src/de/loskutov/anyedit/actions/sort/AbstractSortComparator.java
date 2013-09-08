/*******************************************************************************
 * Copyright (c) 2011 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Clemens Fuchslocher - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.sort;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.loskutov.anyedit.util.LineReplaceResult;

/**
 * @author Clemens Fuchslocher
 */
public abstract class AbstractSortComparator<V extends LineReplaceResult> implements Comparator<V> {

    private final Pattern pattern = Pattern.compile("^\\s*(-?\\d+)");

    protected int compareLineLength(LineReplaceResult left, LineReplaceResult right) {
        int lLength = line(left).length();
        int rLength = line(right).length();
        if (rLength > lLength) {
            return 1;
        } else if(rLength < lLength) {
            return  -1;
        }
        return 0;
    }

    protected int compareLineCaseInsensitive(LineReplaceResult left, LineReplaceResult right) {
        int result = line(left).compareToIgnoreCase(line(right));
        if (result == 0) {
            result = line(left).compareTo(line(right));
        }
        return result;
    }

    protected String line(LineReplaceResult object) {
        String line = object.textToReplace;
        if (line != null) {
            return line;
        }
        return "";
    }

    protected int compareNumber(LineReplaceResult left, LineReplaceResult right) {
        BigInteger l = number(left);
        BigInteger r = number(right);
        if (l == null || r == null) {
            return 0;
        }
        return l.compareTo(r);
    }

    private BigInteger number(LineReplaceResult object) {
        Matcher matcher = pattern.matcher(line(object));
        if (matcher.find()) {
            return new BigInteger(matcher.group(1));
        }
        return null;
    }

}
