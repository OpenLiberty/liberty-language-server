/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.utils;

import org.eclipse.lsp4j.Position;

import io.openliberty.tools.langserver.LibertyConfigFileManager;
import io.openliberty.tools.langserver.ls.LibertyTextDocument;

public class ParserFileHelperUtil {

    LibertyConfigFileManager ccm;

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

    // TODO: change filters to include custom files
    public static boolean isServerEnvFile(LibertyTextDocument tdi) {
        return tdi.getUri().endsWith("server.env");
    }

    public static boolean isBootstrapPropertiesFile(LibertyTextDocument tdi) {
        return tdi.getUri().endsWith("bootstrap.properties");
    }
}