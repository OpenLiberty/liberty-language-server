/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.tools.langserver.lemminx.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SchemaAndFeatureListGeneratorUtil {
    private static final Logger LOGGER = Logger.getLogger(SchemaAndFeatureListGeneratorUtil.class.getName());
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public enum ProcessType {
        SCHEMA("schemagen.log", "schema file"),
        FEATURE_LIST("featurelist-cached.log", "feature list");

        private final String logFilename;
        private final String description;

        ProcessType(String logFilename, String description) {
            this.logFilename = logFilename;
            this.description = description;
        }
    }

    public static void generateFile(ProcessType type, Path resourcesDir, Path jarPath, File outputFile, String localeName) throws Exception {
        String destinationPath = outputFile.getCanonicalPath();
        LOGGER.info(String.format("Generating %s at the destination %s ", type.description, destinationPath));

        List<String> command = new ArrayList<>(Arrays.asList("java", "-jar", jarPath.toAbsolutePath().toString()));

        if (type == ProcessType.SCHEMA) {
            command.addAll(Arrays.asList("--schemaVersion=1.1", "--outputVersion=2"));
        }

        command.add(destinationPath);

        if (StringUtils.isNotEmpty(localeName)) {
            command.add("--locale=" + localeName);
        }

        boolean success = runProcess(command, resourcesDir.toFile(), type);
        if (success && outputFile.exists()) {
            if (type == ProcessType.SCHEMA) {
                DocumentUtil.removeExtraneousAnyAttributeElements(outputFile);
            }
            LOGGER.info(String.format("Generated %s: %s", type.description, outputFile.toURI()));
        } else {
            String errorMessage = String.format("The %s is not generated. See the previous log messages for errors or warnings.", type.description);
            LOGGER.info(errorMessage);
            throw new Exception(errorMessage); // Throwing this as it will be cached from LibertyXSDURIResolver and will return null from there
        }
    }

    private static boolean runProcess(List<String> command, File workingDir,
                                      ProcessType type) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        File logFile = new File(workingDir, type.logFilename);
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);

        Process proc = null;
        try {
            proc = pb.start();
            boolean completed = proc.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                LOGGER.warning(String.format("Exceeded %d second timeout during %s generation", DEFAULT_TIMEOUT_SECONDS, type.description));
                return false;
            }

            int exitCode = proc.exitValue();
            if (exitCode != 0) {
                LOGGER.warning(String.format("Process failed with exit code: %d", exitCode));
                return false;
            }

            return true;
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }
}
