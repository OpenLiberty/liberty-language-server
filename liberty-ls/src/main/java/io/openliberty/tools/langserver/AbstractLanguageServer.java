/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver;

import java.util.logging.Logger;

public class AbstractLanguageServer {
    private final class LibertyServerRunnable implements Runnable {
        @Override
        public void run() {
            LOGGER.info("Starting Liberty Language Server");
            while (!shutdown && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    LOGGER.warning(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            if (!Thread.currentThread().isInterrupted()) {
                LOGGER.info("Liberty Language Server - Client vanished...");
            }
        }
    }
    private static final Logger LOGGER = Logger.getLogger(AbstractLanguageServer.class.getName());

    private Thread runner;
	private volatile boolean shutdown;

    public int startServer() {
        runner = new Thread(new LibertyServerRunnable(), "Liberty Language Client Watcher");
        runner.start();
        return 0;
    }

    public void stopServer() {
        LOGGER.info("Stopping Liberty language server");
        if (runner != null) {
            runner.interrupt();
        } else {
            LOGGER.info("Received request to stop Liberty language server, but it wasn't started.");
        }
    }

    public void shutdownServer() {
        LOGGER.info("Shutting down language server");
        shutdown = true;
    }
}
