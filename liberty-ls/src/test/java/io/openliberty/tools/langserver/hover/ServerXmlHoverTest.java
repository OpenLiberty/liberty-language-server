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

public class ServerXmlHoverTest extends AbstractHoverTest {

    @Test
    public void testHoverEmpty() throws Exception {
        String intputLine = "    <include location=\"${abc}\"/>";
        CompletableFuture<Hover> hover = getHover(intputLine, 0);
        assertEquals("", hover.get().getContents().getRight().getValue());
    }

    @Test
    public void testHoverValue() throws Exception {
        String inputLine = "    <include location=\"${abc}\"/>";
        CompletableFuture<Hover> hover = getHover(inputLine, 27);
        assertEquals("def", hover.get().getContents().getRight().getValue());
    }

    @Test
    public void testHoverEmpty2() throws Exception {
        String inputLine = "    <httpEndpoint httpPort=\"${http.port}\" httpsPort=\"9443\" id=\"defaultHttpEndpoint\"/>";
        CompletableFuture<Hover> hover = getHover(inputLine, 42);
        assertEquals("", hover.get().getContents().getRight().getValue());
    }

    @Test
    public void testHoverValue2() throws Exception {
        String inputLine = "    <httpEndpoint httpPort=\"${http.port}\" httpsPort=\"9443\" id=\"defaultHttpEndpoint\"/>";
        CompletableFuture<Hover> hover = getHover(inputLine, 33);
        assertEquals("9080", hover.get().getContents().getRight().getValue());
    }

    private CompletableFuture<Hover> getHover(String propertyEntry, int position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "server.xml";
        String resourcesDir = "src/test/resources/xml/";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();

        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, propertyEntry));
        return getHover(lls, position, fileURI);
    }
}
