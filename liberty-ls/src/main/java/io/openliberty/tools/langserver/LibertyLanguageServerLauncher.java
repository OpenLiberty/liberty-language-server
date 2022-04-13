/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.tools.langserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import io.openliberty.tools.langserver.api.LibertyLanguageClientAPI;

public class LibertyLanguageServerLauncher {
    public static void main(String[] args) {
        LibertyLanguageServer server = new LibertyLanguageServer();

        Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out,
            Executors.newCachedThreadPool());

        server.setLanguageClient(launcher.getRemoteProxy());
        launcher.startListening();
    }

    /**
     * Create a new Launcher for a language server and an input and output stream.
     * Threads are started with the given executor service. The wrapper function is
     * applied to the incoming and outgoing message streams so additional message
     * handling such as validation and tracing can be included.
     *
     * @param server          - the server that receives method calls from the
     *                        remote client
     * @param in              - input stream to listen for incoming messages
     * @param out             - output stream to send outgoing messages
     * @param executorService - the executor service used to start threads
     * @param wrapper         - a function for plugging in additional message
     *                        consumers
     */
    public static Launcher<LanguageClient> createServerLauncher(LanguageServer server, InputStream in, OutputStream out,
        ExecutorService executorService) {
            return new Builder<LanguageClient>().setLocalService(server).setRemoteInterface(LibertyLanguageClientAPI.class).setInput(in).setOutput(out)
                .setExecutorService(executorService).create();
    }
}
