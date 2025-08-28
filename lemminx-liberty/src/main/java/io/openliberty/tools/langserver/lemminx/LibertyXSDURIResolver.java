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
package io.openliberty.tools.langserver.lemminx;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyVersionDownloadUtil;
import io.openliberty.tools.langserver.lemminx.util.SchemaAndFeatureListGeneratorUtil;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.IExternalGrammarLocationProvider;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;

import io.openliberty.tools.langserver.lemminx.services.ContainerService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

import static io.openliberty.tools.langserver.lemminx.util.LibertyConstants.DEFAULT_LIBERTY_VERSION;

public class LibertyXSDURIResolver implements URIResolverExtension, IExternalGrammarLocationProvider {
    private static final Logger LOGGER = Logger.getLogger(LibertyXSDURIResolver.class.getName());

    // Changing this to contain the version in the file name since the file is copied to the local .lemminx cache.
    // This is how we ensure the latest default server schema gets used in each developer environment.
    private static final String XSD_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/liberty/server-cached-$VERSION_%s.xsd".replace("$VERSION", DEFAULT_LIBERTY_VERSION);
    private static final String XSD_CLASSPATH_LOCATION = "/schema/xsd/liberty/server-cached-$VERSION_%s.xsd".replace("$VERSION", DEFAULT_LIBERTY_VERSION);
    private static final String XSD_RESOURCE_URL_DEFAULT = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/liberty/server-cached-$VERSION.xsd".replace("$VERSION", DEFAULT_LIBERTY_VERSION);
    private static final String XSD_CLASSPATH_LOCATION_DEFAULT = "/schema/xsd/liberty/server-cached-$VERSION.xsd".replace("$VERSION", DEFAULT_LIBERTY_VERSION);
    public static final String LIBERTY_SCHEMA_VERSION_XSD = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_schema/$VERSION/open_liberty_schema-$VERSION.xsd";
    public static final String LIBERTY_SCHEMA_VERSION_WITH_LOCALE_XSD = "https://repo1.maven.org/maven2/io/openliberty/features/open_liberty_schema_$LOCALE/$VERSION/open_liberty_schema_$LOCALE-$VERSION.xsd";

    /**
     * SERVER_XSD_RESOURCE is the server schema that is located at XSD_CLASSPATH_LOCATION
     * that gets deployed (copied) to the .lemminx cache. The XSD_RESOURCE_URL is
     * used by lemmix to determine the path to store the file in the cache. So for the
     * server schema it takes the resource located at XSD_CLASSPATH_LOCATION and deploys
     * it to:
     * ~/.lemminx/cache/https/github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/src/main/resources/schema/xsd/server-cached-<version>.xsd
     *
     * Declared public to be used by tests
     */
    public ResourceToDeploy SERVER_XSD_RESOURCE;

    public static final ResourceToDeploy SERVER_XSD_RESOURCE_DEFAULT = new ResourceToDeploy(XSD_RESOURCE_URL_DEFAULT,
            XSD_CLASSPATH_LOCATION_DEFAULT);

    /**
     * Will return an existing xsd file or generate one from a Liberty installation if ones exists
     *
     * @param baseLocation
     * @param publicId
     * @param systemId
     *
     * @return Path to schema xsd resource as URI.toString()
     */
    public String resolve(String baseLocation, String publicId, String systemId) {
        if (!LibertyUtils.isConfigXMLFile(baseLocation)) {
            return null;
        }

        try {
            String serverXMLUri = URI.create(baseLocation).toString();
            LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLUri);

            if (libertyWorkspace != null) {
                //Set workspace properties if not set
                LibertyUtils.getLibertyRuntimeInfo(libertyWorkspace);

                //Check workspace for Liberty installation and generate schema.xsd file
                //Return schema URI as String, otherwise use cached schema.xsd file
                String serverSchemaUri = null;
                if (libertyWorkspace.isLibertyInstalled()) {
                    Path schemaGenJarPath = LibertyUtils.findLibertyFileForWorkspace(libertyWorkspace, Paths.get("bin", "tools", "ws-schemagen.jar"));
                    if (schemaGenJarPath != null) {
                        //Generate schema file
                        serverSchemaUri = generateServerSchemaXsd(libertyWorkspace, schemaGenJarPath);
                    }
                } else if (libertyWorkspace.isContainerAlive()) {
                    ContainerService container = ContainerService.getInstance();
                    serverSchemaUri = container.generateServerSchemaXsdFromContainer(libertyWorkspace);
                }
                if (serverSchemaUri != null && !serverSchemaUri.isEmpty()) {
                    return serverSchemaUri;
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error: Unable to generate the Liberty schema from the target Liberty runtime: "+e.getMessage());
        }

        try {
            Path serverXSDFile = getServerXSDFile();
            LOGGER.info("Using cached Liberty schema file located at: " + serverXSDFile.toString());
            return serverXSDFile.toUri().toString();
        } catch (Exception e) {
            LOGGER.severe("Error: Unable to retrieve default cached Liberty schema file.");
            return null;
        }
    }

    /**
     * get server xsd resource location
     *
     * @return
     * @throws IOException
     */
    public Path getServerXSDFile() throws IOException {
        Path serverXSDFile = LibertyVersionDownloadUtil.downloadAndCacheLatestResource(LIBERTY_SCHEMA_VERSION_XSD,LIBERTY_SCHEMA_VERSION_WITH_LOCALE_XSD);
        // if above download failed, fallback to schema stored in local classpath
        if (serverXSDFile == null) {
            if (Locale.US.equals(SettingsService.getInstance().getCurrentLocale())) {
                LOGGER.info("Locale is %s. Using default xsd resource located in %s".formatted(SettingsService.getInstance().getCurrentLocale(), SERVER_XSD_RESOURCE));
                return CacheResourcesManager.getResourceCachePath(SERVER_XSD_RESOURCE_DEFAULT);
            }
            try {
                SERVER_XSD_RESOURCE = new ResourceToDeploy(XSD_RESOURCE_URL.formatted(SettingsService.getInstance().getCurrentLocale().toString()),
                        XSD_CLASSPATH_LOCATION.formatted(SettingsService.getInstance().getCurrentLocale().toString()));
                LOGGER.info("Using Locale %s to find xsd resource in %s".formatted(SettingsService.getInstance().getCurrentLocale(), SERVER_XSD_RESOURCE));
                serverXSDFile = CacheResourcesManager.getResourceCachePath(SERVER_XSD_RESOURCE);
            } catch (Exception exception) {
                LOGGER.warning("Unable to find localized xsd resource using current locale %s. Using default xsd resource located in %s".formatted(SettingsService.getInstance().getCurrentLocale(), SERVER_XSD_RESOURCE_DEFAULT));
                serverXSDFile = CacheResourcesManager.getResourceCachePath(SERVER_XSD_RESOURCE_DEFAULT);
            }
        }
        return serverXSDFile;
    }

    @Override
    public Map<String, String> getExternalGrammarLocation(URI fileURI) {
        String xsdFile = resolve(fileURI.toString(), null, null);
        if (xsdFile == null) {
            return null;
        }

        Map<String, String> externalGrammar = new HashMap<>();
        externalGrammar.put(IExternalGrammarLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, xsdFile);
        return externalGrammar;
    }

    /**
     * Generate the schema file for a LibertyWorkspace using the ws-schemagen.jar in the corresponding Liberty installation
     * @param libertyWorkspace
     * @param schemaGenJarPath
     * @return Path to generated schema file.
     */
    private String generateServerSchemaXsd(LibertyWorkspace libertyWorkspace, Path schemaGenJarPath) {
        // java -jar {path to ws-schemagen.jar} {schemaVersion} {outputVersion} {outputFile}
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);

        //If tempDir is null, issue a warning for the current LibertyWorkspace URI and use the default cached schema file
        if (tempDir == null) {
            LOGGER.warning("Could not create a temporary directory for generating the schema file. The cached schema file will be used for the current workspace: " + libertyWorkspace.getWorkspaceString());
            return null;
        }

        File xsdDestFile = new File(tempDir, "server.xsd");
        if (libertyWorkspace.isLibertyRuntimeAndVersionSet()) {
            xsdDestFile = new File(tempDir, libertyWorkspace.getLibertyRuntime() + "-" + libertyWorkspace.getLibertyVersion() + ".xsd");
        }

        if (!xsdDestFile.exists()) {
            try {
                String xsdDestPath = xsdDestFile.getCanonicalPath();

                LOGGER.info("Generating schema file at: " + xsdDestPath);

                SchemaAndFeatureListGeneratorUtil.generateFile(
                        SchemaAndFeatureListGeneratorUtil.ProcessType.SCHEMA,
                        tempDir.toPath(),
                        schemaGenJarPath,
                        xsdDestFile,
                        SettingsService.getInstance().getCurrentLocale().toString()
                );
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                LOGGER.warning("Due to an exception during schema file generation, a cached schema file will be used.");
                return null;
            }
        }

        LOGGER.info("Using schema file at: " + xsdDestFile.toURI().toString());
        return xsdDestFile.toURI().toString();
    }
}