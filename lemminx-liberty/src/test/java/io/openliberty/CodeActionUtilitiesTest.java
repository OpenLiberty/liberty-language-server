package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.codeactions.AddTrailingSlash;
import io.openliberty.tools.langserver.lemminx.codeactions.IndentUtil;

public class CodeActionUtilitiesTest {

    @Test
    public void indentSpacesTest() {
        String indent = "    "; // four spaces
        String sampleText = "\n\t<feature>test<feature>";
        int column = 4;
        String expectedText = System.lineSeparator() + "        <feature>test<feature>";
        String formatedText = IndentUtil.formatText(sampleText, indent, column);
        assertEquals(expectedText, formatedText, "Expected length of " + expectedText.length() + ". Found " + formatedText.length());

        int column2 = 3;
        String expectedText2 = System.lineSeparator() + "    <feature>test<feature>";
        assertEquals(expectedText2, IndentUtil.formatText(sampleText, indent, column2), "Incorrect detection starting indent.");
    }

    @Test
    public void indentTabsTest() {
        String indent = "	";
        String sampleText = "\n\t<feature>test<feature>";
        int column = 1;
        String expectedText = System.lineSeparator() + indent + indent + "<feature>test<feature>";
        assertEquals(expectedText, IndentUtil.formatText(sampleText, indent, column), "Incorrect whitespace buffer calculation.");
    }

    /**
     * Test that a location string with mismatched slashes will be converted to all to forward slashes
     */
    @Test
    public void slashConversionTest() {
        String fileSeparator = "/";
        String locationText = "..\\../test.xml";
        String replaceText = AddTrailingSlash.getReplaceText(fileSeparator, locationText);
        assertFalse(replaceText.contains("\\"));
        assertTrue(replaceText.endsWith("/\"")); // ends with /"
    }

    /**
     * Windows test that backslashes will be retained if it was the only type of slash used
     */
    @Test
    public void retainWindowsBackSlash() {
        if (!File.separator.equals("\\")) {
            return;
        }
        String fileSeparator = "/";
        String locationText = "..\\..\\test.xml";
        String replaceText = AddTrailingSlash.getReplaceText(fileSeparator, locationText);
        assertTrue(replaceText.contains("\\"));
        assertFalse(replaceText.contains("/"));
        assertTrue(replaceText.endsWith("\\\"")); // ends with \"
    }
}
