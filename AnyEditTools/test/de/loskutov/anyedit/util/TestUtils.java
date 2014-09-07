package de.loskutov.anyedit.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;
import de.loskutov.anyedit.util.TextUtil.LineAndCaret;

public class TestUtils extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tu = TextUtil.getDefaultTextUtilities();
        tu.setCharsDisallowedInPath(TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);
        isWindows = EclipseUtils.class.getDeclaredField("isWindows");
        isWindows.setAccessible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        isWindows.set(null, Boolean.FALSE);
        super.tearDown();
    }

    public void testFindPath() throws Exception {
        String firstPart = "e:\\temp\\B.txt";
        String text = firstPart +
                ":10:test  extern VOID fw_ch\nB.txt:10:test  extern VOID fw_ch";

        isWindows.set(null, Boolean.TRUE);

        String path = null;
        // first char after colon
        int firstCursorIdx = 2;

        /*
         * Windoof
         */
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            tu.setCharsDisallowedInPath(TextUtil.WINDOOF_DEF);
            path = tu.findPath(new LineAndCaret(text, i));
            assertEquals(firstPart, path);
        }

        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            tu.setCharsDisallowedInPath(":" + TextUtil.WINDOOF_DEF);
            path = tu.findPath(new LineAndCaret(text, i));
            assertEquals("temp\\B.txt", path);
        }

        /*
         * Linux
         */
        isWindows.set(null, Boolean.FALSE);
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            tu.setCharsDisallowedInPath(TextUtil.LINUX_DEF);
            path = tu.findPath(new LineAndCaret(text, i));
            assertEquals("\\temp\\B.txt", path);
        }

        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            tu.setCharsDisallowedInPath(TextUtil.LINUX_DEF.replace(":", ""));
            path = tu.findPath(new LineAndCaret(text, i));
            assertEquals("e:\\temp\\B.txt:10:test", path);
        }

    }

    public void testFindLineReference() {
        String firstPart = "e:\\temp\\B.txt";
        String text = firstPart +
                ":10:test  extern VOID fw_ch";
        // first char after colon
        int firstCursorIdx = 2;
        String currText;
        for (int i = firstCursorIdx; i < firstPart.length(); i++) {
            currText = text.substring(i);
            for (int j = 0; i < firstPart.length(); i++) {
                int lineRef = tu.findLineReferenceRegex(currText, j);
                assertEquals("idx: " + i + "/" + j , 10, lineRef);
            }
        }
    }

    public void testConvertSpacesToTabs() {
        String line = "   a  b cd \t \t \t";
        StringBuffer sb = new StringBuffer(line);
        TextUtil.convertSpacesToTabs(sb , 4, false, false, true);
    }

    public void testCapitalize() {
        String string = TextUtil.capitalize("abcdefghijklmnopqrstuvwxyz0123456789,.;:-_��<>|@*+~#'`�?�\\");
        assertEquals(string, "Abcdefghijklmnopqrstuvwxyz0123456789,.;:-_��<>|@*+~#'`�?�\\");
    }

    static final String EXPECTED_PATH = "tmp/B.txt";
    static final List<String> lineExamples = Arrays.asList(
            "      :10:x tmp/B.txt:10:test \n extern VOID fw_ch",
            "       x()  tmp/B.txt: line 10",
            "     x{a}(b)tmp/B.txt(line 10) ",
            "      he{}()tmp/B.txt(10) ",
            "        {}()tmp/B.txt() ",
            "          {(tmp/B.txt( ",
            "          })tmp/B.txt)} ",
            "aaaaaaaaaaa/tmp/B.txt",
            "            tmp/B.txt   ",
            "        EMP/tmp/B.txt   ",
            "         $T/tmp/B.txt $X/bla $H/blup $H/$T/blup ",
            "       ${T}/tmp/B.txt $(X)/bla ${H}/blup $(H)/${T}/$X/blup ",
            "       $(T)/tmp/B.txt $(X)/bla ${H}/blup $(H)/${T}/$X/blup ",
            "$(H)/$T/$XX/tmp/B.txt ${T}/B.txt $(X)/bla ${H}/blup $(H)/${T}/$X/blup ",
            "    :10:x ~/tmp/B.txt:10:test \n extern VOID fw_ch",
            "     x()  ~/tmp/B.txt: line 10",
            "          ~/tmp/B.txt   "
            //               |  <  caret position: "B"
            );
    static final int CARET_POS = lineExamples.get(0).indexOf('B');
    static final int CARET_POS_START = CARET_POS - 4;
    static final int CARET_POS_END = CARET_POS + 4;
    private TextUtil tu;
    private Field isWindows;

    public void testFindPathWithVars() throws Exception {
        Set<Entry<String, String>> env = System.getenv().entrySet();
        String var = "";
        String value = "";
        for (Entry<String, String> entry : env) {
            if(entry.getKey().length() > 1 && ("" +entry.getValue()).length() > 1){
                var = entry.getKey();
                value = entry.getValue();
                break;
            }
        }
        assertTrue(var.length() > 1);
        assertTrue(value.length() > 1);

        String firstPart = EXPECTED_PATH;

        int expectedCount = firstPart.length() * lineExamples.size();
        int count = 0;
        for (String line : lineExamples) {
            /*
             * Linux
             */
            count = checkPathInLine(firstPart, count, line);
        }
        assertEquals(expectedCount, count);

    }

    private int checkPathInLine(String firstPart, int count, String line) {
        for (int i = CARET_POS_START; i < CARET_POS_START + firstPart.length(); i++) {
            tu.setCharsDisallowedInPath(TextUtil.DEFAULT_CHARACTERS_DISALLOWED_IN_PATH);
            String path = tu.findPath(new LineAndCaret(line, i));
            assertNotNull("Failed with: " + line + ", index: " + i, path);
            if(!EXPECTED_PATH.equals(path)){
                if(line.contains("~")){
                    String home = System.getProperty("user.home") + "/";
                    assertEquals(home + EXPECTED_PATH, path);
                }
                path = path.replace(EXPECTED_PATH, "");
                assertEquals('/', path.charAt(path.length() - 1));
                assertEquals(-1, path.indexOf('$'));
                assertEquals(-1, path.indexOf('('));
                assertEquals(-1, path.indexOf(')'));
                assertEquals(-1, path.indexOf('{'));
                assertEquals(-1, path.indexOf('}'));
            } else {
                assertEquals(EXPECTED_PATH, path);
            }
            count ++;
        }
        return count;
    }

}
