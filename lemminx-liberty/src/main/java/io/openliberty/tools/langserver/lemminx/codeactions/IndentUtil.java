/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver.lemminx.codeactions;

public class IndentUtil {
    public static final String NEW_LINE = System.lineSeparator();

    public static String whitespaceBuffer(String indent, int column) {
        String whitespaceBuffer = "";
        for (int i = 0; i < column / indent.length(); i++) {
            whitespaceBuffer += indent;
        }
        return whitespaceBuffer;
    }

    public static String formatText(String text, String indent, int column) {
        return text.replace("\n", System.lineSeparator() + whitespaceBuffer(indent, column))
                   .replace("\t", indent);
    }
}
