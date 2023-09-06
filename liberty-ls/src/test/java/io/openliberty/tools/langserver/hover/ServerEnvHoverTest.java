/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.hover;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyLanguageServer;

public class ServerEnvHoverTest extends AbstractHoverTest {
    
    @Test
    public void testHoverOnPropertyName() throws Exception {
        String propertyEntry = "LOG_FILE=log.txt";
        CompletableFuture<Hover> hover = getHover(propertyEntry, 1);

        assertEquals("The log file name. This log file is only used if the server start command is run in the background through the start action.", hover.get().getContents().getRight().getValue());
    }

    private CompletableFuture<Hover> getHover(String propertyEntry, int position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "server.env";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();

        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, propertyEntry));
        return getHover(lls, position, fileURI);
    }
}
