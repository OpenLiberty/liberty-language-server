/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class DockerService {
    private static final Logger LOGGER = Logger.getLogger(DockerService.class.getName());
    private final int DOCKER_TIMEOUT = 20; // seconds

    // Singleton so that only 1 Docker Service can be initialized and is
    // shared between all Lemminx Language Feature Participants
  
    private static DockerService instance;
  
    public static DockerService getInstance() {
        if (instance == null) {
            instance = new DockerService();
        }
        return instance;
    }

    // Liberty images use Unix path
    public static final String DEFAULT_CONTAINER_WLP_DIR = "opt/ol/wlp/";
    public static final String DEFAULT_CONTAINER_OL_PROPERTIES_PATH = DEFAULT_CONTAINER_WLP_DIR + "lib/versions/openliberty.properties";
    public static final String DEFAULT_CONTAINER_SCHEMAGEN_JAR_PATH = DEFAULT_CONTAINER_WLP_DIR + "bin/tools/ws-schemagen.jar";

    /** ===== Public Methods ===== **/

    /**
     * Method to execute a command inside the specified container
     * @param containerName
     * @param cmd
     */
    public void dockerExec(String containerName, String cmd) {
        // $ docker exec [OPTIONS] CONTAINER COMMAND [ARG...]
        String dockerExec = MessageFormat.format("docker exec {0} {1}", containerName, cmd);
        execDockerCmd(dockerExec);
    }

    /**
     * Method to copy a file out from the specified container
     * @param containerName
     * @param containerSrc
     * @param localDest
     */
    public void dockerCp(String containerName, String containerSrc, String localDest) {
        // $ docker cp [OPTIONS] CONTAINER:SRC_PATH DEST_PATH|-
        String dockerCp = MessageFormat.format("docker cp {0}:{1} {2}", containerName, containerSrc, localDest);
        execDockerCmd(dockerCp);
    }

    /**
     * Generate the schema file for a LibertyWorkspace using the ws-schemagen.jar from the corresponding container
     * @param containerName
     * @return Path to generated schema file.
     * @throws IOException
     */
    public String generateServerSchemaXsdFromContainer(LibertyWorkspace libertyWorkspace) throws IOException {
        File tempDir = LibertyUtils.getTempDir(libertyWorkspace);
        String libertyRuntime = libertyWorkspace.getLibertyRuntime();
        String libertyVersion = libertyWorkspace.getLibertyVersion();
        String xsdFileName = (libertyVersion != null && libertyRuntime != null &&
                            !libertyVersion.isEmpty() && !libertyRuntime.isEmpty()) ?
                            libertyRuntime + "-" + libertyVersion + ".xsd" :
                            "server.xsd";
        File xsdFile = new File(tempDir, xsdFileName);

        if (!xsdFile.exists()) {
            // $ java -jar {path to ws-schemagen.jar} {outputFile}
            String containerOutputFileString = "/tmp/" + xsdFileName;
            String cmd = MessageFormat.format("java -jar {0} {1}", DEFAULT_CONTAINER_SCHEMAGEN_JAR_PATH.toString(), containerOutputFileString);

            // generate xsd file inside container
            dockerExec(libertyWorkspace.getContainerName(), cmd);
            // extract xsd file to local/temp dir
            dockerCp(libertyWorkspace.getContainerName(), containerOutputFileString, tempDir.getCanonicalPath());
        }
        LOGGER.info("Using schema file at: " + xsdFile.toURI().toString());
        return xsdFile.toURI().toString();
    }


    /** ===== Protected/Helper Methods ===== **/

    /**
     * @param timeout unit is seconds
     * @return the stdout of the command or null for no output on stdout
     */
    protected String execDockerCmd(String command) {
        String result = null;
        try {
            // debug("execDocker, timeout=" + timeout + ", cmd=" + command);
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor(DOCKER_TIMEOUT, TimeUnit.SECONDS);
            // After waiting for the process, handle the error case and normal termination.
            if (p.exitValue() != 0) {
                LOGGER.severe("Received exit value=" + p.exitValue() + " when running Docker command: " + command);
                // read messages from standard err
                char[] d = new char[1023];
                new InputStreamReader(p.getErrorStream()).read(d);
                LOGGER.severe("RuntimeException " + new String(d).trim()+" RC="+p.exitValue());
                throw new RuntimeException(new String(d).trim()+" RC="+p.exitValue());
            }
            result = readStdOut(p);
        } catch (IllegalThreadStateException  e) {
            // the timeout was too short and the docker command has not yet completed. There is no exit value.
            LOGGER.warning("IllegalThreadStateException, message="+e.getMessage());
            throw new RuntimeException("The Docker command did not complete within the timeout period: " + DOCKER_TIMEOUT + " seconds. ");
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
