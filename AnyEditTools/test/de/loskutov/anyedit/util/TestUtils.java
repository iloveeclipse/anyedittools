package de.loskutov.anyedit.util;

import java.lang.reflect.Field;

import junit.framework.TestCase;

public class TestUtils extends TestCase {

    public void testFromCamelToUnderscore() {
        fail("Not yet implemented");
    }

    public void testIsPath() {
        fail("Not yet implemented");
    }

    public void testIsFilePath() {
        fail("Not yet implemented");
    }

    public void testIsJavaType() {
        fail("Not yet implemented");
    }

    public void testTrimPath() {
        fail("Not yet implemented");
    }

    public void testFindPath() throws Exception {
        String firstPart = "e:\\temp\\B.txt";
        String text = firstPart +
                ":10:test  extern VOID fw_ch\nB.txt:10:test  extern VOID fw_ch";
        TextUtil tu = TextUtil.getDefaultTextUtilities();

        tu.setCharsDisallowedInPath(TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);

        Field isWindows = EclipseUtils.class.getDeclaredField("isWindows");
        isWindows.setAccessible(true);

        isWindows.set(null, Boolean.TRUE);

        String path = null;
        // first char after colon
        int firstCursorIdx = 2;

        /*
         * Windoof
         */
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            path = tu.findPath(text, i);
            assertEquals(firstPart, path);
        }

        tu.setCharsDisallowedInPath(":" + TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            path = tu.findPath(text, i);
            assertEquals("temp\\B.txt", path);
        }

        /*
         * Linux
         */
        isWindows.set(null, Boolean.FALSE);
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            path = tu.findPath(text, i);
            assertEquals("\\temp\\B.txt", path);
        }

        tu.setCharsDisallowedInPath(TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            path = tu.findPath(text, i);
            assertEquals("e:\\temp\\B.txt:10:test", path);
        }

    }

    public void testTrimJavaType() {
        fail("Not yet implemented");
    }

    public void testFindLineReference() {
        String firstPart = "e:\\temp\\B.txt";
        String text = firstPart +
                ":10:test  extern VOID fw_ch";
        // first char after colon
        int firstCursorIdx = 2;
        String currText;
        TextUtil tu = TextUtil.getDefaultTextUtilities();
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            currText = text.substring(i);
            for (int j = 0; i < firstPart.length(); i++) {
                int lineRef = tu.findLineReferenceRegex(currText, j);
                assertEquals("idx: " + i + "/" + j , 10, lineRef);
            }
        }
    }

    public void testFindJavaType() {
        fail("Not yet implemented");
    }

    public void testIndexOf() {
        fail("Not yet implemented");
    }

    public void testGetCharsDisallowedInPath() {
        fail("Not yet implemented");
    }

    public void testGetCharsRequiredInPath() {
        fail("Not yet implemented");
    }

    public void testIsUseRequiredInPathChars() {
        fail("Not yet implemented");
    }

    public void testSetCharsDisallowedInPath() {
        fail("Not yet implemented");
    }

    public void testSetCharsRequiredInPath() {
        fail("Not yet implemented");
    }

    public void testSetUseRequiredInPathChars() {
        fail("Not yet implemented");
    }

    public void testConvertTabsToSpaces() {
        fail("Not yet implemented");
    }

    public void testRemoveTrailingSpace() {
        fail("Not yet implemented");
    }

    public void testConvertSpacesToTabs() {
        String line = "   a  b cd \t \t \t";
        StringBuffer sb = new StringBuffer(line);
        String line2 = "   a  b cd \t \t \t";
        TextUtil.convertSpacesToTabs(sb , 4, false, true);
    }

    public void testCount() {
        fail("Not yet implemented");
    }

    public void testGetEntityForChar() {
        fail("Not yet implemented");
    }

    public void testEscapeText() {
        fail("Not yet implemented");
    }

    public void testUnescapeText() {
        fail("Not yet implemented");
    }

    public void testCapitalize() {
        String string = TextUtil.capitalize("abcdefghijklmnopqrstuvwxyz0123456789,.;:-_צה<>|@*+~#'`´?ß\\");
        assertEquals(string, "Abcdefghijklmnopqrstuvwxyz0123456789,.;:-_צה<>|@*+~#'`´?ß\\");
    }

    public void testInvertCase() {
        fail("Not yet implemented");
    }

}
