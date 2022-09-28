/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.completion;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;

import io.openliberty.tools.langserver.AbstractLibertyLanguageServerTest;

public class AbstractCompletionTest extends AbstractLibertyLanguageServerTest {

    /**
     * 
     * @param completionItems - List<CompletionItem> from Liberty Language Server completion
     * @param valuesToFind - array of Strings that completionItems should contain
     */
    protected void checkCompletionsContainAllStrings(List<CompletionItem> completionItems, String... valuesToFind) {
        List<String> keys = new LinkedList<>(Arrays.asList(valuesToFind));
        Iterator<CompletionItem> it = completionItems.iterator();
        while (it.hasNext()) {
            String itemLabel = it.next().getLabel();
            assertTrue(keys.remove(itemLabel));
        }
        assertTrue(keys.isEmpty());
    }

    protected CompletionItem createExpectedCompletionItem(String value) {
        CompletionItem expectedCompletionItem = new CompletionItem(value);
        return expectedCompletionItem;
    }
}
