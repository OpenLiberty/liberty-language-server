package io.openliberty.tools.langserver.utils;

import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ParserFileHelperUtil {

    public String getLine(LibertyTextDocument textDocumentItem, Position position) {
        return getLine(textDocumentItem, position.getLine());
    }
    
    public String getLine(LibertyTextDocument textDocumentItem, int line) {
        return getLine(textDocumentItem.getText(), line);
    }

    public String getLine(String text, int line) {
        String[] lines = text.split("\\r?\\n", line + 2);
        if (lines.length >= line + 1) {
            return lines[line];
        }
        return null;
    }
}