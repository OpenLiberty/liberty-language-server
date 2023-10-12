/*******************************************************************************
* Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.util.DocumentUtil;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class ContainerService {
    private static final Logger LOGGER = Logger.getLogger(ContainerService.class.getName());
    private final int CONTAINER_TIMEOUT = 20; // seconds

    // Singleton so that only 1 Container Service can be initialized and is
    // shared between all Lemminx Language Feature Participants
  
    private static ContainerService instance;
  
    public static ContainerService getInstance() {
        if (instance == null) {
            instance = new ContainerService();
        }
        return instance;
    }

    // Liberty images use Unix path
    public static final String SCHEMA_GEN_JAR_PATH = "bin/tools/ws-schemagen.jar";
    public static final String FEATURE_LIST_JAR_PATH = "bin/tools/ws-featurelist.jar";

    public static final String DEFAULT_CONTAINER_OL_DIR = "opt/ol/wlp/";
    public static final String DEFAULT_CONTAINER_OL_PROPERTIES_PATH = DEFAULT_CONTAINER_OL_DIR + "lib/versions/openliberty.properties";
    public static final String DEFAULT_CONTAINER_OL_SCHEMAGEN_JAR_PATH = DEFAULT_CONTAINER_OL_DIR + SCHEMA_GEN_JAR_PATH;
    public static final String DEFAULT_CONTAINER_OL_FEATURELIST_JAR_PATH = DEFAULT_CONTAINER_OL_DIR + FEATURE_LIST_JAR_PATH;

    public static final String DEFAULT_CONTAINER_WLP_DIR = "opt/ibm/wlp/";
    public static final String DEFAULT_CONTAINER_WLP_PROPERTIES_PATH = DEFAULT_CONTAINER_WLP_DIR + "lib/versions/WebSphereApplicationServer.properties";
    public static final String DEFAULT_CONTAINER_WLP_SCHEMAGEN_JAR_PATH = DEFAULT_CONTAINER_WLP_DIR + SCHEMA_GEN_JAR_PATH;
    public static final String DEFAULT_CONTAINER_WLP_FEATURELIST_JAR_PATH = DEFAULT_CONTAINER_WLP_DIR + FEATURE_LIST_JAR_PATH;

    /** ===== Public Methods ===== **/

    /**
     * Method to execute a command inside the specified container
     * @param containerType
     * @param containerName
     * @param cmd
     */
    public void containerExec(String containerType, String containerName, String cmd) {
        // $ docker exec [OPTIONS] CONTAINER COMMAND [ARG...]
        String containerExec = MessageFormat.format("{0} exec {1} {2}", containerType, containerName, cmd);
        execContainerCmd(containerExec);
    }

    /**
     * Method to copy a file out from the specified container
     * @param containerType
     * @param containerName
     * @param containerSrc
     * @param localDest
     */
    public void containerCp(String containerType, String containerName, String containerSrc, String localDest) {
        containerCp(containerType, containerName, containerSrc, localDest, false);
    }

    /**
     * Method to copy a file out from the specified container
     * @param containerType
     * @param containerName
     * @param containerSrc
     * @param localDest
     * @param suppressError - if the file may not be present, pass true for this boolean parameter to suppress the error/exception
     */
    public void containerCp(String containerType, String containerName, String containerSrc, String localDest, boolean suppressError) {
        // $ docker cp [OPTIONS] CONTAINER:SRC_PATH DEST_PATH|-
        String containerCp = MessageFormat.format("{0} cp {1}:{2} {3}", containerType, containerName, containerSrc, localDest);
        execContainerCmd(containerCp, suppressError);
    }

    /**
     * Generate the schema file for a LibertyWorkspace using the ws-schemagen.jar from the corresponding container
     * @param libertyWorkspace
     * @return Path to generated schema file or null if failed.
     * @throws IOException
     */
    public String generateServerSchemaXsdFromContainer(LibertyWorkspace libertyWorkspace) throws IOException {
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);
        String libertyRuntime = libertyWorkspace.getLibertyRuntime();
        String libertyVersion = libertyWorkspace.getLibertyVersion();
        String xsdFileName = libertyWorkspace.isLibertyRuntimeAndVersionSet() ?
                            libertyRuntime + "-" + libertyVersion + ".xsd" :
                            "server.xsd";
        File xsdFile = new File(tempDir, xsdFileName);

        if (!xsdFile.exists()) {
            // java -jar {path to ws-schemagen.jar} {schemaVersion} {outputVersion} {outputFile}
            String containerOutputFileString = "/tmp/" + xsdFileName;
            String jarPath = (libertyRuntime != null && !libertyRuntime.isEmpty() && libertyRuntime.equals("wlp")) ? DEFAULT_CONTAINER_WLP_SCHEMAGEN_JAR_PATH.toString() : DEFAULT_CONTAINER_OL_SCHEMAGEN_JAR_PATH.toString();
            String schemaVersion = "--schemaVersion=1.1";
            String outputVersion = "--outputVersion=2";
            String cmd = MessageFormat.format("java -jar {0} {1} {2} {3}", jarPath, schemaVersion, outputVersion, containerOutputFileString);

            LOGGER.info("Generating schema file for container at: " + xsdFile.getCanonicalPath());

            // generate xsd file inside container
            containerExec(libertyWorkspace.getContainerType(), libertyWorkspace.getContainerName(), cmd);
            // extract xsd file to local/temp dir
            containerCp(libertyWorkspace.getContainerType(), libertyWorkspace.getContainerName(), containerOutputFileString, tempDir.getCanonicalPath());
        }
        // (re)confirm xsd generation
        if (!xsdFile.exists()) {
            return null;
        }

        // do some post processing to remove the anyAttribute element from parent element if there is no extraProperties sibling
        DocumentUtil.removeExtraneousAnyAttributeElements(xsdFile);

        LOGGER.info("Using schema file at: " + xsdFile.toURI().toString());
        return xsdFile.toURI().toString();
    }

    /**
     * Generate the feature list for a LibertyWorkspace using the ws-featurelist.jar from the corresponding container
     * @param libertyWorkspace
     * @return File the generated feature list file or null if failed.
     * @throws IOException
     */
    public File generateFeatureListFromContainer(LibertyWorkspace libertyWorkspace) throws IOException {
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);
        String libertyRuntime = libertyWorkspace.getLibertyRuntime();
        String libertyVersion = libertyWorkspace.getLibertyVersion();
        String featureListFileName = libertyWorkspace.isLibertyRuntimeAndVersionSet() ?
                            "featurelist-" + libertyRuntime + "-" + libertyVersion + ".xml" :
                            "featurelist.xml";
        File featureListFile = new File(tempDir, featureListFileName);

        if (!featureListFile.exists()) {
            // java -jar {path to ws-featurelist.jar} {outputFile}
            String containerOutputFileString = "/tmp/" + featureListFileName;
            String jarPath = (libertyRuntime != null && !libertyRuntime.isEmpty() && libertyRuntime.equals("wlp")) ? DEFAULT_CONTAINER_WLP_FEATURELIST_JAR_PATH.toString() : DEFAULT_CONTAINER_OL_FEATURELIST_JAR_PATH.toString();
            String cmd = MessageFormat.format("java -jar {0} {1}", jarPath, containerOutputFileString);

            LOGGER.info("Generating feature list file for container at: " + featureListFile.getCanonicalPath());

            // generate feature list file inside container
            containerExec(libertyWorkspace.getContainerType(), libertyWorkspace.getContainerName(), cmd);
            // extract feature list file to local/temp dir
            containerCp(libertyWorkspace.getContainerType(), libertyWorkspace.getContainerName(), containerOutputFileString, tempDir.getCanonicalPath());
        }
        // (re)confirm feature list generation
        if (!featureListFile.exists()) {
            return null;
        }
        LOGGER.info("Using feature list file at: " + featureListFile.toURI().toString());
        return featureListFile;
    }
    /** ===== Protected/Helper Methods ===== **/

    /**
     * @param command String containing the command to run
     * @return the stdout of the command or null for no output on stdout
     */
    protected String execContainerCmd(String command) {
        return execContainerCmd(command, false);
    }

    /**
     * @param command String containing the command to run
     * @param suppressError If it is expected that the command may fail, pass true for this boolean parameter to suppress the error/exception.
     * @return the stdout of the command or null for no output on stdout
     */
    protected String execContainerCmd(String command, boolean suppressError) {
        String result = null;
        try {
            // debug("execDocker, timeout=" + timeout + ", cmd=" + command);
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor(CONTAINER_TIMEOUT, TimeUnit.SECONDS);
            // After waiting for the process, handle the error case and normal termination.
            if (p.exitValue() != 0 && !suppressError) {
                LOGGER.severe("Received exit value=" + p.exitValue() + " when running container command: " + command);
                // read messages from standard err
                char[] d = new char[1023];
                new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8).read(d);
                String stdErrString = new String(d).trim()+" RC="+p.exitValue();
                //LOGGER.severe(stdErrString);
                throw new RuntimeException(stdErrString);
            }
            result = readStdOut(p);
        } catch (IllegalThreadStateException  e) {
            // the timeout was too short and the container command has not yet completed. There is no exit value.
            LOGGER.warning("IllegalThreadStateException, message="+e.getMessage());
            throw new RuntimeException("The container command did not complete within the timeout period: " + CONTAINER_TIMEOUT + " seconds. ");
        } catch (InterruptedException | IOException e) {
            // If a runtime exception occurred in the server task, log and rethrow
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    protected String readStdOut(Process p) throws IOException, InterruptedException {
        // Read all the output on stdout and return it to the caller
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        StringBuffer allLines = new StringBuffer();
        while ((line = in.readLine())!= null) {
            allLines.append(line).append(" ");
        }
        return (allLines.length() > 0) ? allLines.toString() : null;
    }
}
