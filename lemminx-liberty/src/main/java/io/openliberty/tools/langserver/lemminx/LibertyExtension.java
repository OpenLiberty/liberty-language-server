/*******************************************************************************
* Copyright (c) 2020, 2023 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx;

import io.openliberty.tools.langserver.lemminx.services.FileWatchService;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.completion.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.hover.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lemminx.services.extensions.save.ISaveContext.SaveContextType;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkspaceFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;

public class LibertyExtension implements IXMLExtension {

    private static final Logger LOGGER = Logger.getLogger(LibertyExtension.class.getName());

    private URIResolverExtension xsdResolver;
    private ICompletionParticipant completionParticipant;
    private IHoverParticipant hoverParticipant;
    private IDiagnosticsParticipant diagnosticsParticipant;
    private ICodeActionParticipant codeActionsParticipant;
    private IDocumentLinkParticipant documentLinkParticipant;

    @Override
    public void start(InitializeParams initializeParams, XMLExtensionsRegistry xmlExtensionsRegistry) {
        try {
            List<WorkspaceFolder> folders = initializeParams.getWorkspaceFolders();
            if (folders != null) {
                LibertyProjectsManager.getInstance().setWorkspaceFolders(folders);
            }
        } catch (NullPointerException e) {
            LOGGER.warning("Could not get workspace folders: " + e.toString());
        }
        xsdResolver = new LibertyXSDURIResolver();
        xmlExtensionsRegistry.getResolverExtensionManager().registerResolver(xsdResolver);

        completionParticipant = new LibertyCompletionParticipant();
        xmlExtensionsRegistry.registerCompletionParticipant(completionParticipant);

        hoverParticipant = new LibertyHoverParticipant();
        xmlExtensionsRegistry.registerHoverParticipant(hoverParticipant);

        diagnosticsParticipant = new LibertyDiagnosticParticipant();
        xmlExtensionsRegistry.registerDiagnosticsParticipant(diagnosticsParticipant);

        codeActionsParticipant = new LibertyCodeActionParticipant();
        xmlExtensionsRegistry.registerCodeActionParticipant(codeActionsParticipant);

        documentLinkParticipant = new LibertyDocumentLinkParticipant();
        xmlExtensionsRegistry.registerDocumentLinkParticipant(documentLinkParticipant);

        try {
            SettingsService.getInstance()
                    .populateAllVariables(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // for each workspace, a file alteration observer is added
        for (LibertyWorkspace workspace : LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders()) {
            // checking for any changes in wlp user folder for gradle and maven
            Path libertyUsrGradlePath = new File(workspace.getWorkspaceURI().getPath(),
                    "target").toPath();
            Path libertyUsrMavenPath = new File(workspace.getWorkspaceURI().getPath(),
                    "build").toPath();
            List<String> paths = Arrays.asList(libertyUsrMavenPath.toString(), libertyUsrGradlePath.toString());
            try {
                FileWatchService.getInstance()
                        .addFileAlterationObserver(workspace, paths);
            } catch (Exception e) {
                LOGGER.warning("unable to add file alteration observer for paths " + paths
                        + " with error message " + e.getMessage());
            }
        }
    }

    @Override
    public void stop(XMLExtensionsRegistry xmlExtensionsRegistry) {
        // clean up .libertyls folders
        LibertyProjectsManager.getInstance().cleanUpTempDirs();
        LibertyProjectsManager.getInstance().cleanInstance();

        xmlExtensionsRegistry.getResolverExtensionManager().unregisterResolver(xsdResolver);
        xmlExtensionsRegistry.unregisterCompletionParticipant(completionParticipant);
        xmlExtensionsRegistry.unregisterHoverParticipant(hoverParticipant);
        xmlExtensionsRegistry.unregisterDiagnosticsParticipant(diagnosticsParticipant);
        xmlExtensionsRegistry.unregisterCodeActionParticipant(codeActionsParticipant);
        FileWatchService.getInstance().cleanFileMonitors();
    }

    // Do save is called on startup with a Settings update
    // and any time the settings are updated.
    @Override
    public void doSave(ISaveContext saveContext) {
        // Only need to update settings if the save event was for settings
        // Not if an xml file was updated.
        if (saveContext.getType() == SaveContextType.SETTINGS) {
            Object xmlSettings = saveContext.getSettings();
            SettingsService.getInstance().updateLibertySettings(xmlSettings);
            LOGGER.info("Liberty XML settings updated");
        }
    }
}
