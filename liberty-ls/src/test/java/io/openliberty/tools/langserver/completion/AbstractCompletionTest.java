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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

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

    protected void checkCompletionsContainAllStrings(List<CompletionItem> completionItems, List<String> values) {
        String[] valuesToFind = new String[values.size()];
        for (int i=0; i < values.size(); i++) {
            valuesToFind[i] = values.get(i);
        }
        checkCompletionsContainAllStrings(completionItems, valuesToFind);
    }

    /**
     * 
     * Verify the detail matches the expected value for specified completion items. Also verify the kind is set to text.
     *
     * @param completionItems - List<CompletionItem> from Liberty Language Server completion
     * @param key - key of CompletionItem to verify, or null if all should be verified
     * @param detail - detail to verify
     */
    protected void checkCompletionContainsDetail(List<CompletionItem> completionItems, String key, String detail) {
        Iterator<CompletionItem> it = completionItems.iterator();
        boolean found = false;
        while (it.hasNext()) {
            CompletionItem nextItem = it.next();
            if (key == null) {
                found = true;
                String nextItemDetail = nextItem.getDetail();
                assertNotNull(nextItemDetail);
                assertTrue("The CompletionItem actual detail: "+nextItemDetail+" did not match expected: "+detail, nextItemDetail.equals(detail));
            } else {
                if (nextItem.getLabel().equals(key)) {
                    String nextItemDetail = nextItem.getDetail();
                    assertNotNull(nextItemDetail);
                    assertTrue("The CompletionItem actual detail: "+nextItemDetail+" did not match expected: "+detail, nextItemDetail.equals(detail));
                    found = true;
                } else {
                    assertNotNull(nextItem.getDetail());
                }
            }
            CompletionItemKind kind = nextItem.getKind();
            assertTrue("Unexpected CompletionItemKind: "+kind+" expected: "+CompletionItemKind.Text, kind == CompletionItemKind.Text);

        }
        assertTrue(found);
    }

    protected CompletionItem createExpectedCompletionItem(String value) {
        CompletionItem expectedCompletionItem = new CompletionItem(value);
        return expectedCompletionItem;
    }
}
