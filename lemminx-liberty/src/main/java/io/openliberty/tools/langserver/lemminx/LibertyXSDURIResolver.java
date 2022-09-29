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
package io.openliberty.tools.langserver.lemminx;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.IExternalGrammarLocationProvider;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager.ResourceToDeploy;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;

import io.openliberty.tools.langserver.lemminx.services.DockerService;
import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.services.LibertyWorkspace;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyXSDURIResolver implements URIResolverExtension, IExternalGrammarLocationProvider {
    private static final Logger LOGGER = Logger.getLogger(LibertyXSDURIResolver.class.getName());

    private static final String XSD_RESOURCE_URL = "https://github.com/OpenLiberty/liberty-language-server/blob/master/lemminx-liberty/src/main/resources/schema/xsd/liberty/server.xsd";
    private static final String XSD_CLASSPATH_LOCATION = "/schema/xsd/liberty/server.xsd";

    /**
     * SERVER_XSD_RESOURCE is the server.xsd that is located at `/schema/server.xsd`
     * that should be deployed (copied) to the .lemminx cache. The resourceURI is
     * used by lemmix determine the path to store the file in the cache. So for
     * server.xsd it takes the resource located at `/schema/server.xsd` and deploys
     * it to:
     * ~/.lemminx/cache/https/github.com/OpenLiberty/liberty-language-server/master/lemminx-liberty/src/main/resources/schema/server.xsd
     * 
     * Declared public to be used by tests
     */
    public static final ResourceToDeploy SERVER_XSD_RESOURCE = new ResourceToDeploy(XSD_RESOURCE_URL,
            XSD_CLASSPATH_LOCATION);

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
        if (LibertyUtils.isConfigXMLFile(baseLocation)) {
            try {
                String serverXMLUri = URI.create(baseLocation).toString();
                LibertyWorkspace libertyWorkspace = LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXMLUri);

                if (libertyWorkspace != null) {
                    //Set workspace properties if not set 
                    LibertyUtils.getVersion(serverXMLUri);
                    LibertyUtils.getRuntimeInfo(serverXMLUri);

                    //Check workspace for Liberty installation and generate schema.xsd file
                    //Return schema URI as String, otherwise use cached schema.xsd file
                    String serverSchemaUri = null;
                    if (libertyWorkspace.isLibertyInstalled()) {
                        Path schemaGenJarPath = LibertyUtils.findFileInWorkspace(libertyWorkspace, Paths.get("bin", "tools", "ws-schemagen.jar"));
                        if (schemaGenJarPath != null) {
                            //Generate schema file
                            serverSchemaUri = generateServerSchemaXsd(libertyWorkspace, schemaGenJarPath);
                        }
                    } else if (libertyWorkspace.isContainerAlive()) {
                        DockerService docker = DockerService.getInstance();
                        serverSchemaUri = docker.generateServerSchemaXsdFromContainer(libertyWorkspace);
                    }
                    if (serverSchemaUri != null && !serverSchemaUri.isEmpty()) {
                        return serverSchemaUri;
                    }
                }
                Path serverXSDFile = CacheResourcesManager.getResourceCachePath(SERVER_XSD_RESOURCE);
                LOGGER.info("Using cached Liberty schema file located at: " + serverXSDFile.toString());
                return serverXSDFile.toUri().toString();
            } catch (Exception e) {
                LOGGER.severe("Error: Unable to deploy server.xsd to lemminx cache.");
                e.printStackTrace();
            }
        }
        return null;
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
        //java -jar path/to/ws-schemagen.jar path/to/workspace/.libertyls/libertySchema.xsd
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);

        //If tempDir is null, issue a warning for the current LibertyWorkspace URI and use the default cached schema file
        if (tempDir == null) {
            LOGGER.warning("Could not create a temporary directory for generating the schema file. The cached schema file will be used for the current workspace: " + libertyWorkspace.getWorkspaceString());
            return null;
        }

        //TODO: (?) Add subfolders to tempDir: schema/xsd/liberty/server.xsd
        File xsdDestFile = new File(tempDir, "server.xsd");
        if (libertyWorkspace.getLibertyVersion()!= null && !libertyWorkspace.getLibertyVersion().isEmpty() &&
                libertyWorkspace.getLibertyRuntime()!= null && !libertyWorkspace.getLibertyRuntime().isEmpty()) {
            xsdDestFile = new File(tempDir, libertyWorkspace.getLibertyRuntime() + "-" + libertyWorkspace.getLibertyVersion() + ".xsd");
        }

        if (!xsdDestFile.exists()) {
            try {
                LOGGER.info("Generating schema file from: " + schemaGenJarPath.toString());
                String xsdDestPath = xsdDestFile.getCanonicalPath();
    
                LOGGER.info("Generating schema file at: " + xsdDestPath);
    
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", schemaGenJarPath.toAbsolutePath().toString(), xsdDestPath); //Add locale param here
                pb.directory(tempDir);
                pb.redirectErrorStream(true);
                pb.redirectOutput(new File(tempDir, "schemagen.log"));
    
                Process proc = pb.start();
                if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                    proc.destroy();
                    LOGGER.warning("Exceeded 30 second timeout during schema file generation. Using cached schema.xsd file.");
                    return null;
                }

                LOGGER.info("Caching schema file with URI: " + xsdDestFile.toURI().toString());
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