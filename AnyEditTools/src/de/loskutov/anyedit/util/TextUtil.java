/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.util;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.preference.IPreferenceStore;

import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;


/**
 * @author Andrei
 */
public class TextUtil {

    public static final String SYSTEM_CHARSET = System.getProperty("file.encoding", "utf-8");

    /** The predefined line delimiters */
    private static final char[] CR = { '\r' };

    private static final char[] LF = { '\n' };

    private static final char[] CRLF = { '\r', '\n' };

    private static final char[] EMPTY = {};

    public static final String DEFAULT_CHARACTERS_REQUIRED_IN_PATH = ".";

    public static final String DEFAULT_CHARACTERS_DISALLOWED_IN_PATH = EclipseUtils.isWindows()?
        " \n\"'*?><|=(){};&$,%@" : " \n\"'*?><|=(){};&$,%@:"; // ':' is invalid in some cases too

    public static final String DEFAULT_LINE_SEPARATOR_REGEX = ":|\\s+";

    private static final String INVALID_PATH_ENDS_CHARACTERS = "/\\";

    public static final boolean DEFAULT_UNICODIFY_ALL = false;

    public static final int DEFAULT_BASE64_LINE_LENGTH = 100;

    private static TextUtil instance;

    public boolean useRequiredInPathChars;

    private String charsDisallowedInPath;

    private String charsRequiredInPath;

    private String lineSeparatorRegex;

    private int base64LineLength;

    private boolean unicodifyAll;

    private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("(\\n|\\r| |\\\t)");
    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u[0-9a-fA-F]{2,4}");

    private TextUtil() {
        useRequiredInPathChars = true;
        charsDisallowedInPath = DEFAULT_CHARACTERS_DISALLOWED_IN_PATH;
        charsRequiredInPath = DEFAULT_CHARACTERS_REQUIRED_IN_PATH;
        base64LineLength = DEFAULT_BASE64_LINE_LENGTH;
        unicodifyAll = DEFAULT_UNICODIFY_ALL;
    }

    private static synchronized TextUtil getInstance() {
        if (instance == null) {
            instance = new TextUtil();
        }
        return instance;
    }

    /**
     * @param string
     *            in the "camel" notation like "beMyCamel"
     * @return the resulting string in usual notation like "be_my_camel"
     */
    public static String fromCamelToUnderscore(String string) {
        int size = string.length();
        StringBuffer sb = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            char c = string.charAt(i);
            if (i > 0 && i < size - 1) {
                char next = string.charAt(i + 1);
                char prev = string.charAt(i - 1);
                if (Character.isUpperCase(c) && Character.isJavaIdentifierPart(next)
                        && Character.isJavaIdentifierPart(prev)
                        && !Character.isUpperCase(next)) {
                    sb.append('_');
                    c = Character.toLowerCase(c);
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * @param string
     *            in the "underscore" notation like "be_my_camel"
     * @return the resulting string in "camel" notation like "beMyCamel"
     */
    public static String fromUnderscoreToCamel(String string) {
        int size = string.length();
        StringBuffer sb = new StringBuffer(size);
        boolean skipChar = false;
        boolean toUpper = false;
        for (int i = 0; i < size; i++) {
            char c = string.charAt(i);
            skipChar = i > 0 && c == '_';
            if (skipChar && i < size - 1
                    && !Character.isJavaIdentifierPart(string.charAt(i + 1))) {
                skipChar = false;
            } else if (i == size - 1) {
                skipChar = false;
            }
            if (!skipChar) {
                if (toUpper) {
                    sb.append(Character.toUpperCase(c));
                } else {
                    if (i > 0) {
                        if (Character.isJavaIdentifierPart(string.charAt(i - 1))) {
                            sb.append(Character.toLowerCase(c));
                        } else {
                            sb.append(c);
                        }
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }
            }
            toUpper = skipChar;
        }
        return sb.toString();
    }

    /** Check String to match real path name
     * @return false if this path is may be not a File/Dir path, i.e. contains
     * not alloved characters etc.
     */
    public boolean isPath(String path) {
        if (path == null) {
            return false;
        }
        path = path.trim();
        if (path.length() == 0 || path.length() > 400) {
            return false;
        }

        String disallowed = getCharsDisallowedInPath();
        for (int i = 0; i < disallowed.length(); i++) {
            if (path.indexOf(disallowed.charAt(i)) >= 0) {
                return false;
            }
        }
        if (isUseRequiredInPathChars()) {
            String required = getCharsRequiredInPath();
            for (int i = 0; i < required.length(); i++) {
                if (path.indexOf(required.charAt(i)) >= 0) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Check if given string can contain real <b>file</b> path name
     * @return false if this path is may be not a <b>file</b> path, i.e. contains
     * not alloved characters etc.
     */
    public boolean isFilePath(String path) {
        if (path == null || (path = path.trim()).length() == 0) {
            return false;
        }
        int lastIdx = path.length() - 1;
        for (int i = 0; i < INVALID_PATH_ENDS_CHARACTERS.length(); i++) {
            if (path.charAt(lastIdx) == INVALID_PATH_ENDS_CHARACTERS.charAt(i)) {
                return false;
            }
        }
        return isPath(path);
    }

    /**
     * Check if given string can contain real <b>Java type</b> name
     * @return false if this type is may be not a <b>Java type</b> name, i.e. contains
     * not alloved characters etc.
     */
    public boolean isJavaType(String type) {
        if (type == null || (type = type.trim()).length() == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(type.charAt(0))) {
            return false;
        }
        for (int i = 1; i < type.length(); i++) {
            if (!Character.isJavaIdentifierPart(type.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param path string to check
     * @return a copy of the string, with leading and trailing whitespace
     * and not in path allowed characters (leading and trailing) omitted.
     */
    public String trimPath(String path) {
        if (path == null) {
            return path; // shit in, shit out
        }
        path = path.trim();
        if (path.length() == 0) {
            return path; // shit in, shit out
        }

        if (EclipseUtils.isWindows()) {
            path = path.replace('/', '\\');
            // make "\test.txt" to "test.txt" but do not touch "\\share\text.txt"
            // "\test.txt" causes problems by selecting file in "open resource" dialog
            if (path.charAt(0) == '\\' && path.length() > 1 && path.charAt(1) != '\\') {
                path = path.substring(1);
            }
        }

        String disallowed = getCharsDisallowedInPath();
        /*
         * trim leading characters
         */
        for (int i = 0; i < disallowed.length(); i++) {
            if (path.charAt(0) == disallowed.charAt(i)) {
                path = path.substring(1);
                if (path.length() > 0) {
                    i = -1; // start search again with new first character
                } else {
                    break;
                }
            }
        }
        if (path.length() == 0) {
            return path; // shit in, shit out
        }
        /*
         * trim trailing characters
         */
        for (int i = 0; i < disallowed.length(); i++) {
            if (path.charAt(path.length() - 1) == disallowed.charAt(i)) {
                path = path.substring(0, path.length() - 1);
                if (path.length() > 0) {
                    i = -1; // start search again with new last character
                } else {
                    break;
                }
            }
        }
        int length = path.length();
        path = path.trim();
        if (length != path.length()) {
            // start again!!!
            return trimPath(path);
        }
        return path;
    }

    public String findPath(String line, int caretOffset) {
        if (line == null || line.length() == 0 || caretOffset >= line.length()
                || caretOffset < 0) {
            return null; // shit in, shit out
        }

        /**
         * we search for nearest to caret 'invalid' path characters in both directions
         */
        int backwardSearchIdx = -1;
        String disallowed = getCharsDisallowedInPath();
        for (int i = 0; i < disallowed.length(); i++) {
            char charAt = disallowed.charAt(i);
            int matchIdx = indexOf(line, charAt, caretOffset, backwardSearchIdx, false);
            // search nearest to caret, also biggest
            if (matchIdx > backwardSearchIdx) {
                backwardSearchIdx = matchIdx;
            }
        }

        int forwardSearchIdx = line.length();
        for (int i = 0; i < disallowed.length(); i++) {
            int matchIdx = indexOf(line, disallowed.charAt(i), caretOffset,
                    forwardSearchIdx, true);
            // search nearest to caret, also smaller
            if (matchIdx != -1 && matchIdx < forwardSearchIdx) {
                forwardSearchIdx = matchIdx;
            }
        }

        if (EclipseUtils.isWindows() && disallowed.indexOf(':') < 0) {
            int matchIdx = indexOf(line, ':', caretOffset, forwardSearchIdx, true);
            // search nearest to caret, also smaller
            if (matchIdx != -1 && matchIdx < forwardSearchIdx) {
                forwardSearchIdx = matchIdx;
            }

        }
        /**
         * now we have (or not) both ends of new line: check for identity with line and for
         * needed path characters (like '.') inside
         */
        if (forwardSearchIdx == line.length() && backwardSearchIdx == -1) {
            return trimPath(line);
        } else if (forwardSearchIdx > backwardSearchIdx) {
            line = line.substring(backwardSearchIdx + 1, forwardSearchIdx);
            if (isFilePath(line)) {
                return trimPath(line);
            }
        }
        return null;
    }

    public String trimJavaType(String type) {
        if (type == null || (type = type.trim()).length() == 0) {
            return type; // shit in, shit out
        }
        // trick: compute virtual "caret" in the middle of string
        int caretIdx = type.length() / 2;

        return findJavaType(type, caretIdx);
    }

    /**
     * Search for occurencies of line references in text, like
     * <pre>
     * foo/Foo.java RegexSeparator 156
     * </pre>
     * If the regular expression separator can't be compiled it just uses the separator as string.
	 * @param line
	 * @param startOffset
	 * @return integer value guessed as line reference in text (this is not a offset in given line!!!)
	 */
	public int findLineReferenceRegex(String line, int startOffset) {
		try {
        	Pattern p = Pattern.compile("(" + getLineSeparatorRegex() + ")(\\d+)" );
        	Matcher m = p.matcher(line);
        	if(m.find()){
        	    int groupCount = m.groupCount();
        	    if(groupCount == 0){
        	        return -1;
        	    }
        	    String group = m.group(groupCount);
        	    if(group == null){
        	        return -1;
        	    }
        	    try {
        	        return Integer.parseInt(group);
        	    } catch (Exception e) {
        	        // ignore, there was no line info?
        	        return -1;
        	    }
        	}

        } catch (PatternSyntaxException e) {
        	//since input values in preferences dialog are checked for valid patterns
        	//exception normally can't happen.
        	return findLineReference(line, startOffset);
        }
        return -1;
	}

    /**
     * Search for occurencies of line references in text, like
     * <pre>
     * foo/Foo.java:156
     * </pre>
     * @return integer value guessed as line reference in text (this is not a offset in given line!!!)
     */
    private int findLineReference(String line, int startOffset) {
        if (line == null || line.length() == 0 || startOffset >= line.length()
                || startOffset < 0) {
            return -1; // shit in, shit out
        }

        // search for first ':', if any
        int doppIndx = line.indexOf(getLineSeparatorRegex(), startOffset);

        // means > -1 and not the same occurence
        if (doppIndx > startOffset) {
            // try to find most common occurence: after first ':'
            int firstTry = findLineReference(line, doppIndx);
            // found? ok.
            if (firstTry >= 0) {
                return firstTry;
            }
            // else: we doesn't have line info after ':' or it is before!
        }

        int startChar = -1, stopChar = -1;
        boolean digit;
        for (int i = startOffset; i < line.length(); i++) {
            digit = Character.isDigit(line.charAt(i));
            if (digit) {
                if (startChar < 0) {
                    // let see on pevious character: is it letter, then
                    // followed digit cannot be line number, but is part of
                    // path or java name like 6 in Base64.java:125
                    if (i - 1 >= 0 && Character.isLetter(line.charAt(i - 1))) {
                        continue;
                    }
                    startChar = i;
                }
                stopChar = i + 1;
            } else if (startChar >= 0) {
                stopChar = i;
                break;
            }
        }
        if (startChar >= 0 && stopChar > 0) {
            line = line.substring(startChar, stopChar);
            int result = Integer.parseInt(line);
            return result;
        }
        return -1;
    }

    public String findJavaType(String line, int caretOffset) {
        if (line == null || line.length() == 0 || caretOffset >= line.length()
                || caretOffset < 0) {
            return null; // shit in, shit out
        }

        /**
         * we search for nearest to caret 'invalid' java characters in both directions
         */
        int forwardSearchIdx = caretOffset;
        for (int i = caretOffset; i < line.length(); i++) {
            if (Character.isJavaIdentifierPart(line.charAt(i))) {
                forwardSearchIdx++;
            } else {
                break;
            }
        }

        int backwardSearchIdx = caretOffset;
        for (int i = caretOffset; i >= 0; i--) {
            if (Character.isJavaIdentifierPart(line.charAt(i))) {
                backwardSearchIdx--;
            } else {
                break;
            }
        }
        if (backwardSearchIdx < 0) {
            backwardSearchIdx = 0;
        }

        // find first valid first java character
        for (int i = backwardSearchIdx; i < forwardSearchIdx; i++) {
            if (Character.isJavaIdentifierStart(line.charAt(i))) {
                backwardSearchIdx = i;
                break;
            }
        }

        /**
         * now we have (or not) both ends of new line: check for identity with line and for
         * needed path characters (like '.') inside
         */
        if (forwardSearchIdx == line.length() && backwardSearchIdx == 0) {
            return line;
        } else if (forwardSearchIdx > backwardSearchIdx) {
            return line.substring(backwardSearchIdx, forwardSearchIdx);
        }
        return null;
    }

    public static int indexOf(String line, char c, int startOffset, int stopOffset,
            boolean forward) {
        int i = startOffset;
        while (forward ? i < stopOffset : i > stopOffset) {
            if (line.charAt(i) == c) {
                return i;
            }
            if (forward) {
                i++;
            } else {
                i--;
            }
        }
        return -1;
    }

    public String getCharsDisallowedInPath() {
        return charsDisallowedInPath;
    }

    public String getCharsRequiredInPath() {
        return charsRequiredInPath;
    }

    public String getLineSeparatorRegex() {
        return lineSeparatorRegex;
    }

    public boolean isUseRequiredInPathChars() {
        return useRequiredInPathChars;
    }

    public void setCharsDisallowedInPath(String string) {
        charsDisallowedInPath = string;
    }

    public void setCharsRequiredInPath(String string) {
        charsRequiredInPath = string;
    }

    public void setLineSeparatorRegex(String string) {
        lineSeparatorRegex = string;
    }

    public void setUseRequiredInPathChars(boolean b) {
        useRequiredInPathChars = b;
    }

    public String base64decode(String base64) {
        Base64Preferences prefs = new Base64Preferences();
        prefs.put(null, base64);
        byte[] byteArray = prefs.getByteArray(null, null);
        if(byteArray == null) {
            // not base64 encoded => return input back
            return base64;
        }
        return new String(byteArray);
    }

    public String base64encode(String plainText) {
        Base64Preferences prefs = new Base64Preferences();
        prefs.putByteArray(null, plainText.getBytes());
        return prefs.get(null, null);
    }

    public String base64trim(String text, String lineDelim) {
        text = WHITE_SPACE_PATTERN.matcher(text).replaceAll("");
        StringBuffer resultText = new StringBuffer();
        for (int i = 0; i < text.length(); i += base64LineLength) {
            if ((i + base64LineLength) >= text.length()) {
                resultText.append(text.substring(i));
            } else {
                resultText.append(text.substring(i, i + base64LineLength));
            }
            resultText.append(lineDelim);
        }
        String string = resultText.toString();
        if (string.endsWith(lineDelim)) {
            string = string.substring(0, string.length() - lineDelim.length());
        }
        return string;
    }

    public String toUnicode(String input) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (unicodifyAll || (!Character.isWhitespace(ch) && ch < 0x20 || ch > 0x7e)) {
                ret.append("\\u");
                // requires 1.5 VM
                // ret.append(String.format("%1$04x", new Object[] { Integer.valueOf(ch) }));
                ret.append(leading4Zeros(Integer.toHexString(ch)));
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

    /**
     * @param hexString max 4 characters length
     * @return same string with leading zeros
     */
    private char[] leading4Zeros(String hexString) {
        char[] chars = "0000".toCharArray();
        int length = hexString.length();
        hexString.getChars(0, length, chars, 4 - length);
        return chars;
    }

    /**
     *
     * @param charset may be null. If null, no checks for the supported encoding would be
     * performed
     * @param input non null
     * @throws UnsupportedOperationException if given charset does not support characters
     * from given text
     */
    public String fromUnicode(String charset, String input)
        throws UnsupportedOperationException {
        StringBuffer ret = new StringBuffer();
        Matcher matcher = UNICODE_PATTERN.matcher(input);
        String error = null;
        while (matcher.find()) {
            try {
                String uniValue = matcher.group().substring(2);
                String newValue = new String(new char[] { (char) Integer.parseInt(uniValue, 16) });
                if(charset != null) {
                    error = canEncode(charset, newValue, uniValue);
                    if(error != null) {
                        break;
                    }
                }
                matcher.appendReplacement(ret, quoteReplacement(newValue));
            } catch (NumberFormatException t) {
                matcher.appendReplacement(ret, quoteReplacement(matcher.group()));
            }
        }
        if(error != null) {
            throw new UnsupportedOperationException(error);
        }
        matcher.appendTail(ret);
        return ret.toString();
    }

    // TODO already exists in 1.5 JDK, but here to be compatible with 1.4
    public static String quoteReplacement(String s) {
        if (s.indexOf('\\') == -1 && s.indexOf('$') == -1) {
            return s;
        }
        int length = s.length();
        StringBuffer sb = new StringBuffer(length + 10);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append('\\').append('\\');
            } else if (c == '$') {
                sb.append('\\').append('$');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     *
     * @param charset non null
     * @param text non null
     * @param unicodeValue
     * @return null if text could be encoded, error message otherwise
     */
    public static String canEncode(String charset, CharSequence text, String unicodeValue) {
        Charset cs;
        try {
            cs = Charset.forName(charset);
        } catch (IllegalCharsetNameException e) {
            return "Charset name '" + charset + "' is illegal.";
        } catch (UnsupportedCharsetException e) {
            return "Charset '" + charset + "' is not supported.";
        }
        if(cs.canEncode() && cs.newEncoder().canEncode(text)) {
            return null;
        }
        return "Charset '" + charset + "' does not support encoding for \\u" + unicodeValue + ".";
    }

    public static boolean isValidLineSeparatorRegex(String regex) {
    	try {
        	Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
        	AnyEditToolsPlugin.errorDialog(Messages.OpenLineSeparatorRegex_WarningInvalidRegex, e);
        	return false;
        }
    }

    public static synchronized void updateTextUtils() {
        TextUtil textUtils = getInstance();
        IPreferenceStore store = AnyEditToolsPlugin.getDefault().getPreferenceStore();
        textUtils.setCharsDisallowedInPath(store
                .getString(IAnyEditConstants.CHARACTERS_DISALLOWED_IN_PATH));
        textUtils.setCharsRequiredInPath(store
                .getString(IAnyEditConstants.CHARACTERS_REQUIRED_IN_PATH));
        textUtils.setLineSeparatorRegex(store
                .getString(IAnyEditConstants.LINE_SEPARATOR_REGEX));

        textUtils.setUseRequiredInPathChars(store
                .getBoolean(IAnyEditConstants.USE_REQUIRED_IN_PATH_CHARACTERS));
        textUtils.base64LineLength = store.getInt(IAnyEditConstants.BASE64_LINE_LENGTH);
        if(textUtils.base64LineLength <= 0) {
            // paranoia
            textUtils.base64LineLength = DEFAULT_BASE64_LINE_LENGTH;
        }
        textUtils.unicodifyAll = store.getBoolean(IAnyEditConstants.UNICODIFY_ALL);
    }

    public static TextUtil getDefaultTextUtilities() {
        updateTextUtils();
        return getInstance();
    }

    public static boolean convertTabsToSpaces(StringBuffer line, int tabWidth,
            boolean removeTrailing, boolean replaceAllTabs, boolean useModulo4Tabs) {
        char lastChar;
        boolean changed = false;
        if (removeTrailing) {
            changed = removeTrailingSpace(line);
        }
        int lineLength = line.length();
        int spacesCount = 0;
        int tabsCount = 0;
        int lastIdx = 0;
        for (; lastIdx < lineLength; lastIdx++) {
            lastChar = line.charAt(lastIdx);
            if (lastChar == '\t') {
                changed = true;
                tabsCount++;
            } else if (lastChar == ' ') {
                spacesCount++;
            } else {
                break;
            }
        }
        if (tabsCount > 0) {
            spacesCount = calculateSpaces4Tabs(spacesCount, tabsCount, tabWidth,
                    useModulo4Tabs);

            // delete whitespace to 'last' index, replace with spaces
            line.delete(0, lastIdx);
            line.insert(0, fillWith(spacesCount, ' '));
        }
        if (replaceAllTabs) {
            if (lastIdx >= lineLength) {
                lastIdx = 0;
            }
            changed |= replaceAllTabs(line, lastIdx, tabWidth);
        }
        return changed;
    }

    private static int calculateSpaces4Tabs(int spacesCount, int tabsCount, int tabWidth,
            boolean useModulo4Tabs) {
        if (!useModulo4Tabs) {
            return spacesCount + tabsCount * tabWidth;
        }
        /*
         * This does work well if and only if all three conditions below are met:
         * 1) the same tab size was used as the one set in AnyEdit preferences
         * 2) spaces wasn't "cross over" mixed with tabs multiple times in a line
         * 3) spaces prepends tabs
         */
        return spacesCount - (spacesCount % tabWidth) + tabsCount * tabWidth;
    }

    private static int calculateTabs4Spaces(int spacesCount, int tabWidth) {
        int tabs = spacesCount / tabWidth;
        int rest = spacesCount % tabWidth != 0? 1 : 0;
        return tabs + rest;
    }

    private static boolean replaceAllTabs(StringBuffer line, int start, int tabWidth) {
        String spaces = null;
        boolean changed = false;
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                if (spaces == null) {
                    spaces = String.valueOf(fillWith(tabWidth, ' '));
                }
                line.replace(i, i + 1, spaces);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean replaceAllSpaces(StringBuffer line, int start, int tabWidth) {
        boolean changed = false;
        int spacesCount = 0;
        int lastIdx = start;
        int firstIdx = start;
        for (; lastIdx < line.length(); lastIdx++) {
            char c = line.charAt(lastIdx);
            if (c == ' ') {
                if(spacesCount == 0){
                    firstIdx = lastIdx;
                }
                spacesCount ++;
            } else if(spacesCount > 0){
                int tabsCount = calculateTabs4Spaces(spacesCount, tabWidth);
                line.replace(firstIdx, lastIdx, String.valueOf(fillWith(tabsCount, '\t')));
                changed = true;
                spacesCount = 0;
                lastIdx = firstIdx + tabsCount;
            }
        }
        if(spacesCount > 0){
            int tabsCount = calculateTabs4Spaces(spacesCount, tabWidth);
            line.replace(firstIdx, lastIdx, String.valueOf(fillWith(tabsCount, '\t')));
            changed = true;
        }
        return changed;
    }

    public static boolean removeTrailingSpace(StringBuffer line) {
        boolean changed = false;
        char lastChar;
        int lineLength = line.length();
        int lastCharsLength = getLineEnd(line).length;
        int lastIdx = lineLength - lastCharsLength - 1;
        while (lastIdx >= 0) {
            lastChar = line.charAt(lastIdx);
            if (lastChar != ' ' && lastChar != '\t') {
                break;
            }
            lastIdx--;
        }
        if (lastIdx != lineLength - lastCharsLength - 1) {
            line.delete(lastIdx + 1, lineLength - lastCharsLength);
            changed = true;
        }

        return changed;
    }

    public static boolean convertSpacesToTabs(StringBuffer line, int tabWidth,
            boolean removeTrailing, boolean replaceAllSpaces) {
        boolean changed = false;
        if (removeTrailing) {
            changed = removeTrailingSpace(line);
        }
        int lineLength = line.length();
        int spacesCount = 0;
        int tabsCount = 0;
        int lastIdx = 0;
        char lastChar = '?';
        for (; lastIdx < lineLength; lastIdx++) {
            lastChar = line.charAt(lastIdx);
            if (lastChar == ' ') {
                changed = true;
                spacesCount++;
            } else if (lastChar == '\t') {
                tabsCount++;
            } else {
                break;
            }
        }

        if (spacesCount > 0) {
            boolean isComment = lastChar == '*';
            int additionalTabs = spacesCount / tabWidth;
            if(additionalTabs == 0 && tabsCount == 0){
                if(replaceAllSpaces) {
                    additionalTabs = 1;
                    spacesCount = 0;
                } else {
                    // XXX remove leading spaces, except for javadoc
                    if(!isComment){
                        line.delete(0, lastIdx);
                        changed = true;
                    }
                    return changed;
                }
            }
            if (additionalTabs == 0 && !replaceAllSpaces) {
                line.delete(0, tabsCount + spacesCount);
                if(tabsCount > 0) {
                    line.insert(0, fillWith(tabsCount, '\t'));
                }
                // XXX add extra space for javadoc
                if(isComment){
                    line.insert(tabsCount, fillWith(1, ' '));
                }
                return true;
            }
            tabsCount += additionalTabs;
            // modulo rest
            int extraSpaces = spacesCount % tabWidth;

            // delete whitespace to 'last' index, replace with tabs
            line.delete(0, lastIdx);
            line.insert(0, fillWith(tabsCount, '\t'));
            // if some last spaces exists, add them back
            if (extraSpaces > 0) {
                if(replaceAllSpaces){
                    line.insert(tabsCount, fillWith(1, '\t'));
                } else {
                    line.insert(tabsCount, fillWith(extraSpaces, ' '));
                }
            }
        }
        if (replaceAllSpaces) {
            changed |= replaceAllSpaces(line, tabsCount, tabWidth);
        }
        return changed;
    }

    private static char[] getLineEnd(StringBuffer line) {
        if (line == null) {
            return EMPTY;
        }
        int lastIdx = line.length() - 1;
        if (lastIdx < 0) {
            return EMPTY;
        }
        char last = line.charAt(lastIdx);
        if (last == '\n') {
            if (lastIdx > 0) {
                if (line.charAt(lastIdx - 1) == '\r') {
                    return CRLF; // windows
                }
            }
            return LF; // unix
        } else if (last == '\r') {
            return CR; // mac
        } else {
            return EMPTY;
        }
    }

    /**
     * @return number of occurencies of c in s
     */
    public static int count(String s, char c) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return char array with specified amount of given characters.
     */
    private static char[] fillWith(int length, char c) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = c;
        }
        return chars;
    }

    /**
     * Characters used for escape operations
     */
    private static final String[][] HTML_ESCAPE_CHARS = { {
            "&lt;", "<" }, {
            "&gt;", ">" }, {
            "&amp;", "&" }, {
            "&quot;", "\"" }, {
            "&agrave;", "\u00e0" }, {
            "&Agrave;", "\u00c0" }, {
            "&acirc;", "\u00e2" }, {
            "&auml;", "\u00e4" }, {
            "&Auml;", "\u00c4" }, {
            "&Acirc;", "\u00c2" }, {
            "&aring;", "\u00e5" }, {
            "&Aring;", "\u00c5" }, {
            "&aelig;", "\u00e6" }, {
            "&AElig;", "\u00c6" }, {
            "&ccedil;", "\u00e7" }, {
            "&Ccedil;", "\u00c7" }, {
            "&eacute;", "\u00e9" }, {
            "&Eacute;", "\u00c9" }, {
            "&aacute;", "\u00e1" }, {
            "&Aacute;", "\u00c1" }, {
            "&egrave;", "\u00e8" }, {
            "&Egrave;", "\u00c8" }, {
            "&ecirc;", "\u00ea" }, {
            "&Ecirc;", "\u00ca" }, {
            "&euml;", "\u00eb" }, {
            "&Euml;", "\u00cb" }, {
            "&iuml;", "\u00ef" }, {
            "&Iuml;", "\u00cf" }, {
            "&iacute;", "\u00ed" }, {
            "&Iacute;", "\u00cd" }, {
            "&atilde;", "\u00e3" }, {
            "&Atilde;", "\u00c3" }, {
            "&otilde;", "\u00f5" }, {
            "&Otilde;", "\u00d5" }, {
            "&oacute;", "\u00f3" }, {
            "&Oacute;", "\u00d3" }, {
            "&ocirc;", "\u00f4" }, {
            "&Ocirc;", "\u00d4" }, {
            "&ouml;", "\u00f6" }, {
            "&Ouml;", "\u00d6" }, {
            "&oslash;", "\u00f8" }, {
            "&Oslash;", "\u00d8" }, {
            "&szlig;", "\u00df" }, {
            "&ugrave;", "\u00f9" }, {
            "&Ugrave;", "\u00d9" }, {
            "&uacute;", "\u00fa" }, {
            "&Uacute;", "\u00da" }, {
            "&ucirc;", "\u00fb" }, {
            "&Ucirc;", "\u00db" }, {
            "&uuml;", "\u00fc" }, {
            "&Uuml;", "\u00dc" }, {
            "&nbsp;", " " }, {
            "&reg;", "\u00AE" }, {
            "&copy;", "\u00A9" }, {
            "&euro;", "\u20A0" }, {
            "&#8364;", "\u20AC" }

    };

    /**
     * Get html entity for escape character
     * @return null, if no entity found for given character
     */
    public static String getEntityForChar(char ch) {
        switch (ch) {
        case '<':
            return "&lt;";
        case '>':
            return "&gt;";
        case '&':
            return "&amp;";
        case '"':
            return "&quot;";
        case '\u00e0':
            return "&agrave;";
        case '\u00e1':
            return "&aacute;";
        case '\u00c0':
            return "&Agrave;";
        case '\u00c1':
            return "&Aacute;";
        case '\u00e2':
            return "&acirc;";
        case '\u00c2':
            return "&Acirc;";
        case '\u00e4':
            return "&auml;";
        case '\u00c4':
            return "&Auml;";
        case '\u00e5':
            return "&aring;";
        case '\u00c5':
            return "&Aring;";
        case '\u00e3':
            return "&atilde;";
        case '\u00c3':
            return "&Atilde;";
        case '\u00e6':
            return "&aelig;";
        case '\u00c6':
            return "&AElig;";
        case '\u00e7':
            return "&ccedil;";
        case '\u00c7':
            return "&Ccedil;";
        case '\u00e9':
            return "&eacute;";
        case '\u00c9':
            return "&Eacute;";
        case '\u00e8':
            return "&egrave;";
        case '\u00c8':
            return "&Egrave;";
        case '\u00ea':
            return "&ecirc;";
        case '\u00ca':
            return "&Ecirc;";
        case '\u00eb':
            return "&euml;";
        case '\u00cb':
            return "&Euml;";
        case '\u00ed':
            return "&iacute;";
        case '\u00cd':
            return "&Iacute;";
        case '\u00ef':
            return "&iuml;";
        case '\u00cf':
            return "&Iuml;";
        case '\u00f5':
            return "&otilde;";
        case '\u00d5':
            return "&Otilde;";
        case '\u00f3':
            return "&oacute;";
        case '\u00f4':
            return "&ocirc;";
        case '\u00d3':
            return "&Oacute;";
        case '\u00d4':
            return "&Ocirc;";
        case '\u00f6':
            return "&ouml;";
        case '\u00d6':
            return "&Ouml;";
        case '\u00f8':
            return "&oslash;";
        case '\u00d8':
            return "&Oslash;";
        case '\u00df':
            return "&szlig;";
        case '\u00f9':
            return "&ugrave;";
        case '\u00d9':
            return "&Ugrave;";
        case '\u00fa':
            return "&uacute;";
        case '\u00da':
            return "&Uacute;";
        case '\u00fb':
            return "&ucirc;";
        case '\u00db':
            return "&Ucirc;";
        case '\u00fc':
            return "&uuml;";
        case '\u00dc':
            return "&Uuml;";
        case '\u00AE':
            return "&reg;";
        case '\u00A9':
            return "&copy;";
        case '\u20A0':
            return "&euro;";
        case '\u20AC':
            return "&#8364;";
            // case '' :   return "&euro;";
            // case '\u20AC': return "&#x20AC;";  // euro

            // be carefull with this one (non-breaking white space)
            //case ' ' :    return "&nbsp;";
        default: {
            //Submitted by S. Bayer.
            int ci = 0xffff & ch;
            if (ci < 160) {
                // nothing special only 7 Bit
                return null;
            }
            // Not 7 Bit use the unicode system
            return "&#" + ci + ";";
        }
        }
    }

    /**
     * change escape characters to html entities (from  http://www.rgagnon.com/howto.html)
     * @param s string to be modified
     * @return string with escape characters, changed to html entities
     */
    public static String escapeText(String s) {
        if (s == null) {
            // shit in, shit out
            return null;
        }
        StringBuffer sb = new StringBuffer();
        int n = s.length();
        char c;
        String entity;
        for (int i = 0; i < n; i++) {
            c = s.charAt(i);
            entity = getEntityForChar(c);
            if (entity != null) {
                sb.append(entity);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * change html entities to escape characters (from http://www.rgagnon.com/howto.html)
     * @param s string to unescape
     * @return new string with html entities changed to escape characters
     */
    public static String unescapeText(String s) {
        int i, j, k;
        int arraySize = HTML_ESCAPE_CHARS.length;
        if (s != null && (i = s.indexOf("&")) > -1) {
            j = s.indexOf(";", i);
            if (j > i) {
                String temp = s.substring(i, j + 1);
                // search in escape[][] if temp is there
                k = 0;
                while (k < arraySize) {
                    if (HTML_ESCAPE_CHARS[k][0].equals(temp)) {
                        break;
                    }
                    k++;
                }
                // now we found html escape character
                if (k < arraySize) {
                    // replace it to ASCII
                    s = new StringBuffer(s.substring(0, i)).append(
                            HTML_ESCAPE_CHARS[k][1]).append(s.substring(j + 1))
                            .toString();
                    return unescapeText(s); // recursive call
                } else if (k == arraySize) {
                    s = new StringBuffer(s.substring(0, i)).append("&")
                            .append(unescapeText(s.substring(i + 1))).toString();
                    return s;
                }
            }
        }
        return s;
    }

    /**
     * get index of first non-whitespace letter (one of " \t\r\n")
     * @return -1 if no such (non-whitespace) character found from given
     * startOffset (inclusive)
     */
    private static int indexOfNextWord(String line, int startOffset, int lastIdx) {
        int size = line.length();
        char c;
        boolean continueSequence = lastIdx + 1 == startOffset;
        for (int i = startOffset; i < size; i++) {
            c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                continueSequence = false;
                continue;
            } else if (continueSequence) {
                continue;
            }
            return i;
        }
        return -1;
    }

    public static String capitalize(String line) {
        StringBuffer sb = new StringBuffer(line);
        int size = line.length();
        boolean changed = false;
        char c;
        int lastWordIdx = 0;
        for (int i = 0; i < size; i++) {
            i = indexOfNextWord(line, i, lastWordIdx);
            if (i < 0) {
                break;
            }
            c = line.charAt(i);
            if (Character.isLowerCase(c)) {
                c = Character.toUpperCase(c);
                sb.setCharAt(i, c);
                changed = true;
            }
            lastWordIdx = i;
        }
        if (changed) {
            return new String(sb);
        }
        return line;
    }

    public static String invertCase(String line) {
        char[] chars = line.toCharArray();
        char c;
        boolean changed = false;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];
            // XXX DOESN'T WORK WITH UNICODE SPECIAL CHARS!!!!
            if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
                changed = true;
            } else if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
                changed = true;
            }
        }
        if (changed) {
            return String.valueOf(chars);
        }
        return line;
    }

}
