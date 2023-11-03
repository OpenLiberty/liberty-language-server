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

// Use this helper class to format your strings with indentation
public class IndentUtil {
    public static final String NEW_LINE = System.lineSeparator();

    public static String whitespaceBuffer(String indent, int column) {
        StringBuilder sb = new StringBuilder();
        if ((indent != null) && (indent.length() > 0)) {
            for (int i = 0; i < column / indent.length(); ++i) {
                sb.append(indent);
            }
        }
        return sb.toString();
    }

    /**
     * Will return a string where `\n` will be replaced with a proper line separator and match the
     * indentation level for the passed in column number. Adding `\t` will add an indent level.
     * @param text
     * @param indent
     * @param column
     * @return
     */
    public static String formatText(String text, String indent, int column) {
        return text.replace("\n", System.lineSeparator() + whitespaceBuffer(indent, column))
                   .replace("\t", indent);
    }
}
