/*******************************************************************************
* Copyright (c) 2020, 2025 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.services;

import io.openliberty.tools.common.plugins.config.ServerConfigDocument;
import io.openliberty.tools.langserver.lemminx.util.CommonLogger;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.eclipse.lemminx.utils.JSONUtility;
import io.openliberty.tools.langserver.lemminx.models.settings.*;
import org.eclipse.lsp4j.InitializeParams;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static io.openliberty.tools.langserver.lemminx.util.LibertyUtils.findFileInWorkspace;

public class SettingsService {

    // Singleton so that only 1 Settings Service can be initialized and is
    // shared between all Lemminx Language Feature Participants

    private static SettingsService instance = new SettingsService();

    public static SettingsService getInstance() {
        return instance;
    }

    // default request delay is 10 seconds
    private static int DEFAULT_REQUEST_DELAY = 10;
    private static final Logger LOGGER = Logger.getLogger(SettingsService.class.getName());

    private SettingsService() {
    }

    private LibertySettings settings;

    private Map<String,Properties> variables;
    private Locale currentLocale = Locale.getDefault();
    private boolean configCopiedToServer = false;
    /**
     * Takes the xml settings object and parses out the Liberty Settings
     * @param xmlSettings - All xml settings provided by the client
     */
    public void updateLibertySettings(Object xmlSettings) {
        AllSettings rootSettings = JSONUtility.toModel(xmlSettings, AllSettings.class);
        if (rootSettings != null) {
            settings = JSONUtility.toModel(rootSettings.getLiberty(), LibertySettings.class);
        }
    }

    public String getLibertyVersion() {
        return settings != null ? settings.getVersion() : null;
    }

    public String getLibertyRuntime() {
        return settings != null ? settings.getRuntime() : null;
    }

    public int getRequestDelay() {
        if (settings != null) {
            int requestDelay = settings.getRequestDelay();
            if (requestDelay > 0) {
                return requestDelay;
            }
        }

        return DEFAULT_REQUEST_DELAY;
    }

    /**
     * populate all variables for all available workspace folders
     *
     * @param workspaceFolders workspace folders
     */
    public void populateAllVariables(Collection<LibertyWorkspace> workspaceFolders) {
        variables = new HashMap<>();
        for (LibertyWorkspace workspace : workspaceFolders) {
            populateVariablesForWorkspace(workspace);
        }
    }

    /**
     * read all variables from workspace directories
     *
     * @param workspace workspace
     */
    public void populateVariablesForWorkspace(LibertyWorkspace workspace) {
        Properties variablesForWorkspace = new Properties();
        Path pluginConfigFilePath = findFileInWorkspace(workspace, Paths.get("liberty-plugin-config.xml"));
        if (pluginConfigFilePath != null) {
            File installDirectory = LibertyUtils.getFileFromLibertyPluginXml(pluginConfigFilePath, "installDirectory");
            File serverDirectory = LibertyUtils.getFileFromLibertyPluginXml(pluginConfigFilePath, "serverDirectory");
            File userDirectory = LibertyUtils.getFileFromLibertyPluginXml(pluginConfigFilePath, "userDirectory");
            File serverOutputDirectory = LibertyUtils.getFileFromLibertyPluginXml(pluginConfigFilePath, "serverOutputDirectory");
            if (serverDirectory != null && installDirectory != null && userDirectory != null && serverOutputDirectory !=null) {
                try {
                    ServerConfigDocument serverConfigDocument = new ServerConfigDocument(
                            new CommonLogger(LOGGER), null, installDirectory, userDirectory, serverDirectory, serverOutputDirectory);
                    variablesForWorkspace.putAll(serverConfigDocument.getDefaultProperties());
                    variablesForWorkspace.putAll(serverConfigDocument.getProperties());
                    LOGGER.finest("Populated variables for workspace: " + workspace.getWorkspaceString() + ". Number of variables found: " + variablesForWorkspace.size());
                } catch (Exception e) {
                    LOGGER.warning("Variable resolution is not available because the necessary directory locations were not found in the liberty-plugin-config.xml file.");
                    LOGGER.info("Exception received: " + e.getMessage());
                }
            }
        } else {
            LOGGER.warning("Could not find liberty-plugin-config.xml in workspace URI " + workspace.getWorkspaceString() + ". Variable resolution cannot be performed");
        }
        variables.put(workspace.getWorkspaceString(), variablesForWorkspace);
    }

    /**
     * Get variables list for a workspace server xml file
     *
     * @param serverXmlURI serverXmlURI
     * @return variables
     */
    public Properties getVariablesForServerXml(String serverXmlURI) {
        LibertyWorkspace workspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlURI);
        Properties variableProps = new Properties();
        if (workspace == null) {
            LOGGER.warning("Could not find workspace for server xml URI %s. Variable resolution cannot be performed.".formatted(serverXmlURI));
        } else if (variables != null && variables.containsKey(workspace.getWorkspaceString())) {
            variableProps = variables.get(workspace.getWorkspaceString());
        } else {
            LOGGER.warning("Could not find variable mapping for workspace URI %s. Variable resolution cannot be performed.".formatted(workspace.getWorkspaceString()));
        }
        return variableProps;
    }

    public boolean isConfigCopiedToServer() {
        return configCopiedToServer;
    }

    public void setConfigCopiedToServer(boolean configCopiedToServer) {
        this.configCopiedToServer = configCopiedToServer;
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public void initializeLocale(InitializeParams initializeParams) {
        if (initializeParams != null && initializeParams.getLocale() != null) {
            this.currentLocale = LocaleUtils.toLocale(initializeParams.getLocale());
        }
    }

    public List<Locale> getAvailableLocales() {
        return Arrays.asList(
                new Locale("pt", "BR"), Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE,
                new Locale("cs", "CZ"), Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
                new Locale("hu", "HU"), Locale.ITALIAN, Locale.JAPANESE, Locale.KOREAN,
                new Locale("pl", "PL"), new Locale("ro", "RO"), new Locale("ru", "RU"),
                new Locale("es", "ES")
        );
    }

    // Check if the liberty-plugin-config.xml is copied to server or not
    public boolean isLibertyPluginConfigAvailableInServer(LibertyWorkspace libertyWorkspace) {
        if (libertyWorkspace != null) {
            Path pluginConfigFilePath = LibertyUtils.findFileInWorkspace(libertyWorkspace, Paths.get("liberty-plugin-config.xml"));
            return pluginConfigFilePath != null;
        }
        return false;
    }
}
