package io.openliberty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
}
