/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.console;

import junit.framework.TestCase;

/**
 * @author sandreev
 *
 */
public class TestSplitStackTrace extends TestCase {


    public void testEclipseFormattedStackTrace() {
        String stackTrace = join(
                "Exception in thread \"main\" java.io.IOException: nested exception",
                "    at some.path.SomeClass.wrappingMethod(SomeClass.java:31)",
                "    at some.path.SomeClass.main(SomeClass.java:42)",
                "Caused by: java.lang.NullPointerException: original cause",
                "    at some.path.SomeClass.someMethod(SomeClass.java:36)",
                "    at some.path.SomeClass.wrappingMethod(SomeClass.java:29)");
        String actualSplit = split(stackTrace);
        String expectedSplit = join(
                "Exception in thread \"main\" java.io.IOException: nested exception",
                " at some.path.SomeClass.wrappingMethod(SomeClass.java:31)",
                " at some.path.SomeClass.main(SomeClass.java:42)",
                "Caused by:",
                "java.lang.NullPointerException: original cause",
                " at some.path.SomeClass.someMethod(SomeClass.java:36)",
                " at some.path.SomeClass.wrappingMethod(SomeClass.java:29)");

        assertEquals("stack trace not splitted as expected",
                expectedSplit, actualSplit);
    }

    public void testStackTraceToString() {
        String stackTrace = "[some.path.SomeClass.wrappingMethod(SomeClass.java:31), some.path.SomeClass.main(SomeClass.java:42)]";
        String actualSplit = split(stackTrace);
        String expectedSplit = join(
                "some.path.SomeClass.wrappingMethod(SomeClass.java:31),\n" +
                "some.path.SomeClass.main(SomeClass.java:42)");

        assertEquals("stack trace not splitted as expected",
                expectedSplit, actualSplit);
    }

    private static String split(String stackTrace) {
        return new SplitStackTrace(stackTrace).splitUp();
    }

    private static String join(String... lines) {
        return String.join(System.lineSeparator(), lines);
    }
}
