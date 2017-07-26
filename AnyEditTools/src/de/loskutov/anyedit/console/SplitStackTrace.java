/*******************************************************************************
 * Copyright (c) 2017 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.console;

class SplitStackTrace {

    private final String string;


    SplitStackTrace(String string) {
        this.string = string;
    }


    String splitUp() {
        if (string != null) {
            String temp = string;
            temp = formatEclipseConsoleStackTrace(temp);
            temp = formatStackTraceToString(temp);
            return temp;
        }
        return null;
    }


    private static String formatEclipseConsoleStackTrace(String temp) {
        temp = temp.replaceAll( "\\.[\\t ]+\\n?",     ".\n"            );
        temp = temp.replaceAll( "\\s+at[\\t ]+",      "\n at "         );
        temp = temp.replaceAll( "\\s*Caused by:\\s*", "\nCaused by:\n" );
        return temp;
    }

    private static String formatStackTraceToString(String temp) {
        temp = temp.replaceAll( "[\\[|\\]]",          ""               );
        temp = temp.replaceAll( "\\s*,[\\t ]*",       ",\n"            );
        return temp;
    }

}
