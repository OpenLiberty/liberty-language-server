/*******************************************************************************
* Copyright (c) 2020, 2024 IBM Corporation and others.
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

import java.util.logging.Logger;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.services.WorkspaceService;

public class LibertyWorkspaceService implements WorkspaceService {

    private final LibertyLanguageServer libertyLanguageServer;
    private static final Logger LOGGER = Logger.getLogger(LibertyWorkspaceService.class.getName());

    public LibertyWorkspaceService(LibertyLanguageServer libertyls) {
        this.libertyLanguageServer = libertyls;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        libertyLanguageServer.updateSettings(params.getSettings());
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        for (FileEvent change : params.getChanges()) {
            String uri = change.getUri();
            if (uri.endsWith(LibertyConfigFileManager.LIBERTY_PLUGIN_CONFIG_XML)) {
                LibertyConfigFileManager.processLibertyPluginConfigXml(uri);
            }
        }
    }
}
