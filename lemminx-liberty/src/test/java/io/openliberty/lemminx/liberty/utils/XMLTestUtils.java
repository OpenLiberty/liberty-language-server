package io.openliberty.lemminx.liberty.utils;

public class XMLTestUtils {
    static String newLine = System.lineSeparator();

    public static String joinWithNewLine(String ... lines) {
        return String.join(newLine, lines);
    }
}
